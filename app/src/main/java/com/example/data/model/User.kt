package com.example.data.model

enum class UserRole {
    ADMIN,
    SUB_ADMIN,
    MEMBER
}

data class PoolUser(
    val email: String,
    val name: String,
    val role: UserRole,
    val isWhitelisted: Boolean = true,
    val avatarColorHex: Long = 0xFF0D9488
) {
    val displayName: String get() = name
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val isSubAdmin: Boolean get() = role == UserRole.SUB_ADMIN
    val canManage: Boolean get() = role == UserRole.ADMIN || role == UserRole.SUB_ADMIN

    companion object {
        const val ADMIN_EMAIL = "dipuraj.thapa@gmail.com"

        val DEFAULT_USERS = listOf(
            PoolUser(
                email = ADMIN_EMAIL,
                name = "Dipuraj Thapa (Admin)",
                role = UserRole.ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFF0D9488
            )
        )
    }
}
