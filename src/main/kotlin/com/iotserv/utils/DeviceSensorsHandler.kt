package com.iotserv.utils

import com.iotserv.exceptions.OtherException
import com.iotserv.utils.RoutesResponses.arrivedTypeOfStateIsNotCorrect
import com.iotserv.utils.RoutesResponses.arrivedTypeOfStateIsNotCorrectCode

object DeviceSensorsHandler {
    private const val tInteger = "Integer"
    private const val tBoolean = "Boolean"

    private fun getDefaultTypes(type: String): String =
        when (type) {
            tInteger -> "0"
            tBoolean -> "false"
            else -> "null"
        }

    fun serializeWithDefaultValues (sensors: List<String>, types: List<String>) : String {
        val state = StringBuilder()
        repeat(sensors.size) {
            getDefaultTypes(types[it]).let {type ->
                state.append("${sensors[it]}:$type;")
            }
        }
        return state.toString()
    }
    fun updateToDefaultValues(map: HashMap<String, String>): String{
        val cpy = HashMap<String, String>()
        map.forEach { (k, v) ->
            if (v.toIntOrNull() != null || v.toBooleanStrictOrNull() != null) {
                if (v.toIntOrNull() == null)   cpy[k] = "false"
                else                           cpy[k] = "0"
            }
            else cpy[v] = "null"
        }
        return serializeMapToString(cpy)
    }

    fun deserializeToMap (state: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        state.split(";").filter { it.isNotEmpty() }.map {
            val sensor = it.split(":")
            map[sensor[0]] = sensor[1]
        }
        return map
    }
    private fun serializeMapToString(map: HashMap<String, String>): String {
        val builder = StringBuilder()
        map.forEach {
            builder.append(it.key).append(":").append(it.value).append(";")
        }
        return builder.toString()
    }

    private fun isTypeCorrect (type: String,  state: String) : Boolean {
        return when (type) {
            tInteger -> state.toIntOrNull() != null
            tBoolean -> state.toBooleanStrictOrNull() != null
            else     -> false
        }
    }

    fun getUpdateState (type: String, sensorState: String, sensor: String, deviceState: String) : String {
        if (!isTypeCorrect(type, sensorState))
            throw OtherException(arrivedTypeOfStateIsNotCorrectCode, arrivedTypeOfStateIsNotCorrect, listOf("state: $sensorState"))

        deserializeToMap(deviceState).apply {
            this.forEach {note ->
                if (note.key == sensor) {
                    this[sensor] = sensorState
                    return@apply
                }
            }
        }.let { return serializeMapToString(it) }
    }
}