package com.example.data.model

enum class UserRole(val label: String, val description: String) {
    SUPER_ADMIN("Super Admin", "Master executive access, PIN authentication, and system governance"),
    ADMIN("Admin", "Full treasury control, dual-approval signing, and member management"),
    SUB_ADMIN("Sub-Admin", "Operational reconciliation, trade execution, and pool metrics"),
    MEMBER("User", "Default member role for depositing funds and reviewing personal transactions");

    val isDefaultRole: Boolean get() = this == MEMBER
}

enum class UserResponsibility(
    val id: String,
    val title: String,
    val description: String,
    val category: String
) {
    DEPOSIT_FUNDS(
        "deposit_funds",
        "Capital Injection & Add Funds",
        "Submit capital deposits, attach payment receipts, and record personal injections",
        "Treasury"
    ),
    VIEW_OWN_LEDGER(
        "view_own_ledger",
        "View Personal Ledger & Receipts",
        "Access personal statements, payment confirmations, and transaction proofs",
        "Personal"
    ),
    P2P_TRADING(
        "p2p_trading",
        "P2P Trading & Exchange",
        "Record buy/sell crypto orders and manage USDT liquidity executions",
        "Trading"
    ),
    DUAL_APPROVAL(
        "dual_approval",
        "Dual-Approval (Maker-Checker)",
        "Authorize pending capital injections and second-sign high-value pool wires",
        "Audit"
    ),
    SETTLEMENT_RECONCILIATION(
        "settlement_reconciliation",
        "Bank UTR & Settlement Confirmation",
        "Confirm bank credit receipts, match UTR records, and adjudicate disputes",
        "Operations"
    ),
    CAPITAL_DISTRIBUTION(
        "capital_distribution",
        "Capital & Profit Payouts",
        "Disburse pool capital, profit shares, and investor payouts",
        "Treasury"
    ),
    VIEW_POOL_METRICS(
        "view_pool_metrics",
        "Pool Reserve & Lot Metrics",
        "View aggregate reserves, FIFO cost lots, and weighted exchange rate analytics",
        "Analytics"
    ),
    MANAGE_ROLES_AND_MEMBERS(
        "manage_roles_and_members",
        "Assign Roles & Responsibilities",
        "Whitelist new individuals and assign team roles and operational duties",
        "Administration"
    ),
    EXPORT_AUDITS(
        "export_audits",
        "Export Statements & Run Audits",
        "Generate CSV ledgers, download financial sheets, and run security audits",
        "Compliance"
    );

    companion object {
        fun defaultFor(role: UserRole): Set<UserResponsibility> = when (role) {
            UserRole.SUPER_ADMIN -> entries.toSet()
            UserRole.ADMIN -> setOf(
                DEPOSIT_FUNDS,
                VIEW_OWN_LEDGER,
                P2P_TRADING,
                DUAL_APPROVAL,
                SETTLEMENT_RECONCILIATION,
                CAPITAL_DISTRIBUTION,
                VIEW_POOL_METRICS,
                MANAGE_ROLES_AND_MEMBERS,
                EXPORT_AUDITS
            )
            UserRole.SUB_ADMIN -> setOf(
                DEPOSIT_FUNDS,
                VIEW_OWN_LEDGER,
                P2P_TRADING,
                SETTLEMENT_RECONCILIATION,
                VIEW_POOL_METRICS,
                EXPORT_AUDITS
            )
            UserRole.MEMBER -> setOf(
                DEPOSIT_FUNDS,
                VIEW_OWN_LEDGER
            )
        }
    }
}

