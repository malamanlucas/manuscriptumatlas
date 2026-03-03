package com.ntcoverage.service

import com.ntcoverage.model.UserDTO
import com.ntcoverage.model.UserRole
import com.ntcoverage.repository.UserRepository
import org.slf4j.LoggerFactory

class UserService(private val repo: UserRepository) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun getAll(): List<UserDTO> = repo.findAll()

    fun create(email: String, displayName: String, role: UserRole, actorEmail: String): UserDTO {
        require(email.isNotBlank()) { "Email is required" }
        require(displayName.isNotBlank()) { "Display name is required" }

        log.info("AUDIT: user_created | actor=$actorEmail | target=$email | role=$role")
        return repo.create(email, displayName, role)
    }

    fun updateRole(targetId: Int, newRole: UserRole, actorEmail: String): Boolean {
        val target = repo.findById(targetId)
            ?: throw NoSuchElementException("User not found")

        require(target.email != actorEmail) { "Cannot change your own role" }

        if (target.role == UserRole.ADMIN.name && newRole == UserRole.MEMBER) {
            check(repo.countByRole(UserRole.ADMIN) > 1) { "Cannot remove the last ADMIN" }
        }

        log.info("AUDIT: user_role_changed | actor=$actorEmail | target=${target.email} | old=${target.role} | new=$newRole")
        return repo.updateRole(targetId, newRole)
    }

    fun delete(targetId: Int, actorEmail: String) {
        val target = repo.findById(targetId)
            ?: throw NoSuchElementException("User not found")

        require(target.email != actorEmail) { "Cannot delete yourself" }

        if (target.role == UserRole.ADMIN.name) {
            check(repo.countByRole(UserRole.ADMIN) > 1) { "Cannot delete the last ADMIN" }
        }

        log.info("AUDIT: user_deleted | actor=$actorEmail | target=${target.email} | role=${target.role}")
        repo.delete(targetId)
    }
}
