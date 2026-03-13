# 💸 AppliCash

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=flat&logo=kotlin)
![KMP](https://img.shields.io/badge/Platform-KMP-orange.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-green.svg?logo=jetpackcompose)

**AppliCash** is a multi-module financial application built with **Kotlin Multiplatform (KMP)**. The project is designed with a focus on shared logic across platforms and a native, modern UI for Android using **Jetpack Compose**.

---

## 🚀 Project Overview
AppliCash follows a **Clean Modular Architecture**, separating concerns into distinct layers to ensure high maintainability and scalability:

* **Data Layer**: Responsible for API communication, local databases, and data mapping.
* **Domain Layer**: Contains the core business logic, entities, and reusable Use Cases.
* **Presentation Layer**: Built using feature-based modules with **Jetpack Compose** for a reactive UI.

---

## 🛠️ Tech Stack
* **Core Logic**: Kotlin Multiplatform (KMP)
* **Android UI**: Jetpack Compose
* **Dependency Injection**: Koin / Hilt (depending on module)
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
* **Testing & Automation**: Maestro (UI automation), JUnit, MockK
* **CI/CD**: GitHub Actions

---

## 📊 Test Strategy & Quality Report
Quality is integrated into every step of our development. We maintain a strict testing matrix to ensure the stability of the shared logic and the mobile interface.

### 🔗 Live Test & Tests Report
Our automated reports, including code coverage and test execution results, are published at:
👉 **[https://michael16b.github.io/AppliCash/](https://michael16b.github.io/AppliCash/)**

👉 **[https://github.com/Michael16b/AppliCash/pull/66#issuecomment-4058461290](https://github.com/Michael16b/AppliCash/pull/66#issuecomment-4058461290)**

### 🧪 Coverage Matrix
| Layer | Test Type | Tools |
| :--- | :--- | :--- |
| **Domain** | Unit Testing | JUnit 5 / Kotlin Test |
| **Data** | Integration Testing | MockK / Flow |
| **Presentation** | UI Testing | Compose Testing Library |
| **End-to-End** | Automation | Maestro (Nightly UI runs) |

### 🤖 CI/CD Integration
* **Automated Runs**: Every Pull Request triggers a full suite of unit and integration tests.
* **Artifacts**: HTML coverage reports are automatically generated and deployed to GitHub Pages.
* **Naming Conventions**: We use descriptive naming (e.g., `Given_When_Then`) to ensure tests serve as living documentation.

---

## 👥 The Team
* **Michaël**
* **Maxence**
* **David**

---

## 🚦 Getting Started
1.  **Clone the repository**:
    ```bash
    git clone [https://github.com/Michael16b/AppliCash.git](https://github.com/Michael16b/AppliCash.git)
    ```
2.  **Open in Android Studio**: Use the latest stable version of Android Studio.
3.  **Run Tests locally**:
    ```bash
    ./gradlew test
    ```
4.  **Install on device**:
    ```bash
    ./gradlew installDebug
    ```

---
*For a more detailed breakdown of our testing infrastructure, please check [TESTING.md](./TESTING.md).*

# 📑 Global Test Execution Report: AppliCash

**Project Version:** 1.0 (Commit: `1b2c7bf`)
**Status:** 🟢 **STABLE** (100% Success Rate)  
**Report Date:** March 2024

---

## 1. Quality Dashboard (Key Metrics)

The following metrics summarize the test execution results from our automated CI/CD pipeline.

| Metric | Value |
| :--- | :--- |
| **Total Tests Executed** | **281** |
| **Tests Passed** | 281 ✅ |
| **Tests Failed** | 0 ❌ |
| **Test Suites** | 22 |
| **Execution Time** | 2m 59s ⏱️ |
| **Success Rate** | **100%** |

---

## 2. Testing Strategy

Our strategy follows the **Software Testing Pyramid**, ensuring a solid foundation of unit tests complemented by integration and UI automation.



[Image of the software testing pyramid]


### 🏗️ Multi-Layer Coverage
* **Domain Layer (CommonMain):** Pure Kotlin tests for business logic, use cases, and entities. This logic is shared across platforms via **KMP**.
* **Data Layer:** Integration tests for repositories, verifying the data flow between remote APIs, local persistence, and data mapping.
* **Presentation Layer (Android):** Unit tests for **ViewModels** and UI interaction tests using **Jetpack Compose Testing** libraries.
* **End-to-End (E2E):** Critical user journeys (e.g., creating a group, adding an expense) are automated using **Maestro**.

---

## 3. Architecture & Modular Organization

AppliCash is built using a **Clean Modular Architecture**. Our test suites are isolated per module to ensure high maintainability.



### 🔍 Core Features Tested
* **Authentication & Profile (RG14):** Verification of user profile emission, currency code propagation, and login state management.
* **Expense Management (RG11, RG12):** Defensive testing against invalid inputs (negative amounts, blank descriptions) and correct debt calculations.
* **Group Management (RG13):** Logic for adding/removing participants and maintaining relational integrity with expenses.
* **Serialization Logic:** Robustness of JSON parsing/serialization for complex data structures like `splitDetails`.

---

## 4. Technical Environment

To guarantee deterministic results and high performance, we use the following stack:
* **JUnit 5 & Kotlin Test:** Standard assertion libraries.
* **Robolectric:** To run Android-dependent tests (ViewModels/UI) on the JVM for faster feedback.
* **Fake Repository Pattern:** We prioritize **Fakes** over Mocks to test stateful interactions and avoid brittle test code.
* **Coroutines Test:** Using `StandardTestDispatcher` and `runTest` to handle asynchronous logic synchronously and avoid flakiness.

---

## 5. CI/CD & Automation

Quality enforcement is fully automated via **GitHub Actions**.

1.  **Continuous Testing:** Every Pull Request triggers the full suite of 281 tests.
2.  **Linting:** Static code analysis via **Ktlint** ensures the codebase adheres to professional standards.
3.  **Automated Reporting:** Test results and coverage reports are automatically published to our public documentation site.

### 🔗 Live Interactive Report
For a granular view of every test case and coverage percentage, visit:  
👉 **[https://michael16b.github.io/AppliCash/](https://michael16b.github.io/AppliCash/)**

---

## 6. Methodology: Behavior-Driven Development (BDD)

The team utilizes the **Given-When-Then** naming convention. This ensures that each test case serves as "Living Documentation" for the business rules.

* **Given:** A specific state (e.g., "User is in a group with 2 participants").
* **When:** An action occurs (e.g., "User adds a 20€ expense").
* **Then:** An assertion is made (e.g., "The group balance is updated to -10€ for the other member").

---

## 7. Conclusion

With **281 tests passing with a 100% success rate**, the AppliCash project demonstrates a high level of reliability and architectural maturity. The modular approach combined with KMP ensures that the business core is robust and ready for scaling or multi-platform deployment (iOS/Web).

**Future Roadmap:**
* Implement **Snapshot Testing** to prevent visual regressions in Compose UI.
* Expand the **Maestro** suite to cover edge cases in offline-first scenarios.

---
*Report maintained by: Michaël, Maxence, and David.*
