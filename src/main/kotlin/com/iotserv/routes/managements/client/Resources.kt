package com.iotserv.routes.managements.client

import io.ktor.resources.*

@Resource("management/user/devices")
class Devices {
    @Resource("/{id}")
    class Id (val parent: Devices = Devices(), val id: Long) {
        @Resource("/reset")
        class Reset (val parent: Id)
        @Resource("/change")
        class Change (val parent: Id)
    }
}