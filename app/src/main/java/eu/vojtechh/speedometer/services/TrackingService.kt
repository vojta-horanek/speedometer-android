package eu.vojtechh.speedometer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.vojtechh.speedometer.MainActivity
import eu.vojtechh.speedometer.R
import eu.vojtechh.speedometer.interfaces.MqttCommunicatorInterface
import eu.vojtechh.speedometer.models.DeviceInfo
import eu.vojtechh.speedometer.models.SessionDataItem
import eu.vojtechh.speedometer.mqtt.MqqtCommunicator
import eu.vojtechh.speedometer.other.Constants.ACTION_PAUSE_SERVICE
import eu.vojtechh.speedometer.other.Constants.ACTION_START_OR_RESUME_SERVICE
import eu.vojtechh.speedometer.other.Constants.ACTION_STOP_SERVICE
import eu.vojtechh.speedometer.other.Constants.EXTRA_LAUNCHED_FROM_NOTIFICATION
import eu.vojtechh.speedometer.other.Constants.INTENT_ACTION
import eu.vojtechh.speedometer.other.Constants.KEY_ERROR
import eu.vojtechh.speedometer.other.Constants.KEY_MSG_TYPE
import eu.vojtechh.speedometer.other.Constants.KEY_READY
import eu.vojtechh.speedometer.other.Constants.KEY_STOPPED
import eu.vojtechh.speedometer.other.Constants.MSG_TYPE_ERROR
import eu.vojtechh.speedometer.other.Constants.MSG_TYPE_STATUS
import eu.vojtechh.speedometer.other.Constants.NOTIFICATION_CHANNEL_ID
import eu.vojtechh.speedometer.other.Constants.NOTIFICATION_ID
import kotlinx.coroutines.*


class TrackingService : LifecycleService(), MqttCommunicatorInterface {

    var mqqtCommunicator = MqqtCommunicator(this, this)
    lateinit var communicatorJob: Job

    var sessionSpeedRecord: MutableList<SessionDataItem> = mutableListOf()
    var sessionDistance: Float = 0.0f

    var justStarted = true

    lateinit var broadcaster: LocalBroadcastManager

    companion object {
        val deviceData = MutableLiveData<DeviceInfo>()
        val isTracking = MutableLiveData<Boolean>()
        val durationMillis = MutableLiveData<Long>()
    }

    override fun onCreate() {
        super.onCreate()

        broadcaster = LocalBroadcastManager.getInstance(this)
        deviceData.postValue(DeviceInfo(0f, 0f))
        isTracking.postValue(true)
        durationMillis.postValue(0L)

        isTracking.observe(this, Observer {

        })
    }

    private fun saveSession() {
        // TODO Implement, use DATABASE
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (justStarted) {
                        startTrackingService()
                        justStarted = false
                    } else {
                        isTracking.postValue(true)
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseTrackingService()
                }
                ACTION_STOP_SERVICE -> {
                    stopTrackingService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)

        // TODO Connect to MQTT, broadcast new info, save to array and then save the session
    }

    private fun startTimer() {
        // TODO("Not yet implemented")
    }

    private fun pauseTrackingService() {
        isTracking.postValue(false)
        // TODO("Not yet implemented")
    }

    private fun stopTrackingService() {
        mqqtCommunicator.stop()
        runBlocking {
            communicatorJob.join()
        }
        saveSession()
        broadcaster.sendBroadcast(Intent(INTENT_ACTION).apply {
            putExtra(KEY_MSG_TYPE, MSG_TYPE_STATUS)
            putExtra(KEY_STOPPED, true)
        })
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun startTrackingService() {
        startTimer()
        communicatorJob = CoroutineScope(Dispatchers.IO).launch {
            mqqtCommunicator.connect()
        }
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
            .apply {
                putExtra(EXTRA_LAUNCHED_FROM_NOTIFICATION, true)
            }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(resources.getString(R.string.tracking_notification))
            .setSmallIcon(R.drawable.ic_bike)
            .setContentIntent(pendingIntent)
            .setColor(resources.getColor(R.color.colorPrimary))
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {

        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            resources.getString(R.string.tracking_service_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onNewUpdate(info: DeviceInfo) {
        isTracking.value?.let {
            if (it) {
                deviceData.postValue(info)
                sessionSpeedRecord.add(SessionDataItem(info.speed, durationMillis.value!!))
                sessionDistance = info.distance
            }
        }
    }

    override fun onError(error: String) {
        broadcaster.sendBroadcast(Intent(INTENT_ACTION).apply {
            putExtra(KEY_MSG_TYPE, MSG_TYPE_ERROR)
            putExtra(KEY_ERROR, error)
        })
    }

    override fun onReady() {
        broadcaster.sendBroadcast(Intent(INTENT_ACTION).apply {
            putExtra(KEY_MSG_TYPE, MSG_TYPE_STATUS)
            putExtra(KEY_READY, true)
        })
    }

    override fun onShouldHalt() {
        haltTrackingService()
    }

    private fun haltTrackingService() {
        mqqtCommunicator.stop()
        runBlocking {
            communicatorJob.join()
        }
        stopSelf()
    }
}