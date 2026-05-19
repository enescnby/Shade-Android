package com.shade.app.data.repository

import android.content.Context
import android.util.Log
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.remote.api.KeysService
import com.shade.app.data.remote.api.MediaService
import com.shade.app.data.remote.api.UserService
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.security.KeyVaultManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val userService: UserService,
    private val keysService: KeysService,
    private val keyVaultManager: KeyVaultManager,
    private val mediaService: MediaService,
    @ApplicationContext private val context: Context
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
            val token = "Bearer ${keyVaultManager.getAccessToken()}"
            val response = userService.lookup(token, shadeId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val freshProfileName = dto.displayName?.takeIf { it.isNotBlank() }

                    // Profil fotoğrafını indir (yalnızca imageId değiştiyse)
                    val imagePath = downloadProfileImageIfNeeded(
                        token = token,
                        shadeId = shadeId,
                        remoteImageId = dto.profileImageId,
                        currentPath = local?.profileImagePath
                    )

                    if (local != null) {
                        // Kişi zaten DB'de var — profileName ve profileImagePath'i güncelle
                        // (savedName'e dokunma: kullanıcının kaydettiği özel isim korunur)
                        contactDao.updateProfileNameByShadeId(shadeId, freshProfileName)
                        if (imagePath != local.profileImagePath) {
                            contactDao.updateProfileImageByShadeId(shadeId, imagePath)
                        }
                        local.copy(profileName = freshProfileName, profileImagePath = imagePath)
                    } else {
                        // Yeni kişi: profileName, encryptionPublicKey ve profileImagePath'i kaydet
                        val newContact = ContactEntity(
                            userId = dto.userId,
                            shadeId = dto.shadeId,
                            encryptionPublicKey = dto.encryptionPublicKey,
                            savedName = null,
                            profileName = freshProfileName,
                            profileImagePath = imagePath
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

    /**
     * Profil fotoğrafını backend'den indirir ve yerel dosyaya kaydeder.
     * Aynı imageId zaten mevcutsa mevcut path'i döndürür (gereksiz indirme yapmaz).
     * imageId yoksa null döndürür.
     */
    private suspend fun downloadProfileImageIfNeeded(
        token: String,
        shadeId: String,
        remoteImageId: String?,
        currentPath: String?
    ): String? {
        if (remoteImageId == null) return null

        // Mevcut dosya adından imageId çıkar (dosya adı = "<imageId>.jpg")
        val currentImageId = currentPath?.let { File(it).nameWithoutExtension }
        if (currentImageId == remoteImageId && currentPath != null && File(currentPath).exists()) {
            return currentPath // Zaten güncel, tekrar indirme
        }

        return try {
            val response = mediaService.downloadImage(token, remoteImageId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    Log.w("ContactRepo", "Boş yanıt gövdesi: $shadeId")
                    return currentPath
                }
                val dir = File(context.filesDir, "avatars").also { it.mkdirs() }
                val dest = File(dir, "$remoteImageId.jpg")
                body.byteStream().use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                if (dest.exists() && dest.length() > 0) dest.absolutePath else currentPath
            } else {
                Log.w("ContactRepo", "Profil fotoğrafı indirilemedi: ${response.code()} for $shadeId")
                currentPath
            }
        } catch (e: Exception) {
            Log.e("ContactRepo", "Profil fotoğrafı indirme hatası: ${e.message}")
            currentPath
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
            val shadeId = existing?.shadeId?.ifBlank { body.coreGuardId } ?: body.coreGuardId

            val lookupToken = "Bearer ${keyVaultManager.getAccessToken()}"
            var displayName: String? = null
            var remoteImageId: String? = null
            try {
                val lookupResp = userService.lookup(lookupToken, shadeId)
                if (lookupResp.isSuccessful) {
                    val dto = lookupResp.body()
                    displayName = dto?.displayName?.takeIf { it.isNotBlank() }
                    remoteImageId = dto?.profileImageId
                }
            } catch (_: Exception) {}

            val needsPhoto = existing?.profileImagePath
                ?.let { File(it).exists() } != true
            val imagePath = if (needsPhoto) {
                downloadProfileImageIfNeeded(
                    token = lookupToken,
                    shadeId = shadeId,
                    remoteImageId = remoteImageId,
                    currentPath = existing?.profileImagePath,
                )
            } else {
                existing?.profileImagePath
            }

            val contact =
                existing?.copy(
                    encryptionPublicKey = pubkey,
                    shadeId = shadeId,
                    profileName = existing.profileName ?: displayName,
                    profileImagePath = imagePath,
                ) ?: ContactEntity(
                    userId = userId,
                    shadeId = shadeId,
                    encryptionPublicKey = pubkey,
                    savedName = null,
                    profileName = displayName,
                    profileImagePath = imagePath,
                )
            contactDao.insertContact(contact)
            contact
        } catch (e: Exception) {
            Log.w(TAG, "getOrFetchContactByUserId network error userId=$userId — ${e.message}")
            existing?.takeIf { it.encryptionPublicKey.isNotBlank() }
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

    private companion object {
        private const val TAG = "ContactRepo"
    }
}