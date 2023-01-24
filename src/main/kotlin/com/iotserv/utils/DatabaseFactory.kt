package com.iotserv.utils

import com.iotserv.dao.device_definition.DeviceDefinitionTable
import com.iotserv.dao.device_structure.DeviceStructureTable
import com.iotserv.dao.personal_data.PersonalDataTable
import com.iotserv.dao.users_devices.UserDevicesTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


object DatabaseFactory : KoinComponent{
    private val environment by inject<ApplicationEnvironment>()

    fun initPostgreSQL() {
        val defaultPropertyUrl = "databases.postgres"

        val url = environment.config.property("$defaultPropertyUrl.url").getString()
        val driver = environment.config.property("$defaultPropertyUrl.driver").getString()
        val login = environment.config.property("$defaultPropertyUrl.login").getString()
        val password = environment.config.property("$defaultPropertyUrl.password").getString()


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