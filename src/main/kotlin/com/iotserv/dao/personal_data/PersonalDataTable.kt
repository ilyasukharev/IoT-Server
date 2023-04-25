package com.iotserv.dao.personal_data

import org.jetbrains.exposed.dao.id.LongIdTable

object PersonalDataTable : LongIdTable("personal_data") {
    val email = varchar("email", length = 50).uniqueIndex()
    val password = varchar("password", length = 50)
}