package com.iotserv.utils

object DeviceStatementsCooker {
    fun generateStatement (sensors: List<String>, states: List<String>) : String {
        val builder = StringBuilder()
        for (i in 0 until sensors.size) {
            builder.append("${sensors[i]}:${states[i]};")
        }
        return builder.toString()
    }
}