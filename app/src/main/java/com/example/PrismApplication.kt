package com.example

import android.app.Application
import android.database.CursorWindow
import com.example.util.AppLogger

class PrismApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("PrismApplication", "Application onCreate: configuring runtime parameters")

        // Increase CursorWindow size to 50MB to safely handle large libraries with lyrics/metadata without crashing
        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 50 * 1024 * 1024)
            AppLogger.i("PrismApplication", "sCursorWindowSize successfully set to 50MB")
        } catch (e: Throwable) {
            AppLogger.w("PrismApplication", "Could not reflectively set sCursorWindowSize: ${e.message}")
        }
    }
}
