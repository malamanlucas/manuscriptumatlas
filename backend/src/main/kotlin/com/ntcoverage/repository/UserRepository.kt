package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

class UserRepository {

    private val cache = ConcurrentHashMap<String, Pair<UserDTO, Long>>()
    private val cacheTtlMs = 5 * 60 * 1000L

    private fun ResultRow.toUserDTO() = UserDTO(
        id = this[Users.id].value,
        email = this[Users.email],
        displayName = this[Users.displayName],
        pictureUrl = this[Users.pictureUrl],
        role = this[Users.role].name
    )

    fun findByEmail(email: String): UserDTO? {
        cache[email]?.let { (user, ts) ->
            if (System.currentTimeMillis() - ts < cacheTtlMs) return user
            cache.remove(email)
        }
        return transaction {
            Users.selectAll().where { Users.email eq email }.singleOrNull()?.toUserDTO()
        }?.also { cache[email] = it to System.currentTimeMillis() }
    }

    fun findById(id: Int): UserDTO? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUserDTO()
    }

    fun findAll(): List<UserDTO> = transaction {
        Users.selectAll().orderBy(Users.createdAt).map { it.toUserDTO() }
    }

    fun countByRole(role: UserRole): Long = transaction {
        Users.selectAll().where { Users.role eq role }.count()
    }

    fun create(email: String, displayName: String, role: UserRole): UserDTO = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = Users.insertAndGetId {
            it[Users.email] = email
            it[Users.displayName] = displayName
            it[Users.role] = role
            it[Users.createdAt] = now
        }
        UserDTO(id.value, email, displayName, null, role.name)
    }.also { invalidateAll() }

    fun updateRole(id: Int, role: UserRole): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.role] = role
        } > 0
    }.also { invalidateAll() }

    fun delete(id: Int): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }.also { invalidateAll() }

    fun updateLastLoginAndPicture(email: String, pictureUrl: String?) {
        transaction {
            Users.update({ Users.email eq email }) {
                it[lastLoginAt] = OffsetDateTime.now(ZoneOffset.UTC)
                if (pictureUrl != null) it[Users.pictureUrl] = pictureUrl
            }
        }
    }

    fun invalidateCache(email: String) { cache.remove(email) }
    fun invalidateAll() { cache.clear() }
}
