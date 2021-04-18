package nu.mine.terranz.callstocalendarsync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import nu.mine.terranz.callstocalendarsync.util.CallsUtil

class SyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("SyncReceiver", "Alarm fired for sync")
        CallsUtil().doSync(context)
    }

}