package nu.mine.terranz.callstocalendarsync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nu.mine.terranz.callstocalendarsync.util.CalendarUtil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            CalendarUtil(context).setupSyncAlarm()
        }
    }
}