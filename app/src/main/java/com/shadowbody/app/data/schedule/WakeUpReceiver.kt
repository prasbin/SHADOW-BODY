package com.shadowbody.app.data.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WakeUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = AndroidWakeUpScheduler(context)
        scheduler.scheduleWakeUp()
    }
}
