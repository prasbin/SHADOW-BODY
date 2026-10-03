package com.shadowbody.app.data.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shadowbody.app.domain.schedule.WakeUpScheduler

class AndroidWakeUpScheduler(
    private val context: Context,
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleWakeUp(): Boolean {
        val schedule = WakeUpScheduler.calculateNextWakeUp()
        if (schedule.isSaturday || schedule.nextWakeUpTime == null) {
            cancelWakeUp()
            return false
        }

        val pendingIntent = createPendingIntent() ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return false
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            schedule.nextWakeUpTime,
            pendingIntent,
        )
        return true
    }

    fun cancelWakeUp() {
        val pendingIntent = createPendingIntent() ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(): PendingIntent? {
        val intent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "com.shadowbody.app.WAKE_UP"
        }
        return PendingIntent.getBroadcast(
            context,
            WAKE_UP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val WAKE_UP_REQUEST_CODE = 1001
    }
}
