package fr.univ.nantes.archi

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import fr.univ.nantes.archi.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        startKoin()
    }

    private fun startKoin() {
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApplication)
            modules(appModules)
        }
    }
}
