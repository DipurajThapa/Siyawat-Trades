package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PoolUser
import com.example.data.model.UserRole
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.ProfitGreenDark
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.ProfitGreenBorder
import com.example.ui.theme.RedCritical

@Composable
fun UserRoleSwitcherDialog(
    currentUser: PoolUser,
    whitelistedUsers: List<PoolUser>,
    onDismiss: () -> Unit,
    onSelectUser: (PoolUser) -> Unit,
    onSimulateCustomLogin: (String) -> Unit,
    onAddWhitelistedMember: (String, String) -> Unit,
    onResetDemoData: () -> Unit,
    onAssignRolesAndResponsibilities: ((PoolUser) -> Unit)? = null
) {
    var customEmailInput by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    var newMemberName by remember { mutableStateOf("") }
    var showAddSection by remember { mutableStateOf(false) }
    var pendingAdminUser by remember { mutableStateOf<PoolUser?>(null) }

    if (pendingAdminUser != null) {
        AdminPinChallengeDialog(
            targetAdminName = pendingAdminUser!!.name,
            onDismiss = { pendingAdminUser = null },
            onSuccess = {
                val target = pendingAdminUser!!
                pendingAdminUser = null
                onSelectUser(target)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("user_role_switcher_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MutedBlueLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MutedBlueLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OAuth & Whitelist Simulation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Designated Admin: ${PoolUser.ADMIN_EMAIL}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Whitelisted Users List
                Text(
                    text = "APPROVED WHITELIST MEMBERS (${whitelistedUsers.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    whitelistedUsers.forEach { user ->
                        val isCurrent = user.email.equals(currentUser.email, ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                                    color = if (isCurrent) MutedBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (user.isAdmin && !isCurrent) {
                                        pendingAdminUser = user
                                    } else {
                                        onSelectUser(user)
                                    }
                                }
                                .padding(12.dp)
                                .testTag("select_user_${user.email}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(user.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (user.customDesignation.isNotBlank()) {
                                        Text(
                                            text = user.customDesignation,
                                            fontSize = 10.sp,
                                            color = MutedBlueDark,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val roleColor = when (user.role) {
                                    UserRole.SUPER_ADMIN -> Color(0xFF6366F1)
                                    UserRole.ADMIN -> MutedBluePrimary
                                    UserRole.SUB_ADMIN -> MutedBlueLight
                                    UserRole.MEMBER -> Color(0xFF64748B)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(roleColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("user_role_chip_${user.email}")
                                ) {
                                    Text(
                                        text = if (user.isDefaultUser) "USER (DEFAULT)" else user.role.label.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor
                                    )
                                }

                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MutedBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if ((currentUser.isAdmin || currentUser.canManageMembers) && onAssignRolesAndResponsibilities != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onAssignRolesAndResponsibilities(user) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("assign_role_switcher_btn_${user.email}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Assign Roles & Duties",
                                            tint = MutedBluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Test Unauthorized Gating Simulation
                Text(
                    text = "TEST WHITELIST ENFORCEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simulate login with an unauthorized Google account to verify access restriction:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        placeholder = { Text("stranger.trader@gmail.com") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_email_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (customEmailInput.isNotBlank()) {
                                onSimulateCustomLogin(customEmailInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedCritical,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("test_unauthorized_btn")
                    ) {
                        Text("Simulate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add Member to Whitelist Section (Admin only)
                if (currentUser.isAdmin) {
                    if (!showAddSection) {
                        OutlinedButton(
                            onClick = { showAddSection = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Member to Whitelist")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Add Member to Approved Whitelist",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ProfitGreenContainer)
                                            .border(1.dp, ProfitGreenBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Default Assigned Role: User",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Admins can assign duties anytime",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newMemberEmail,
                                    onValueChange = { newMemberEmail = it },
                                    label = { Text("Google Account Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newMemberName,
                                    onValueChange = { newMemberName = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { showAddSection = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newMemberEmail.isNotBlank()) {
                                                onAddWhitelistedMember(newMemberEmail, newMemberName)
                                                newMemberEmail = ""
                                                newMemberName = ""
                                                showAddSection = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MutedBluePrimary, contentColor = Color.White)
                                    ) {
                                        Text("Add to Whitelist", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clear Ledger Data button (Production Clean)
                OutlinedButton(
                    onClick = {
                        onResetDemoData()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_clear_all_ledger_data"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RedCritical
                    ),
                    border = BorderStroke(1.dp, RedCritical.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = RedCritical
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Ledger Data", color = RedCritical, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
