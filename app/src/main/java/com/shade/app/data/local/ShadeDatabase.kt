package com.shade.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shade.app.data.local.converters.RoomConverters
import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.dao.MessageDao
import com.shade.app.data.local.entities.ChatEntity
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.data.local.entities.MessageEntity

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE messages ADD COLUMN audioPath TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN audioDurationMs INTEGER")
        database.execSQL("ALTER TABLE messages ADD COLUMN filePath TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN fileName TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN fileSizeBytes INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add group columns to chats table
        database.execSQL("ALTER TABLE chats ADD COLUMN isGroup INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE chats ADD COLUMN groupName TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Kişinin kendi profilinde belirlediği görünen adı saklamak için yeni kolon
        database.execSQL("ALTER TABLE contacts ADD COLUMN profileName TEXT")
    }
}

@Database(
    entities = [MessageEntity::class, ContactEntity::class, ChatEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class ShadeDatabase : RoomDatabase() {
    abstract fun messageDao() : MessageDao
    abstract fun chatDao() : ChatDao
    abstract fun contactDao() : ContactDao
}