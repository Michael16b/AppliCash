package fr.univ.nantes.data.expense.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN splitType INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE expenses ADD COLUMN splitDetails TEXT NOT NULL DEFAULT '{}'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // splitType and splitDetails may already exist if migrated from 1->2,
        // so we use a safe approach: recreate only if columns are missing.
        try {
            db.execSQL("ALTER TABLE expenses ADD COLUMN splitType INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) { /* column already exists */ }
        try {
            db.execSQL("ALTER TABLE expenses ADD COLUMN splitDetails TEXT NOT NULL DEFAULT '{}'")
        } catch (_: Exception) { /* column already exists */ }

        // Remove possible duplicate participants (same groupId + name), keep first occurrence
        db.execSQL(
            """
            DELETE FROM participants
            WHERE rowid NOT IN (
                SELECT MIN(rowid) FROM participants GROUP BY groupId, name
            )
            """.trimIndent()
        )

        // Create the unique index (idempotent thanks to IF NOT EXISTS)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_participants_groupId_name` ON `participants` (`groupId`, `name`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM participants
            WHERE rowid NOT IN (
                SELECT MIN(rowid) FROM participants GROUP BY groupId, name
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_participants_groupId_name` ON `participants` (`groupId`, `name`)")
    }
}

/**
 * Migration 4 → 5 : conversion des clés primaires de INTEGER (Long) vers TEXT (UUID).
 *
 * Étapes pour chaque table :
 *  1. Créer une table temporaire avec le nouveau schéma (id TEXT, groupId TEXT).
 *  2. Copier les données existantes en convertissant les ids entiers en chaînes de type UUID-like.
 *  3. Supprimer l'ancienne table.
 *  4. Renommer la table temporaire.
 *  5. Recréer les index nécessaires.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── expense_groups ────────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expense_groups_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `groupName` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `shareCode` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `expense_groups_new` (`id`, `groupName`, `createdAt`, `shareCode`)
            SELECT CAST(`id` AS TEXT), `groupName`, `createdAt`, `shareCode`
            FROM `expense_groups`
        """.trimIndent())
        db.execSQL("DROP TABLE `expense_groups`")
        db.execSQL("ALTER TABLE `expense_groups_new` RENAME TO `expense_groups`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_groups_shareCode` ON `expense_groups` (`shareCode`)")

        // ── participants ──────────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `participants_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `groupId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `participants_new` (`id`, `groupId`, `name`)
            SELECT CAST(`id` AS TEXT), CAST(`groupId` AS TEXT), `name`
            FROM `participants`
        """.trimIndent())
        db.execSQL("DROP TABLE `participants`")
        db.execSQL("ALTER TABLE `participants_new` RENAME TO `participants`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_participants_groupId` ON `participants` (`groupId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_participants_groupId_name` ON `participants` (`groupId`, `name`)")

        // ── expenses ──────────────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `groupId` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `paidBy` TEXT NOT NULL,
                `splitType` INTEGER NOT NULL DEFAULT 0,
                `splitDetails` TEXT NOT NULL DEFAULT '{}',
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `expenses_new` (`id`, `groupId`, `description`, `amount`, `paidBy`, `splitType`, `splitDetails`, `createdAt`)
            SELECT CAST(`id` AS TEXT), CAST(`groupId` AS TEXT), `description`, `amount`, `paidBy`, `splitType`, `splitDetails`, `createdAt`
            FROM `expenses`
        """.trimIndent())
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_groupId` ON `expenses` (`groupId`)")
    }
}

