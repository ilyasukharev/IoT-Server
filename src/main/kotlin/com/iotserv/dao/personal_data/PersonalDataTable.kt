package com.iotserv.dao.personal_data

import org.jetbrains.exposed.sql.Table

object PersonalDataTable : Table("personal_data") {
    val id = ulong("id").autoIncrement()
    val number = varchar("number", length = 11)
    val email = varchar("email", length = 50)
    val password = varchar("password", length = 50)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}