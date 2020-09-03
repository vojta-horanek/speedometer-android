package eu.vojtechh.speedometer.interfaces

import eu.vojtechh.speedometer.data.DeviceInfo

interface MqttCommunicatorInterface {
    fun onNewUpdate(info: DeviceInfo)
    fun onError(error: String)
    fun onReady()
    fun onShouldHalt()
}