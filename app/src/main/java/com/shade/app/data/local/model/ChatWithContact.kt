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
    /** Sohbet listesi ve chat başlığı için: sadece kullanıcının kaydettiği isim ya da Shade ID */
    val displayName: String
        get() = if (chat.isGroup) chat.groupName ?: chat.chatId
                else contact?.savedName ?: contact?.shadeId ?: chat.chatId

    /** Profil ekranı için: kayıtlı isim → profil adı → shadeId */
    val fullDisplayName: String
        get() = if (chat.isGroup) chat.groupName ?: chat.chatId
                else contact?.savedName ?: contact?.profileName ?: contact?.shadeId ?: chat.chatId
}
