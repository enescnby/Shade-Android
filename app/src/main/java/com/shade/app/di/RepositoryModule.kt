package com.shade.app.di

import com.shade.app.data.repository.AuthRepositoryImpl
import com.shade.app.data.repository.ChatRepositoryImpl
import com.shade.app.data.repository.ContactRepositoryImpl
import com.shade.app.data.repository.MessageRepositoryImpl
import com.shade.app.domain.repository.AuthRepository
import com.shade.app.domain.repository.ChatRepository
import com.shade.app.domain.repository.ContactRepository
import com.shade.app.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository
}