package com.shade.app.domain.repository

import com.shade.app.data.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun insertContact(contact: ContactEntity)
    fun getAllContacts(): Flow<List<ContactEntity>>
    suspend fun getContactByShadeId(shadeId: String): ContactEntity?
    fun observeContactByShadeId(shadeId: String): Flow<ContactEntity?>
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    suspend fun getOrFetchContact(shadeId: String): ContactEntity?
    suspend fun deleteContact(contact: ContactEntity)
    suspend fun updateContactName(shadeId: String, newName: String)
}