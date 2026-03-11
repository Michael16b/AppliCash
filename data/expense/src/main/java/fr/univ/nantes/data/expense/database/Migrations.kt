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

        // Create missing unique index on participants table
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_participants_groupId_name` ON `participants` (`groupId`, `name`)")
    }
}
