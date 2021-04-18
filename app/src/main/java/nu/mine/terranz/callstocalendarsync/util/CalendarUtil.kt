package nu.mine.terranz.callstocalendarsync.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.provider.Contacts
import android.util.Log
import nu.mine.terranz.callstocalendarsync.R
import nu.mine.terranz.callstocalendarsync.entity.PhoneCall
import nu.mine.terranz.callstocalendarsync.receiver.SyncReceiver
import java.util.*

private const val CAL_ID = "cal_id"

class CalendarUtil(private var context: Context) {

    fun setupSyncAlarm() {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, SyncReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
        }
        alarmMgr.cancel(alarmIntent)
        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_HOUR,
            alarmIntent
        )
        Log.i(this.javaClass.name, "Alarm set");
    }


    fun getContactName(phoneNumber: String): String {
        val uri: Uri
        var projection: Array<String>
        var mBaseUri = Contacts.Phones.CONTENT_FILTER_URL
        projection = arrayOf(Contacts.People.NAME)
        try {
            val c = Class
                .forName("android.provider.ContactsContract\$PhoneLookup")
            mBaseUri = c.getField("CONTENT_FILTER_URI").get(mBaseUri) as Uri
            projection = arrayOf("display_name")
        } catch (e: Exception) {
        }

        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        var contactName = ""

        if (cursor!!.moveToFirst()) {
            contactName = cursor.getString(0)
        }
        cursor.close()
        return contactName
    }

    fun saveCallEvent(phoneCall: PhoneCall) {

        val sp = PreferenceManager.getDefaultSharedPreferences(context)

        if (!checkCallEvent(phoneCall.genId())) {
            Log.i(this.javaClass.name, "Saving new call event $phoneCall");
            var descr = if (phoneCall.incoming) {
                context.getString(R.string.in_call)
            } else {
                context.getString(R.string.out_call)
            }

            descr = if (phoneCall.missed) {
                context.getString(R.string.lost_call) + " " + descr
            } else {
                context.getString(R.string.accepted_call) + " " + descr
            }

            val cr = context.contentResolver
            val values = ContentValues()
            val calendarId = sp.getLong(CAL_ID, 1L)
            val len = phoneCall.length / 1000
            val descFull =
                descr + " вызов, от: " + phoneCall.number + " (" + getContactName(phoneCall.number) + ") длительностью: " + len + " секунд"

            values.put(CalendarContract.Events.DTSTART, phoneCall.startDate)
            values.put(CalendarContract.Events.DTEND, phoneCall.startDate + phoneCall.length)
            values.put(
                CalendarContract.Events.TITLE,
                descr + " " + context.getString(R.string.call)
            )
            values.put(CalendarContract.Events.DESCRIPTION, descFull)
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            values.put(CalendarContract.Events.UID_2445, phoneCall.genId())
            values.size()
            cr.insert(CalendarContract.Events.CONTENT_URI, values)
        }
    }

    fun checkCallEvent(id: Int): Boolean {
        val cr = context.contentResolver
        var cursor: Cursor? = null;
        try {
            cursor = cr.query(
                CalendarContract.Events.CONTENT_URI,
                null,
                CalendarContract.Events.UID_2445 + " = ?",
                arrayOf(id.toString()),
                null
            );
            return cursor != null && cursor.count > 0
        } finally {
            cursor?.close();
        }
    }
}