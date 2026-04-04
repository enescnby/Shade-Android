package com.shade.app.di

import android.content.Context
import com.shade.app.BuildConfig
import com.shade.app.data.remote.api.AuthService
import com.shade.app.data.remote.api.MediaService
import com.shade.app.data.remote.api.MessageService
import com.shade.app.data.remote.api.UserService
import com.shade.app.data.remote.websocket.ShadeWebSocketManager
import com.shade.app.data.remote.websocket.ShadeWebSocketManagerImpl
import com.shade.app.util.ConnectivityObserver
import com.shade.app.util.NetworkConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(okHttpClient)
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
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(60))
            .build()
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(impl: ShadeWebSocketManagerImpl): ShadeWebSocketManager = impl

    @Provides
    @Singleton
    fun provideMediaService(retrofit: Retrofit): MediaService {
        return retrofit.create(MediaService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageService(retrofit: Retrofit): MessageService {
        return retrofit.create(MessageService::class.java)
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }
}