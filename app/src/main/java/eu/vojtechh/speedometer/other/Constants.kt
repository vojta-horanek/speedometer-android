package eu.vojtechh.speedometer.other

object Constants {
    const val DEVICE_SSID = "Speedometer FF-00"
    const val SERVER_URI = "tcp://192.168.1.1:1883"
    const val MQTT_CLIENT_ID = "android_app"

    const val TOPIC_SPEEDOMETER_UPDATE = "speedometer/update"
    const val TOPIC_CONFIG_CONTROL = "config/control"
    const val TOPIC_CONFIG_WHEEL = "config/wheel_diameter"

    const val INTENT_ACTION = "tracking_service"

    const val KEY_SPEED = "speed"
    const val KEY_DISTANCE = "distance"
    const val KEY_MSG_TYPE = "message"
    const val KEY_STOPPED = "stopped"
    const val KEY_ERROR = "error"
    const val KEY_READY = "ready"
    const val KEY_CONNECTED = "connected"

    const val NOTIFICATION_CHANNEL_ID = "TrackingServiceChannel"
    const val NOTIFICATION_ID = 1

    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    const val EXTRA_LAUNCHED_FROM_NOTIFICATION = "from_notification"

    const val MSG_TYPE_STATUS = "status"
    const val MSG_TYPE_ERROR = "error"
    const val MSG_TYPE_WIFI_CALLBACK = "wifi_connected"

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_SHOW_TRACKING_UI = "ACTION_SHOW_TRACKING_UI"
}