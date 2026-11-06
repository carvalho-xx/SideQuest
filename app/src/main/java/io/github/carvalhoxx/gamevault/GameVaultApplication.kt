package io.github.carvalhoxx.gamevault

import android.app.Application
import io.github.carvalhoxx.gamevault.di.AppContainer

class GameVaultApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
