package io.github.carvalhoxx.gamevault.data.repository

import io.github.carvalhoxx.gamevault.data.local.dao.UserProfileDao
import io.github.carvalhoxx.gamevault.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/** Acesso ao perfil persistido do usuário. */
class UserRepository(
    private val userProfileDao: UserProfileDao
) {
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    fun observeUserProfile(): Flow<UserProfileEntity?> = userProfile

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        userProfileDao.upsertUserProfile(profile)
    }
}
