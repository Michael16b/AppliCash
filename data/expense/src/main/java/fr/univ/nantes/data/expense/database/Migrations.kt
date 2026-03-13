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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add a new column 'receiptPath' with default empty string to keep backward compatibility
        try {
            db.execSQL("ALTER TABLE expenses ADD COLUMN receiptPath TEXT NOT NULL DEFAULT ''")
        } catch (_: Exception) {
            // ignore if column already exists
        }
    }
}
