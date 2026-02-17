package fr.univ.nantes.data.profil

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "is_logged_in") val isLoggedIn: Boolean,
)
