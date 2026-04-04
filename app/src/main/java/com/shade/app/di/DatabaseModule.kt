package com.shade.app.di

import android.content.Context
import androidx.room.Room
import com.shade.app.data.local.ShadeDatabase
import com.shade.app.data.local.dao.ChatDao
import com.shade.app.data.local.dao.ContactDao
import com.shade.app.data.local.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ShadeDatabase {
        return Room.databaseBuilder(
                context,
                ShadeDatabase::class.java,
                "shade_database"
            ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideChatDao(db: ShadeDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: ShadeDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideContactDao(db: ShadeDatabase): ContactDao = db.contactDao()
}