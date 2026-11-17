package com.pocketradar.smartcoach.sample

import android.app.Application
import com.pocketradar.smartcoach.SmartCoach
import com.pocketradar.smartcoach.SmartCoachDeviceConfigurationOptions
import com.pocketradar.smartcoach.SmartCoachError

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configure once, before anything else touches SmartCoach. Never crash over it: a
        // missing or malformed API key is reported on screen instead.
        try {
            SmartCoach.configure(
                this,
                SmartCoachDeviceConfigurationOptions(
                    autoReconnect = true,
                    debugLogging = true, // logcat tag SmartCoachSDK
                ),
            )
        } catch (e: SmartCoachError) {
            configurationError = e
        }
    }

    companion object {
        /** Why [SmartCoach.configure] failed, or `null` if it succeeded. */
        var configurationError: SmartCoachError? = null
            private set
    }
}
