package nu.mine.terranz.callstocalendarsync.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import nu.mine.terranz.callstocalendarsync.util.CallsUtil

/**
 * Background sync worker. Replaces the old hourly [android.app.AlarmManager]
 * alarm + [SyncReceiver]. Runs on a background coroutine and performs the same
 * call-log → calendar sync that used to happen in the broadcast receiver.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i("SyncWorker", "Running sync")
        CallsUtil().doSync(applicationContext)
        return Result.success()
    }
}
