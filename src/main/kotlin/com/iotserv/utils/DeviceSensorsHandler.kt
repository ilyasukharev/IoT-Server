package com.iotserv.utils

object DeviceSensorsHandler {

    fun serialize (sensors: List<String>, types: List<String>) : String {
        val state = StringBuilder()
        repeat(sensors.size) {
            state.append("${sensors[it]}:${types[it]};")
        }
        return state.toString()
    }
}