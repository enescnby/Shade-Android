package com.shade.app.di

import com.shade.app.BuildConfig
import com.shade.app.data.remote.api.AuthService
import com.shade.app.data.remote.api.UserService
import com.shade.app.data.remote.websocket.ShadeWebSocketManager
import com.shade.app.data.remote.websocket.ShadeWebSocketManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .pingInterval(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(15))
            .build()
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(impl: ShadeWebSocketManagerImpl): ShadeWebSocketManager = impl
}