package com.klvcorp.lumio

import android.content.Context

/** Shared application-scoped controller: activity recreation does not interrupt an active session. */
object LumioRuntime {
    @Volatile private var instance: LumioController? = null
    @Synchronized fun get(context: Context): LumioController {
        return instance ?: LumioController(context.applicationContext).also { instance = it }
    }
    @Synchronized fun reset() {
        instance?.close()
        instance = null
    }
}
