package com.example.data.model

enum class UserRole(val code: String, val label: String, val description: String) {
    ADMIN("ADMIN", "Administrator", "Full system configuration, security & user management"),
    OM("OM", "O&M", "Operations & Maintenance field engineer"),
    SUPERVISION("SUPERVISION", "Supervision", "NOC & regional supervisor monitoring site access"),
    TECHNICIAN("TECHNICIAN", "Technician", "Field technician executing audits and photo logging"),
    DISPATCHER("DISPATCHER", "Dispatcher", "Work order & site access routing coordinator"),
    ACQUISITION("ACQUISITION", "Acquisition", "Site acquisition & landlord lease clearance officer")
}
