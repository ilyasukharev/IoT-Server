package com.iotserv.dao.personal_data

import com.iotserv.dto.*
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

interface PersonalDataManagement {
    suspend fun writeNewUser(data: RegistrationData): ULong?
    suspend fun verifyUserLoginAndPwd(data: LoginData): ULong?
    suspend fun isUserExists(data: RegistrationData): Boolean
    suspend fun isUserExists(email: String): Boolean
    suspend fun getId(data: EmailData) : ULong?
    suspend fun updatePassword(id: ULong, data: PasswordData): Boolean
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

    override suspend fun verifyUserLoginAndPwd(data: LoginData): ULong? = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.email eq data.email
        }.limit(1).singleOrNull()?.let {
            if (it[PersonalDataTable.password].toString() == data.password)     it[PersonalDataTable.id]
            else                                                                null
        }
    }

    override suspend fun isUserExists(data: RegistrationData): Boolean = dbQuery {
        PersonalDataTable.select {
            (PersonalDataTable.email eq data.email) or (PersonalDataTable.number eq data.number)
        }.limit(1).singleOrNull()?.let{ true } ?: false
    }

    override suspend fun isUserExists(email: String): Boolean = dbQuery{
        PersonalDataTable.select {
            PersonalDataTable.email eq email
        }.limit(1).singleOrNull() != null
    }

    override suspend fun getId(data: EmailData): ULong? = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.email eq data.email
        }.limit(1).singleOrNull()?.get(PersonalDataTable.id)
    }

    override suspend fun updatePassword(id: ULong, data: PasswordData): Boolean = dbQuery {
        PersonalDataTable.update ({PersonalDataTable.id eq id}) {
            it[password] = data.password
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