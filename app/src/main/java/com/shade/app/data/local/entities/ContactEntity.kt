package com.shade.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val userId: String,

    val shadeId: String,

    val encryptionPublicKey: String,

    val savedName: String?,
    val profileImagePath: String?,
    val isBlocked: Boolean = false
)
