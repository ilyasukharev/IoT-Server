package com.iotserv.utils

import com.iotserv.dao.device_definition.DeviceDefinitionTable
import com.iotserv.dao.device_structure.DeviceStructureTable
import com.iotserv.dao.personal_data.PersonalDataTable
import com.iotserv.dao.users_devices.UserDevicesTable
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


object DatabaseFactory {
    fun initPostgreSQL(config: ApplicationConfig) {
        val defaultPropertyUrl = "databases.postgres"

        val url = config.property("$defaultPropertyUrl.url").getString()
        val driver = config.property("$defaultPropertyUrl.driver").getString()
        val login = config.property("$defaultPropertyUrl.login").getString()
        val password = config.property("$defaultPropertyUrl.password").getString()


        val database = Database.connect(url, driver = driver, user = login, password = password)

        transaction(database) {
            SchemaUtils.create(PersonalDataTable)
            SchemaUtils.create(DeviceDefinitionTable)
            SchemaUtils.create(DeviceStructureTable)
            SchemaUtils.create(UserDevicesTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}