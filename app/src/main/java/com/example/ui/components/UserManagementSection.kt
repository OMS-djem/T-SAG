package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
    onCreateUser: (name: String, email: String, phone: String, role: String, department: String, region: String) -> Unit,
    onUpdateUser: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onSelectTechnician: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    val filteredUsers = users.filter { user ->
        val matchesRole = selectedRoleFilter == "ALL" || user.role.equals(selectedRoleFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.department.contains(searchQuery, ignoreCase = true) ||
                user.region.contains(searchQuery, ignoreCase = true)
        matchesRole && matchesSearch
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Action Bar: Add User & Search
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "User & Role Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${users.size} registered users across 6 telecom roles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                modifier = Modifier.testTag("add_user_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add User", fontSize = 12.sp)
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

        // Role Filter Chips
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
            items(UserRole.values()) { role ->
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredUsers.forEach { user ->
                    UserCardItem(
                        user = user,
                        onEdit = { userToEdit = user },
                        onDelete = { userToDelete = user },
                        onSelectTechnician = { onSelectTechnician(user) }
                    )
                }
            }
        }
    }

    // CREATE USER DIALOG
    if (showCreateDialog) {
        UserFormDialog(
            title = "Add New User",
            initialUser = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, email, phone, role, dept, region ->
                onCreateUser(name, email, phone, role, dept, region)
                showCreateDialog = false
            }
        )
    }

    // EDIT USER DIALOG
    userToEdit?.let { user ->
        UserFormDialog(
            title = "Edit User: ${user.name}",
            initialUser = user,
            onDismiss = { userToEdit = null },
            onSave = { name, email, phone, role, dept, region ->
                onUpdateUser(
                    user.copy(
                        name = name,
                        email = email,
                        phone = phone,
                        role = role,
                        department = dept,
                        region = region
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
            title = { Text("Delete User?") },
            text = { Text("Are you sure you want to remove '${user.name}' (${user.role}) from the directory?") },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectTechnician: () -> Unit
) {
    val roleColor = getRoleBadgeColor(user.role)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle with initials
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.department} • ${user.region}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Role Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roleColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = user.role,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: Set Active Tech, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectTechnician,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Active Field Tech", fontSize = 10.sp)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AccessRefusedRed, modifier = Modifier.size(16.dp))
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
    onSave: (name: String, email: String, phone: String, role: String, department: String, region: String) -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var email by remember { mutableStateOf(initialUser?.email ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var selectedRole by remember { mutableStateOf(initialUser?.role ?: "TECHNICIAN") }
    var department by remember { mutableStateOf(initialUser?.department ?: "Field Services") }
    var region by remember { mutableStateOf(initialUser?.region ?: "National") }

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
                        value = UserRole.values().firstOrNull { it.code == selectedRole }?.label ?: selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Role *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        UserRole.values().forEach { role ->
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
                        onSave(name, email, phone, selectedRole, department, region)
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
