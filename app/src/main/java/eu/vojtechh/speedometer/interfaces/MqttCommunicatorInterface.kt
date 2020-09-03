package eu.vojtechh.speedometer.interfaces

import eu.vojtechh.speedometer.models.DeviceInfo

interface MqttCommunicatorInterface {
    fun onNewUpdate(info: DeviceInfo)
    fun onError(error: String)
    fun onReady()
    fun onShouldHalt()
}