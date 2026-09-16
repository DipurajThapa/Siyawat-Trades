package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.data.model.UserResponsibility
import com.example.data.model.UserRole
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperCard
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBorder
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.ProfitGreenDark
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignRoleAndResponsibilitiesDialog(
    user: PoolUser,
    onDismiss: () -> Unit,
    onSave: (UserRole, Set<UserResponsibility>, String) -> Unit
) {
    var selectedRole by remember(user) { mutableStateOf(user.role) }
    var selectedResponsibilities by remember(user) { mutableStateOf(user.responsibilities) }
    var customDesignation by remember(user) { mutableStateOf(user.customDesignation) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("assign_role_and_responsibilities_dialog"),
            color = SlateBackground,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, PaperBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MutedBluePrimary.copy(alpha = 0.15f))
                                .border(1.dp, MutedBluePrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = MutedBluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Assign Roles & Duties",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Configure permissions and operational roles",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_assign_role_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target User Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, PaperBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                                    color = TextPrimary
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Current Role Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (user.isDefaultUser) Color(0xFF94A3B8).copy(alpha = 0.2f)
                                    else MutedBluePrimary.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (user.isDefaultUser) "USER (DEFAULT)" else user.role.label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isDefaultUser) Color(0xFF64748B) else MutedBluePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content: Role Selection + Designation + Responsibilities
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: ROLE ASSIGNMENT
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "1. ASSIGN ROLE LEVEL",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = MutedBlueDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ProfitGreenContainer)
                                            .border(1.dp, ProfitGreenBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Default: User",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreenDark
                                        )
                                    }
                                }

                                Text(
                                    text = "Reset Role Defaults",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MutedBluePrimary,
                                    modifier = Modifier
                                        .clickable {
                                            selectedResponsibilities = UserResponsibility.defaultFor(selectedRole)
                                        }
                                        .padding(4.dp)
                                        .testTag("reset_role_defaults_btn")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By default, individuals are assigned as standard Users. Administrators can elevate privileges as required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // 4 Role Options
                            UserRole.entries.forEach { role ->
                                val isSelected = selectedRole == role
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedRole = role
                                            // Automatically update to default baseline when selecting role
                                            selectedResponsibilities = UserResponsibility.defaultFor(role)
                                        }
                                        .testTag("role_option_${role.name.lowercase()}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) SlateCardElevated else SlateCard
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MutedBluePrimary else PaperBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedRole = role
                                                selectedResponsibilities = UserResponsibility.defaultFor(role)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = MutedBluePrimary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = role.label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                if (role == UserRole.MEMBER) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE2E8F0))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "DEFAULT",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = role.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: CUSTOM DESIGNATION
                    item {
                        Column {
                            Text(
                                text = "2. ROLE DESIGNATION & TITLE (OPTIONAL)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MutedBlueDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customDesignation,
                                onValueChange = { customDesignation = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_designation_input"),
                                placeholder = {
                                    Text("e.g. Lead Liquidity Trader, Treasury Analyst, Contributor", fontSize = 12.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MutedBluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MutedBluePrimary,
                                    unfocusedBorderColor = PaperBorder,
                                    focusedContainerColor = SlateCard,
                                    unfocusedContainerColor = SlateCard
                                )
                            )
                        }
                    }

                    // SECTION 3: RESPONSIBILITIES QUICK PRESETS
                    item {
                        Column {
                            Text(
                                text = "3. QUICK PRESET DUTIES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MutedBlueDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickPresetChip(
                                    title = "Standard User (Default)",
                                    isSelected = selectedRole == UserRole.MEMBER && selectedResponsibilities == UserResponsibility.defaultFor(UserRole.MEMBER),
                                    onClick = {
                                        selectedRole = UserRole.MEMBER
                                        selectedResponsibilities = UserResponsibility.defaultFor(UserRole.MEMBER)
                                    }
                                )
                                QuickPresetChip(
                                    title = "Trading Desk",
                                    isSelected = selectedResponsibilities.contains(UserResponsibility.P2P_TRADING) && !selectedRole.isDefaultRole,
                                    onClick = {
                                        selectedRole = UserRole.SUB_ADMIN
                                        selectedResponsibilities = setOf(
                                            UserResponsibility.DEPOSIT_FUNDS,
                                            UserResponsibility.VIEW_OWN_LEDGER,
                                            UserResponsibility.P2P_TRADING,
                                            UserResponsibility.VIEW_POOL_METRICS
                                        )
                                    }
                                )
                                QuickPresetChip(
                                    title = "Dual-Signer Auditor",
                                    isSelected = selectedResponsibilities.contains(UserResponsibility.DUAL_APPROVAL),
                                    onClick = {
                                        selectedRole = UserRole.ADMIN
                                        selectedResponsibilities = setOf(
                                            UserResponsibility.DEPOSIT_FUNDS,
                                            UserResponsibility.VIEW_OWN_LEDGER,
                                            UserResponsibility.DUAL_APPROVAL,
                                            UserResponsibility.EXPORT_AUDITS,
                                            UserResponsibility.VIEW_POOL_METRICS
                                        )
                                    }
                                )
                                QuickPresetChip(
                                    title = "Full Administrator",
                                    isSelected = selectedRole == UserRole.ADMIN && selectedResponsibilities == UserResponsibility.defaultFor(UserRole.ADMIN),
                                    onClick = {
                                        selectedRole = UserRole.ADMIN
                                        selectedResponsibilities = UserResponsibility.defaultFor(UserRole.ADMIN)
                                    }
                                )
                            }
                        }
                    }

                    // SECTION 4: GRANULAR RESPONSIBILITIES CHECKLIST
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "4. ASSIGNED RESPONSIBILITIES (${selectedResponsibilities.size}/${UserResponsibility.entries.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MutedBlueDark
                                )

                                Text(
                                    text = if (selectedResponsibilities.size == UserResponsibility.entries.size) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MutedBluePrimary,
                                    modifier = Modifier
                                        .clickable {
                                            selectedResponsibilities = if (selectedResponsibilities.size == UserResponsibility.entries.size) {
                                                emptySet()
                                            } else {
                                                UserResponsibility.entries.toSet()
                                            }
                                        }
                                        .padding(4.dp)
                                        .testTag("toggle_all_responsibilities_btn")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select the specific operational duties and actions this individual is authorized to execute.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Group responsibilities by category
                    val grouped = UserResponsibility.entries.groupBy { it.category }
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(items, key = { it.id }) { resp ->
                            val isChecked = selectedResponsibilities.contains(resp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedResponsibilities = if (isChecked) {
                                            selectedResponsibilities - resp
                                        } else {
                                            selectedResponsibilities + resp
                                        }
                                    }
                                    .testTag("resp_item_${resp.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked) SlateCardElevated else SlateCard
                                ),
                                border = BorderStroke(
                                    width = if (isChecked) 1.dp else 0.5.dp,
                                    color = if (isChecked) MutedBluePrimary.copy(alpha = 0.6f) else PaperBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedResponsibilities = if (checked) {
                                                selectedResponsibilities + resp
                                            } else {
                                                selectedResponsibilities - resp
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MutedBluePrimary,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = resp.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = resp.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = PaperBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_assign_role_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSave(selectedRole, selectedResponsibilities, customDesignation)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_role_assignments_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MutedBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Assignments", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MutedBluePrimary.copy(alpha = 0.18f) else SlateCard
            )
            .border(
                1.dp,
                if (isSelected) MutedBluePrimary else PaperBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("preset_${title.replace(" ", "_").lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MutedBluePrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MutedBlueDark else TextSecondary
            )
        }
    }
}
