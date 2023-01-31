package com.iotserv.utils.logger

import com.iotserv.utils.RoutesResponses.loggerCanNotLogging
import com.iotserv.utils.RoutesResponses.loggerFileIsNotExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface Logger {
    suspend fun writeLog(text: String, senderData: String, senderType: SenderType)
}

enum class SenderType {
    ID,
    IP_ADDRESS,
    IP_ADDRESS_BOARD
}

class FileLogger (private val path: String) : Logger {
    override suspend fun writeLog(text: String, senderData: String, senderType: SenderType) = withContext(Dispatchers.IO){
        val file = File(path)
        if (!file.exists()) {
            throw Exception(loggerFileIsNotExists)
        }
        val writer = BufferedWriter(FileWriter(file, true))
        val reader = BufferedReader(FileReader(file))

        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentTime = LocalDateTime.now().format(formatter)
            val sender = when(senderType) {
                SenderType.ID -> "ID:$senderData"
                SenderType.IP_ADDRESS -> senderData
                SenderType.IP_ADDRESS_BOARD -> "BOARD:$senderData"
            }

            if (reader.readLine() == null)  writer.write("[$currentTime]:$sender - $text")
            else                            writer.write("\n[$currentTime]:$sender - $text")
        } catch(e: IOException) {
            throw Exception(loggerCanNotLogging)
        }

        writer.close()
        reader.close()
    }
}