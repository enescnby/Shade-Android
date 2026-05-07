package com.shade.app.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.entities.ContactEntity

data class ChatWithContact(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "shadeId"
    ) val contact: ContactEntity?
) {
    val displayName: String
        // Öncelik: kullanıcının kaydettiği özel isim → kişinin kendi profil adı → shadeId
        get() = contact?.savedName ?: contact?.profileName ?: contact?.shadeId ?: chat.chatId
}
