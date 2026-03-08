package fr.univ.nantes.data.login

import fr.univ.nantes.core.security.PasswordHasher
import fr.univ.nantes.data.profil.ProfileDao
import fr.univ.nantes.data.profil.ProfileEntity
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginRepository
import fr.univ.nantes.domain.login.User

class LoginRepositoryRoomImpl(
    private val profileDao: ProfileDao
) : LoginRepository {

    override suspend fun authenticate(email: String, password: String): User {
        val existing = profileDao.findByEmail(email) ?: throw LoginException.NotExistingException
        if (!PasswordHasher.verifyPassword(password, existing.password)) {
            throw LoginException.WrongPasswordException
        }
        // ensure single session
        profileDao.logout()
        profileDao.upsert(existing.copy(isLoggedIn = true))
        return User(username = existing.firstName.ifBlank { existing.email }, email = existing.email)
    }

    override suspend fun createUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        currency: String
    ): User {
        val existing = profileDao.findByEmail(email)
        if (existing != null) throw LoginException.AlreadyExistsException
        val entity = ProfileEntity(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            password = PasswordHasher.hashPassword(password),
            isLoggedIn = true
        )
        profileDao.logout()
        profileDao.upsert(entity)
        return User(username = entity.firstName.ifBlank { entity.email }, email = entity.email)
    }
}
