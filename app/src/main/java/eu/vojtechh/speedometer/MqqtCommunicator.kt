package eu.vojtechh.speedometer

import android.content.Context
import android.util.Log
import eu.vojtechh.speedometer.data.DeviceInfo
import eu.vojtechh.speedometer.interfaces.MqttCommunicatorInterface
import eu.vojtechh.speedometer.other.Constants.KEY_DISTANCE

import eu.vojtechh.speedometer.other.Constants.KEY_SPEED
import eu.vojtechh.speedometer.other.Constants.MQTT_CLIENT_ID
import eu.vojtechh.speedometer.other.Constants.SERVER_URI
import eu.vojtechh.speedometer.other.Constants.TOPIC_CONFIG_CONTROL
import eu.vojtechh.speedometer.other.Constants.TOPIC_CONFIG_WHEEL
import eu.vojtechh.speedometer.other.Constants.TOPIC_SPEEDOMETER_UPDATE
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

class MqqtCommunicator(val context: Context, var consumer: MqttCommunicatorInterface) {

    lateinit var mqttAndroidClient: MqttAndroidClient

    fun stop() {
        mqttAndroidClient.unregisterResources()
        mqttAndroidClient.close()
    }

    fun connect() {
        mqttAndroidClient = MqttAndroidClient(
            context,
            SERVER_URI,
            MQTT_CLIENT_ID + System.currentTimeMillis().toString()
        )
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    subscribeToTopic()
                }
            }

            override fun connectionLost(cause: Throwable) {
                sendError(context.getString(R.string.lost_connection_error))
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                // Checking might be unnecessary since this only subscribes to one topic
                if (topic == TOPIC_SPEEDOMETER_UPDATE) {
                    val report = JSONObject(String(message.payload))
                    val speed = report.getDouble(KEY_SPEED)
                    val distance = report.getDouble(KEY_DISTANCE)
                    consumer.onNewUpdate(DeviceInfo(speed.toFloat(), distance.toFloat()))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        try {

            val mqttConnectOptions = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
            }

            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    mqttAndroidClient.setBufferOpts(DisconnectedBufferOptions().apply {
                        isBufferEnabled = true
                        bufferSize = 100
                        isPersistBuffer = false
                        isDeleteOldestMessages = false
                    })
                    subscribeToTopic()

                    mqttAndroidClient.publish(
                        TOPIC_CONFIG_WHEEL,
                        MqttMessage(
                            SharedPref.read(SharedPref.WHEEL_DIAMETER, 215.5f).toString()
                                .toByteArray()
                        )
                    )
                    mqttAndroidClient.publish(
                        TOPIC_CONFIG_CONTROL,
                        MqttMessage("start".toByteArray())
                    )
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    sendError(context.getString(R.string.connection_error))
                    consumer.onShouldHalt()
                    Log.e("Mqtt", exception.message)
                    return
                }
            })
        } catch (ex: MqttException) {
            sendError(context.getString(R.string.unexpected_error))
            consumer.onShouldHalt()
            return
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(
                TOPIC_SPEEDOMETER_UPDATE,
                0,
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        Log.e("onSucces", "Subscribed to topic")
                        consumer.onReady()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        sendError(context.getString(R.string.subsciption_error))
                        consumer.onShouldHalt()
                    }
                })
        } catch (ex: MqttException) {
            sendError(context.getString(R.string.unexpected_error))
            consumer.onShouldHalt()
        }
    }

    private fun sendError(error: String) {
        consumer.onError(error)
    }
}