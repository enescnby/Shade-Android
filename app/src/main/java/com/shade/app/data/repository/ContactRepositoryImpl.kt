package com.shade.app.data.repository

import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.remote.api.UserService
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val userService: UserService,
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

    override fun observeContactByShadeId(shadeId: String): Flow<ContactEntity?> {
        return contactDao.observeContactByShadeId(shadeId)
    }

    override fun searchContacts(query: String): Flow<List<ContactEntity>> {
        return contactDao.searchContacts(query)
    }

    override suspend fun getOrFetchContact(shadeId: String): ContactEntity? {
        val local = contactDao.getContactByShadeId(shadeId)
        if (local != null) return local

        return try {
            val response = userService.lookup("Bearer ${keyVaultManager.getAccessToken()}", shadeId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val newContact = ContactEntity(
                        userId = dto.userId,
                        shadeId = dto.shadeId,
                        encryptionPublicKey = dto.encryptionPublicKey,
                        savedName = null,
                        profileImagePath = null
                    )
                    contactDao.insertContact(newContact)
                    newContact
                }
            } else null
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
}