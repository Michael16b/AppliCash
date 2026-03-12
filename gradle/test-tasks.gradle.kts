// Shared Gradle tasks to run multi-module tests and aggregate JaCoCo reports
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import javax.xml.parsers.DocumentBuilderFactory

// Apply JaCoCo plugin to subprojects that expose test tasks
subprojects {
    // Apply only if the subproject has a test task
    afterEvaluate {
        if (plugins.hasPlugin("java") || tasks.findByName("test") != null) {
            pluginManager.apply(JacocoPlugin::class.java)

            // Configure JaCoCo reporting for each module
            tasks.withType(Test::class.java).configureEach {
                extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java) {
                    isEnabled = true
                    // set module-specific exec file using non-deprecated API
                    destinationFile = layout.buildDirectory.file("jacoco/jacoco.exec").get().asFile
                }
            }

            // Generate XML/HTML report per module (useful for local debugging)
            tasks.register("jacocoReportForModule", JacocoReport::class.java) {
                dependsOn(tasks.matching { it.name == "test" })
                group = "verification"
                description = "Generate JaCoCo report for this module"

                val javaClasses = files(layout.buildDirectory.dir("classes/kotlin/main").get().asFile, layout.buildDirectory.dir("classes/java/main").get().asFile)
                val execFiles = files(layout.buildDirectory.file("jacoco/jacoco.exec").get().asFile)
                val sourceDirs = files("src/main/kotlin", "src/main/java")

                executionData.setFrom(execFiles)
                additionalClassDirs.setFrom(javaClasses)
                sourceDirectories.setFrom(sourceDirs)

                reports {
                    xml.required.set(true)
                    html.required.set(true)
                }
            }
        }
    }
}

// Root task to run all JVM/unit tests across modules
tasks.register("testAll") {
    group = "verification"
    description = "Run all unit tests in all modules (JVM tests)"
    dependsOn(subprojects.flatMap { sp -> sp.tasks.matching { it.name == "test" }.toList() })
}

// Root task to run only tests in affected modules supplied via -PaffectedModules=module1,module2
// Example: ./gradlew fastTests -PaffectedModules=feature/expense,domain/login
tasks.register("fastTests") {
    group = "verification"
    description = "Run tests only for affected modules (pass -PaffectedModules=path1,path2). If empty, runs testAll."

    // Read property (available during configuration when passed via -P)
    val affectedProp = providers.gradleProperty("affectedModules").orNull
    val modulesList: List<String> = affectedProp?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    if (modulesList.isEmpty()) {
        // No modules specified: run full test suite
        dependsOn("testAll")
    } else {
        // Build a collection of test tasks from the specified modules
        val depsMutable = mutableListOf<TaskProvider<*>>()
        modulesList.forEach { mod ->
            val projectPath = ":" + mod.replace('/', ':')
            val p = rootProject.findProject(projectPath)
            if (p == null) {
                logger.warn("fastTests: project $projectPath not found, falling back to testAll for this entry")
                depsMutable.add(rootProject.tasks.named("testAll"))
            } else {
                val matching = p.tasks.matching { t -> t.name == "test" || t.name == "testDebugUnitTest" }
                if (matching.isEmpty()) {
                    logger.warn("fastTests: no test tasks found in $projectPath, skipping")
                } else {
                    matching.forEach { depsMutable.add(it as TaskProvider<*>) }
                }
            }
        }

        if (depsMutable.isNotEmpty()) dependsOn(depsMutable)
        else dependsOn("testAll")
    }
}

// JaCoCo aggregation: collect exec/xml files from modules and create aggregated XML report
// Each module is expected to produce ${buildDir}/jacoco/jacoco.exec
tasks.register("jacocoAggregate", JacocoReport::class.java) {
    group = "verification"
    description = "Aggregate JaCoCo exec files from subprojects"

    // Ensure compile/test tasks are executed before aggregation to avoid implicit dependency errors
    dependsOn(subprojects.flatMap { sp ->
        sp.tasks.matching { it.name == "test" || it.name == "compileKotlin" || it.name == "compileJava" }.toList()
    })

    // Collect exec files from subprojects using serializable paths (configuration-cache friendly)
    val execFilePaths: List<String> = subprojects.map { p -> p.layout.buildDirectory.file("jacoco/jacoco.exec").get().asFile.absolutePath }
    val execFiles = files(execFilePaths.map { File(it) })
    executionData.setFrom(execFiles)

    // Collect class dirs and source dirs across subprojects (use plain File paths)
    val classDirsFiles = subprojects.flatMap { p ->
        listOf(p.layout.buildDirectory.dir("classes/kotlin/main").get().asFile, p.layout.buildDirectory.dir("classes/java/main").get().asFile)
    }.filter { it.exists() }
    val srcDirsFiles = subprojects.flatMap { p ->
        listOf(File(p.projectDir, "src/main/kotlin"), File(p.projectDir, "src/main/java"))
    }

    additionalClassDirs.setFrom(files(classDirsFiles))
    sourceDirectories.setFrom(files(srcDirsFiles))

    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}

