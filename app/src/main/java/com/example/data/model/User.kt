package com.example.data.model

enum class UserRole {
    SUPER_ADMIN,
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
    val isSuperAdmin: Boolean get() = role == UserRole.SUPER_ADMIN
    val isAdmin: Boolean get() = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
    val isSubAdmin: Boolean get() = role == UserRole.SUB_ADMIN
    val isMember: Boolean get() = role == UserRole.MEMBER
    val canManage: Boolean get() = role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SUB_ADMIN

    companion object {
        const val ADMIN_EMAIL = "dipuraj.thapa@gmail.com"

        val DEFAULT_USERS = listOf(
            PoolUser(
                email = ADMIN_EMAIL,
                name = "Dipuraj Thapa (Super Admin)",
                role = UserRole.SUPER_ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFF0D9488
            ),
            PoolUser(
                email = "tariq.admin@siyawat.com",
                name = "Tariq Mansoor (Admin)",
                role = UserRole.ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFF2563EB
            ),
            PoolUser(
                email = "bilal.ops@siyawat.com",
                name = "Bilal Khan (Sub-Admin)",
                role = UserRole.SUB_ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFFD97706
            ),
            PoolUser(
                email = "alice.crypto@gmail.com",
                name = "Alice Trader (Member)",
                role = UserRole.MEMBER,
                isWhitelisted = true,
                avatarColorHex = 0xFF7C3AED
            ),
            PoolUser(
                email = "bob.partner@gmail.com",
                name = "Bob Partner (Member)",
                role = UserRole.MEMBER,
                isWhitelisted = true,
                avatarColorHex = 0xFF059669
            )
        )
    }
}
