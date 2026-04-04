package com.shade.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shade.app.data.local.converters.RoomConverters
import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.dao.MessageDao
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.local.entities.MessageEntity

@Database(
    entities = [MessageEntity::class, ContactEntity::class, ChatEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class ShadeDatabase : RoomDatabase() {
    abstract fun messageDao() : MessageDao
    abstract fun chatDao() : ChatDao
    abstract fun contactDao() : ContactDao
}