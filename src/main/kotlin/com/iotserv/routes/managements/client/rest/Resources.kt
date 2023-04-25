package com.iotserv.routes.managements.client.rest

import io.ktor.resources.*


@Resource("/management/user/devices")
class Devices(val offset: Long? = 0, val limit: Int? = Int.MAX_VALUE) {
    @Resource("/{id}")
    class Id (val parent: Devices = Devices(), val id: Long) {
        @Resource("/reset")
        class Reset (val parent: Id)
        @Resource("/change")
        class Change (val parent: Id)
    }
}