package com.iotserv.plugins

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_definition.DeviceDefinitionManagementImpl
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.device_structure.DeviceStructureIMPL
import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dao.personal_data.PersonalDataManagementIMPL
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dao.users_devices.UserDeviceImpl
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.JwtCookerImpl
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger


fun Application.configureKoin() {

    val koinModule = module {
        single  {environment}
        single { newClient(Endpoint.from(environment.config.property("databases.redis.endpoint").getString())) }
        single<JwtCooker>{JwtCookerImpl()}
        single<PersonalDataManagement> { PersonalDataManagementIMPL() }
        single<DeviceDefinitionManagement> { DeviceDefinitionManagementImpl() }
        single<DeviceStructure> { DeviceStructureIMPL() }
        single<UserDevice> { UserDeviceImpl() }
    }

    install(Koin) {
        slf4jLogger()
        modules(koinModule)
    }
}