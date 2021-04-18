package nu.mine.terranz.callstocalendarsync.util

import android.content.Context
import android.database.Cursor
import android.preference.PreferenceManager
import android.provider.CallLog
import android.util.Log
import nu.mine.terranz.callstocalendarsync.entity.PhoneCall
import java.util.*
import kotlin.collections.ArrayList


class CallsUtil {

    fun doSync(context: Context?) {
        Log.i(this.javaClass.name, "Starting sync");
        val celendarUtil = context?.let {
            CalendarUtil(
                it
            )
        }
        val callsUtil = CallsUtil()
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (context != null) {
            val calls = callsUtil.getCallLogs(
                context,
                sp.getLong("last_sync", Calendar.getInstance().timeInMillis - 60 * 60 * 1000)
            )
            Log.i(this.javaClass.name, "Received " + calls.size + " new calls to process");

            calls.forEach { phoneCall: PhoneCall -> celendarUtil?.saveCallEvent(phoneCall) }

            sp.edit().putLong("last_sync", Calendar.getInstance().timeInMillis - 60 * 60 * 1000)
                .apply()
        }
    }

    fun getCallLogs(context: Context, lastCheck: Long): List<PhoneCall> {
        val phoneCalls = ArrayList<PhoneCall>()
        val managedCursor: Cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null, CallLog.Calls.DATE + " > ?", arrayOf(lastCheck.toString()), null
        )!!
        val number: Int = managedCursor.getColumnIndex(CallLog.Calls.NUMBER)
        val type: Int = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date: Int = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration: Int = managedCursor.getColumnIndex(CallLog.Calls.DURATION)
        while (managedCursor.moveToNext()) {
            val phNumber: String = managedCursor.getString(number)
            val callType: String = managedCursor.getString(type)
            val callDate: String = managedCursor.getString(date)
            val dircode = callType.toInt()
            var missed = false
            var incoming = false
            when (dircode) {
                CallLog.Calls.OUTGOING_TYPE -> incoming = false
                CallLog.Calls.INCOMING_TYPE -> incoming = true
                CallLog.Calls.MISSED_TYPE -> missed = true
            }

            phoneCalls.add(
                PhoneCall(
                    missed,
                    incoming,
                    callDate.toLong(),
                    managedCursor.getString(duration).toLong(),
                    phNumber
                )
            )
        }
        managedCursor.close()
        return phoneCalls;
    }
}