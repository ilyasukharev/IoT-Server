package com.iotserv.routes.managements.client.rest

import io.ktor.resources.*


@Resource("/management/user/devices")
class Devices(val offset: Long? = 0, val limit: Int? = Int.MAX_VALUE) {
    @Resource("/reset")
    class Reset (val parent: Devices = Devices())
    @Resource("/{id}")
    class Id (val parent: Devices = Devices(), val id: Long) {
        @Resource("/change")
        class Change (val parent: Id)
    }
}