data class PoolUser(
    val email: String,
    val name: String,
    val role: UserRole = UserRole.MEMBER,
    val isWhitelisted: Boolean = true,
    val avatarColorHex: Long = 0xFF0D9488,
    val responsibilities: Set<UserResponsibility> = UserResponsibility.defaultFor(role),
    val customDesignation: String = ""
) {
    val displayName: String get() = name
    val isSuperAdmin: Boolean get() = role == UserRole.SUPER_ADMIN
    val isAdmin: Boolean get() = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
    val isSubAdmin: Boolean get() = role == UserRole.SUB_ADMIN
    val isMember: Boolean get() = role == UserRole.MEMBER
    val isDefaultUser: Boolean get() = role == UserRole.MEMBER
    val canManage: Boolean get() = role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.SUB_ADMIN || hasResponsibility(UserResponsibility.MANAGE_ROLES_AND_MEMBERS)

    fun hasResponsibility(resp: UserResponsibility): Boolean {
        return isSuperAdmin || responsibilities.contains(resp)
    }

    val canDeposit: Boolean get() = hasResponsibility(UserResponsibility.DEPOSIT_FUNDS)
    val canViewOwnLedger: Boolean get() = hasResponsibility(UserResponsibility.VIEW_OWN_LEDGER)
    val canTrade: Boolean get() = hasResponsibility(UserResponsibility.P2P_TRADING) || isAdmin
    val canTradeP2P: Boolean get() = canTrade
    val canApprove: Boolean get() = hasResponsibility(UserResponsibility.DUAL_APPROVAL) || isAdmin
    val canPerformDualApproval: Boolean get() = canApprove
    val canSettle: Boolean get() = hasResponsibility(UserResponsibility.SETTLEMENT_RECONCILIATION) || isAdmin
    val canDistribute: Boolean get() = hasResponsibility(UserResponsibility.CAPITAL_DISTRIBUTION) || isAdmin
    val canViewPoolMetrics: Boolean get() = hasResponsibility(UserResponsibility.VIEW_POOL_METRICS) || canManage
    val canManageMembers: Boolean get() = hasResponsibility(UserResponsibility.MANAGE_ROLES_AND_MEMBERS) || isAdmin
    val canExportAudits: Boolean get() = hasResponsibility(UserResponsibility.EXPORT_AUDITS) || canManage

    companion object {
        const val ADMIN_EMAIL = "dipuraj.thapa@gmail.com"

        val DEFAULT_USERS = listOf(
            PoolUser(
                email = ADMIN_EMAIL,
                name = "Dipuraj Thapa (Super Admin)",
                role = UserRole.SUPER_ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFF0D9488,
                responsibilities = UserResponsibility.defaultFor(UserRole.SUPER_ADMIN),
                customDesignation = "Executive Director & Super Admin"
            ),
            PoolUser(
                email = "tariq.admin@siyawat.com",
                name = "Tariq Mansoor (Admin)",
                role = UserRole.ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFF2563EB,
                responsibilities = UserResponsibility.defaultFor(UserRole.ADMIN),
                customDesignation = "Treasury & Dual Signatory Admin"
            ),
            PoolUser(
                email = "bilal.ops@siyawat.com",
                name = "Bilal Khan (Sub-Admin)",
                role = UserRole.SUB_ADMIN,
                isWhitelisted = true,
                avatarColorHex = 0xFFD97706,
                responsibilities = UserResponsibility.defaultFor(UserRole.SUB_ADMIN),
                customDesignation = "Reconciliation & Operations Lead"
            ),
            PoolUser(
                email = "alice.crypto@gmail.com",
                name = "Alice Trader (User)",
                role = UserRole.MEMBER,
                isWhitelisted = true,
                avatarColorHex = 0xFF7C3AED,
                responsibilities = UserResponsibility.defaultFor(UserRole.MEMBER),
                customDesignation = "Liquidity Participant"
            ),
            PoolUser(
                email = "bob.partner@gmail.com",
                name = "Bob Partner (User)",
                role = UserRole.MEMBER,
                isWhitelisted = true,
                avatarColorHex = 0xFF059669,
                responsibilities = UserResponsibility.defaultFor(UserRole.MEMBER),
                customDesignation = "Capital Contributor"
            )
        )
    }
}

