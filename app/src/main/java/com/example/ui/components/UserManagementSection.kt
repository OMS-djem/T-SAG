package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary

@Composable
fun UserManagementSection(
    users: List<UserEntity>,
    isAdmin: Boolean = true,
    onCreateUser: (name: String, email: String, phone: String, role: String, department: String, region: String) -> Unit,
    onUpdateUser: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onActivateUser: (UserEntity, Boolean) -> Unit = { _, _ -> },
    onSyncSupabaseUsers: (() -> Unit)? = null,
    isSyncingUsers: Boolean = false,
    onSelectTechnician: (UserEntity) -> Unit,
    onAddToToGroup: ((String) -> Unit)? = null,
    onAddToCcGroup: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    val pendingCount = users.count { !it.isActive }

    val filteredUsers = users.filter { user ->
        val matchesFilter = when (selectedRoleFilter) {
            "ALL" -> true
            "PENDING" -> !user.isActive
            else -> user.role.equals(selectedRoleFilter, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.department.contains(searchQuery, ignoreCase = true) ||
                user.region.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Action Bar: Add User, Sync Supabase & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "User & Authorization Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${users.size} users (${pendingCount} pending activation)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pendingCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (pendingCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onSyncSupabaseUsers != null) {
                        OutlinedButton(
                            onClick = onSyncSupabaseUsers,
                            enabled = !isSyncingUsers,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("sync_supabase_users_button")
                        ) {
                            if (isSyncingUsers) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                        modifier = Modifier.height(34.dp).testTag("add_user_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Read-Only (Admin required for CRUD & activation)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, email, department...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role & Pending Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedRoleFilter == "ALL",
                    onClick = { selectedRoleFilter = "ALL" },
                    label = { Text("All (${users.size})", fontSize = 11.sp) }
                )
            }

            if (pendingCount > 0) {
                item {
                    FilterChip(
                        selected = selectedRoleFilter == "PENDING",
                        onClick = { selectedRoleFilter = "PENDING" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pending ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFFEF3C7),
                            labelColor = Color(0xFFB45309),
                            selectedContainerColor = Color(0xFFFDE68A),
                            selectedLabelColor = Color(0xFF92400E)
                        )
                    )
                }
            }

            items(UserRole.entries) { role ->
                val count = users.count { it.role.equals(role.code, ignoreCase = true) }
                FilterChip(
                    selected = selectedRoleFilter == role.code,
                    onClick = { selectedRoleFilter = role.code },
                    label = { Text("${role.label} ($count)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getRoleBadgeColor(role.code).copy(alpha = 0.15f),
                        selectedLabelColor = getRoleBadgeColor(role.code)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Users List
        if (filteredUsers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No users match current filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredUsers.forEach { user ->
                    UserCardItem(
                        user = user,
                        isAdmin = isAdmin,
                        onActivate = { active -> onActivateUser(user, active) },
                        onEdit = { userToEdit = user },
                        onDelete = { userToDelete = user },
                        onSelectTechnician = { onSelectTechnician(user) },
                        onAddToToGroup = onAddToToGroup,
                        onAddToCcGroup = onAddToCcGroup
                    )
                }
            }
        }
    }

    // CREATE USER DIALOG
    if (showCreateDialog) {
        UserFormDialog(
            title = "Add New User Account",
            initialUser = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, email, phone, role, dept, region, isActive ->
                onCreateUser(name, email, phone, role, dept, region)
                showCreateDialog = false
            }
        )
    }

    // EDIT USER DIALOG
    userToEdit?.let { user ->
        UserFormDialog(
            title = "Edit User & Authorization: ${user.name}",
            initialUser = user,
            onDismiss = { userToEdit = null },
            onSave = { name, email, phone, role, dept, region, isActive ->
                onUpdateUser(
                    user.copy(
                        name = name,
                        email = email,
                        phone = phone,
                        role = role,
                        department = dept,
                        region = region,
                        isActive = isActive
                    )
                )
                userToEdit = null
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = AccessRefusedRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete User Account?") },
            text = { Text("Are you sure you want to remove '${user.name}' (${user.role}) from the local and Supabase directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccessRefusedRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserCardItem(
    user: UserEntity,
    isAdmin: Boolean = true,
    onActivate: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectTechnician: () -> Unit,
    onAddToToGroup: ((String) -> Unit)? = null,
    onAddToCcGroup: ((String) -> Unit)? = null
) {
    val roleColor = getRoleBadgeColor(user.role)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (user.isActive) 1.dp else 3.dp),
        border = if (!user.isActive) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)) else null,
        modifier = Modifier.fillMaxWidth().testTag("user_card_${user.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name/Dept, Status & Role Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Avatar circle with initials
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.department} • ${user.region}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badges Column: Active status & Role badge
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = user.role,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Activation Status Badge
                    if (user.isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccessAccordedGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AccessAccordedGreen, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccessAccordedGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(10.dp))
                                Text(
                                    text = "PENDING APPROVAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = user.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = user.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: Quick Mail Group Assignment, Set Active Tech, Admin Activation Button, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectTechnician,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Tech", fontSize = 10.sp)
                    }

                    if (onAddToToGroup != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TelecomNavyPrimary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAddToToGroup(user.email) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text("+ TO Group", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelecomNavyPrimary)
                            }
                        }
                    }

                    if (onAddToCcGroup != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccessAccordedGreen.copy(alpha = 0.12f),
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAddToCcGroup(user.email) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text("+ CC Group", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccessAccordedGreen)
                            }
                        }
                    }
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Admin One-Tap Activation Button
                        if (!user.isActive) {
                            Button(
                                onClick = { onActivate(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccessAccordedGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).testTag("activate_user_${user.id}")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Activate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onActivate(false) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).testTag("deactivate_user_${user.id}")
                            ) {
                                Text("Deactivate", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("edit_user_${user.id}")) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("delete_user_${user.id}")) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AccessRefusedRed, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    title: String,
    initialUser: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, role: String, department: String, region: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var email by remember { mutableStateOf(initialUser?.email ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var selectedRole by remember { mutableStateOf(initialUser?.role ?: "TECHNICIAN") }
    var department by remember { mutableStateOf(initialUser?.department ?: "Field Services") }
    var region by remember { mutableStateOf(initialUser?.region ?: "National") }
    var isActive by remember { mutableStateOf(initialUser?.isActive ?: true) }

    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Corporate Email *") },
                    placeholder = { Text("username@mobiserveholding.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+213 550...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Role Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isRoleDropdownExpanded,
                    onExpandedChange = { isRoleDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = UserRole.entries.firstOrNull { it.code == selectedRole }?.label ?: selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Operational Role *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = role.label, fontWeight = FontWeight.Bold)
                                        Text(text = role.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedRole = role.code
                                    isRoleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = { Text("Region") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Account Activation & Authorization Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Activated & Authorized", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                text = if (isActive) "User can sign in and file audits" else "Waiting for admin activation",
                                fontSize = 10.sp,
                                color = if (isActive) AccessAccordedGreen else Color(0xFFD97706)
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccessAccordedGreen
                            )
                        )
                    }
                }

                errorMessage?.let {
                    Text(text = it, color = AccessRefusedRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank()) {
                        errorMessage = "Please enter full name and email."
                    } else {
                        onSave(name, email, phone, selectedRole, department, region, isActive)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getRoleBadgeColor(role: String): Color {
    return when (role.uppercase()) {
        "ADMIN" -> Color(0xFF6B21A8) // Purple
        "OM" -> Color(0xFF0284C7)    // Cyan/Blue
        "SUPERVISION" -> Color(0xFF059669) // Emerald
        "TECHNICIAN" -> Color(0xFFD97706)  // Amber
        "DISPATCHER" -> Color(0xFFE11D48)  // Rose
        "ACQUISITION" -> Color(0xFF0F766E) // Teal
        else -> TelecomNavyPrimary
    }
}
