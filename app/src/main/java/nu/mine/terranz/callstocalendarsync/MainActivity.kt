package nu.mine.terranz.callstocalendarsync

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import nu.mine.terranz.callstocalendarsync.util.CalendarUtil
import nu.mine.terranz.callstocalendarsync.util.CallsUtil

class MainActivity : AppCompatActivity() {
    private val calendarsMap = HashMap<Long, String>()
    private val CAL_ID = "cal_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        CalendarUtil(this).setupSyncAlarm()
        CallsUtil().doSync(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkAndRequestPermissions()

        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
            )

            val cr = contentResolver
            val uri = CalendarContract.Calendars.CONTENT_URI
            val cur = cr.query(uri, projection, null, null, null)
            while (cur!!.moveToNext()) {
                val id = cur.getLong(0)
                val name = cur.getString(1)
                calendarsMap[id] = name
            }
            cur.close()
        } catch (e: SecurityException) {
            Log.i(this.javaClass.name, "Get permissions firstly")
        }
    }

    fun chooseCal(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите календарь")

        val sp = PreferenceManager.getDefaultSharedPreferences(this)

        val selectedCalendar = sp.getLong(CAL_ID, -1L)

        var checkedItem = 0
        if (selectedCalendar != -1L)
            checkedItem = calendarsMap.keys.indexOf(selectedCalendar)

        builder.setSingleChoiceItems(
            calendarsMap.values.toTypedArray(),
            checkedItem
        ) { dialog, which ->
            Log.i(this.javaClass.name, which.toString())
            sp.edit().putLong(CAL_ID, calendarsMap.keys.elementAt(which)).apply()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestPermissions() {
        val readCal = checkSelfPermission(Manifest.permission.READ_CALENDAR)
        val writeCal = checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
        val readCallLog = checkSelfPermission(Manifest.permission.READ_CALL_LOG)
        val readContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS)
        val internet = checkSelfPermission(Manifest.permission.INTERNET)
        val onBoot = checkSelfPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED)

        if (readCal != PackageManager.PERMISSION_GRANTED
            || writeCal != PackageManager.PERMISSION_GRANTED
            || readCallLog != PackageManager.PERMISSION_GRANTED
            || readContacts != PackageManager.PERMISSION_GRANTED
            || internet != PackageManager.PERMISSION_GRANTED
            || onBoot != PackageManager.PERMISSION_GRANTED
        ) {
            val perms = arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.INTERNET,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
            )
            ActivityCompat.requestPermissions(this, perms, 1)
        }
    }
}