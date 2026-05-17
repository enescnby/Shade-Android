package com.shade.app.data.repository

import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.remote.api.KeysService
import com.shade.app.data.remote.api.UserService
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val userService: UserService,
    private val keysService: KeysService,
    private val keyVaultManager: KeyVaultManager
) : ContactRepository {
    override suspend fun insertContact(contact: ContactEntity) {
        contactDao.insertContact(contact)
    }

    override fun getAllContacts(): Flow<List<ContactEntity>> {
        return contactDao.getAllContacts()
    }

    override suspend fun getContactByShadeId(shadeId: String): ContactEntity? {
        return contactDao.getContactByShadeId(shadeId)
    }

    override suspend fun getContactByUserId(userId: String): ContactEntity? {
        return contactDao.getContactByUserId(userId)
    }

    override fun observeContactByShadeId(shadeId: String): Flow<ContactEntity?> {
        return contactDao.observeContactByShadeId(shadeId)
    }

    override fun searchContacts(query: String): Flow<List<ContactEntity>> {
        return contactDao.searchContacts(query)
    }

    override suspend fun getOrFetchContact(shadeId: String): ContactEntity? {
        val local = contactDao.getContactByShadeId(shadeId)

        return try {
            val response = userService.lookup("Bearer ${keyVaultManager.getAccessToken()}", shadeId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    if (local != null) {
                        // Kişi zaten DB'de var — sadece profileName'i güncelle
                        // (savedName'e dokunma: kullanıcının kaydettiği özel isim korunur)
                        val freshProfileName = dto.displayName?.takeIf { it.isNotBlank() }
                        contactDao.updateProfileNameByShadeId(shadeId, freshProfileName)
                        local.copy(profileName = freshProfileName)
                    } else {
                        // Yeni kişi: hem profileName hem encryptionPublicKey'i kaydet
                        val newContact = ContactEntity(
                            userId = dto.userId,
                            shadeId = dto.shadeId,
                            encryptionPublicKey = dto.encryptionPublicKey,
                            savedName = null,
                            profileName = dto.displayName?.takeIf { it.isNotBlank() },
                            profileImagePath = null
                        )
                        contactDao.insertContact(newContact)
                        newContact
                    }
                }
            } else local
        } catch (e: Exception) {
            local
        }
    }

    override suspend fun getOrFetchContactByUserId(
        userId: String,
        bypassCache: Boolean,
    ): ContactEntity? {
        val existing = contactDao.getContactByUserId(userId)
        if (!bypassCache && existing != null) return existing

        return try {
            val token = "Bearer ${keyVaultManager.getAccessToken()}"
            val response = keysService.getKeys(token, userId)
            if (!response.isSuccessful) return null
            val body = response.body() ?: return null
            val pubkey = body.publicKey.trim()
            val contact =
                existing?.copy(
                    encryptionPublicKey = pubkey,
                    shadeId = existing.shadeId.ifBlank { body.coreGuardId },
                ) ?: ContactEntity(
                    userId = userId,
                    shadeId = body.coreGuardId,
                    encryptionPublicKey = pubkey,
                    savedName = null,
                    profileName = null,
                    profileImagePath = null,
                )
            contactDao.insertContact(contact)
            contact
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateContactName(shadeId: String, newName: String) {
        contactDao.updateNameByShadeId(shadeId, newName)
    }

    override suspend fun deleteContact(contact: ContactEntity) {
        contactDao.deleteContact(contact)
    }

    override suspend fun setBlocked(userId: String, isBlocked: Boolean) {
        contactDao.setBlocked(userId, isBlocked)
    }
}