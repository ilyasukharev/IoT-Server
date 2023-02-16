package com.iotserv.dao.personal_data

import com.iotserv.dto.*
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

interface PersonalDataManagement {
    suspend fun writeNewUser(data: RegistrationData): ULong?
    suspend fun isUserDataCorrect(data: LoginData): Boolean
    suspend fun isUserExists(email: String, number: String): Boolean
    suspend fun isUserExists(email: String): Boolean
    suspend fun isUserExists(id: ULong): Boolean
    suspend fun getId(email: String) : ULong?
    suspend fun updatePassword(email: String, password: String): Boolean
    suspend fun updateAll(id:ULong, data: PersonalData): Boolean
}

class PersonalDataManagementIMPL : PersonalDataManagement {

    override suspend fun writeNewUser(data: RegistrationData): ULong? = dbQuery {
        PersonalDataTable.insert {
            it[email] = data.email
            it[password] = data.password
            it[number] = data.number
        }.resultedValues?.singleOrNull()?.get(PersonalDataTable.id)
    }

    override suspend fun isUserDataCorrect(data: LoginData): Boolean = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.email eq data.email
        }.limit(1).singleOrNull()?.let {it[PersonalDataTable.password] == data.password} ?: false
    }

    override suspend fun isUserExists(email: String, number: String): Boolean = dbQuery {
        PersonalDataTable.select {
            (PersonalDataTable.email eq email) or (PersonalDataTable.number eq number)
        }.limit(1).singleOrNull() != null
    }

    override suspend fun isUserExists(email: String): Boolean = dbQuery{
        PersonalDataTable.select {
            PersonalDataTable.email eq email
        }.limit(1).singleOrNull() != null
    }

    override suspend fun isUserExists(id: ULong): Boolean = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.id eq id
        }.limit(1).singleOrNull() != null
    }

    override suspend fun getId(email: String): ULong? = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.email eq email
        }.limit(1).singleOrNull()?.get(PersonalDataTable.id)
    }

    override suspend fun updatePassword(email: String, password: String): Boolean = dbQuery {
        PersonalDataTable.update ({PersonalDataTable.email eq email}) {
            it[PersonalDataTable.password] = password
        } > 0
    }

    override suspend fun updateAll(id: ULong, data: PersonalData): Boolean = dbQuery {
        PersonalDataTable.update ({PersonalDataTable.id eq id}) {
            it[email] = data.email
            it[number] = data.number
            it[password] = data.password
        } > 0
    }

}