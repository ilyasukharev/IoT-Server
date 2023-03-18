package com.iotserv.dao.personal_data

import com.iotserv.dto.*
import com.iotserv.exceptions.ExposedException
import com.iotserv.utils.DatabaseFactory.dbQuery
import com.iotserv.utils.RoutesResponses.userNotFound
import com.iotserv.utils.RoutesResponses.userNotFoundCode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

interface PersonalDataManagement {
    suspend fun writeNewUser(data: RegistrationData): Long
    suspend fun isPasswordCorrect(data: LoginData): Boolean
    suspend fun isUserExists(email: String): Boolean
    suspend fun isUserExists(id: Long): Boolean
    suspend fun getId(email: String) : Long
    suspend fun updatePassword(email: String, password: String): Boolean
    suspend fun updateAll(id: Long, data: PersonalData): Boolean
}

class PersonalDataManagementIMPL : PersonalDataManagement {

    override suspend fun writeNewUser(data: RegistrationData): Long = dbQuery {
        PersonalDataManager.new {
            email = data.email
            password = data.password
        }.id.value
    }

    override suspend fun isPasswordCorrect (data: LoginData): Boolean = dbQuery {
        PersonalDataManager.find {
            PersonalDataTable.email eq data.email
        }.limit(1).singleOrNull()?.let {
            it.password == data.password
        } ?: throw ExposedException(userNotFoundCode, userNotFound, listOf(data.email))
    }

    override suspend fun isUserExists(email: String): Boolean = dbQuery {
        PersonalDataManager.find {
            PersonalDataTable.email eq email
        }.limit(1).singleOrNull() != null
    }

    override suspend fun isUserExists(id: Long): Boolean = dbQuery {
        PersonalDataTable.select {
            PersonalDataTable.id eq id
        }.limit(1).singleOrNull() != null
    }

    override suspend fun getId(email: String): Long = dbQuery {
        PersonalDataManager.find {
            PersonalDataTable.email eq email
        }.limit(1).singleOrNull()?.id?.value
            ?: throw ExposedException(userNotFoundCode, userNotFound, listOf(email))
    }

    override suspend fun updatePassword(email: String, password: String): Boolean = dbQuery {
        PersonalDataTable.update ({PersonalDataTable.email eq email}) {
            it[PersonalDataTable.password] = password
        } > 0
    }

    override suspend fun updateAll(id: Long, data: PersonalData): Boolean = dbQuery {
        PersonalDataTable.update ({PersonalDataTable.id eq id}) {
            it[email] = data.email
            it[password] = data.password
        } > 0
    }
}

class PersonalDataManager (id: EntityID<Long>) : LongEntity(id) {
    companion object: LongEntityClass<PersonalDataManager>(PersonalDataTable)

    var email by PersonalDataTable.email
    var password by PersonalDataTable.password
}