// Task that produces aggregated HTML reports by copying per-module HTML and creating an index
tasks.register("jacocoHtmlAggregate") {
    group = "verification"
    description = "Generate aggregated JaCoCo HTML report (depends on jacocoAggregate and per-module reports)"
    dependsOn("testAll")
    dependsOn("jacocoAggregate")

    // Pre-compute module report paths as simple strings to avoid capturing Project instances in the task action
    val moduleReportPaths: List<String> = subprojects.map { p -> p.layout.buildDirectory.dir("reports/jacoco/html").get().asFile.absolutePath }

    doLast {
        // Create output dir for aggregated reports
        val outDir = File(layout.buildDirectory.dir("reports/jacoco/html").get().asFile.absolutePath)
        outDir.mkdirs()

        // Copy per-module HTML reports if present
        moduleReportPaths.forEach { path ->
            val moduleHtml = File(path)
            if (moduleHtml.exists()) {
                copy {
                    from(moduleHtml)
                    into(File(outDir, moduleHtml.name))
                }
            }
        }

        // Create a simple index.html summary referencing modules that produced reports
        val indexFile = File(outDir, "index.html")
        indexFile.writeText("<html><body><h1>Aggregated coverage reports</h1><ul>")
        moduleReportPaths.forEach { path ->
            val moduleDir = File(path)
            if (moduleDir.exists()) {
                // moduleDir.name may not be the original project name; we try to infer from folder name
                val moduleName = moduleDir.parentFile?.name ?: moduleDir.name
                indexFile.appendText("<li><a href=\"${moduleName}/index.html\">${moduleName}</a></li>")
            }
        }
        indexFile.appendText("</ul></body></html>")

        println("Aggregated JaCoCo HTML report available at: ${outDir.absolutePath}")
    }
}

// Enforce aggregated coverage thresholds by parsing the aggregated JaCoCo XML report
// - Fail build if coverage < 80% (0.80)
// - Emit a warning if coverage is >= 80% and < 95%
// - No alert if coverage >= 95%
// This task reads the aggregated XML produced by `jacocoAggregate` (build/reports/jacoco/jacoco.xml)
tasks.register("coverageEnforce") {
    group = "verification"
    description = "Enforce aggregated coverage thresholds: fail if <80%, warn if >=80% and <95%"
    dependsOn("jacocoAggregate")

    // Pre-compute aggregated XML path to avoid capturing Project in the action
    val aggXmlPath = File(layout.buildDirectory.file("reports/jacoco/jacoco.xml").get().asFile.absolutePath).absolutePath

    doLast {
        val xmlFile = File(aggXmlPath)
        if (!xmlFile.exists()) {
            logger.warn("Aggregated JaCoCo XML report not found at: ${xmlFile.absolutePath}")
            return@doLast
        }

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        // Prefer LINE counter, fallback to INSTRUCTION if LINE not present
        val counters = doc.getElementsByTagName("counter")
        var covered = 0L
        var missed = 0L
        for (i in 0 until counters.length) {
            val node = counters.item(i)
            val elem = node as org.w3c.dom.Element
            val type = elem.getAttribute("type")
            if (type == "LINE") {
                covered = elem.getAttribute("covered").toLong()
                missed = elem.getAttribute("missed").toLong()
                break
            }
        }
        if (covered == 0L && missed == 0L) {
            // Try INSTRUCTION
            for (i in 0 until counters.length) {
                val node = counters.item(i)
                val elem = node as org.w3c.dom.Element
                val type = elem.getAttribute("type")
                if (type == "INSTRUCTION") {
                    covered = elem.getAttribute("covered").toLong()
                    missed = elem.getAttribute("missed").toLong()
                    break
                }
            }
        }

        val total = covered + missed
        if (total == 0L) {
            logger.warn("No coverage data found in aggregated JaCoCo report.")
            return@doLast
        }

        val coverage = covered.toDouble() / total.toDouble()
        val percent = String.format("%.2f", coverage * 100)
        if (coverage < 0.80) {
            throw org.gradle.api.GradleException("Aggregated coverage is ${percent}% — below the required minimum of 80% (build failed).")
        } else if (coverage < 0.95) {
            logger.warn("Aggregated coverage is ${percent}% — below the 95% informational threshold. (minimum enforced: 80%)")
        } else {
            logger.lifecycle("Aggregated coverage is ${percent}% — meets thresholds.")
        }
    }
}
