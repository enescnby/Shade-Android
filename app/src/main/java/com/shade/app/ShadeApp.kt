package com.shade.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.shade.app.data.remote.websocket.ShadeWebSocketManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShadeApp : Application(), DefaultLifecycleObserver {
    
    @Inject
    lateinit var webSocketManager: ShadeWebSocketManager

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        webSocketManager.disconnect()
    }


}
