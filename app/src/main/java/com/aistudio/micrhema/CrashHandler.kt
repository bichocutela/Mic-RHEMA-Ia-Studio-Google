package com.aistudio.micrhema

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object CrashHandler : Thread.UncaughtExceptionHandler {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val stackTrace = Log.getStackTraceString(e)
        prefs.edit().putString("last_crash", stackTrace).commit()
        defaultHandler?.uncaughtException(t, e)
    }

    fun getLastCrash(context: Context): String? {
        val p = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        return p.getString("last_crash", null)
    }

    fun clearLastCrash(context: Context) {
        val p = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        p.edit().remove("last_crash").apply()
    }
}
