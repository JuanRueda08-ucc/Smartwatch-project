package com.atiempo.wear

import android.app.Application
import com.atiempo.wear.data.seed.Seeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ATiempoApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Trabajo de la app que no depende de una pantalla. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch(Dispatchers.IO) {
            Seeder(container.database, container.settings).seedIfNeeded()
        }
    }
}
