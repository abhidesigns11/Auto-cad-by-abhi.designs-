package com.example.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "CAD Engineer",
    val membershipPlan: MembershipTier = MembershipTier.PRO_ENGINEER,
    val organization: String = "SS Fabrication & Engineering",
    val projectsCount: Int = 0,
    val maxCloudStorageProjects: Int = 50,
    val memberSince: String = "September 2026"
)

enum class MembershipTier(
    val title: String,
    val badge: String,
    val monthlyPrice: String,
    val maxProjects: Int,
    val features: List<String>
) {
    FREE_STARTER(
        title = "Starter Drafter",
        badge = "FREE",
        monthlyPrice = "$0 / mo",
        maxProjects = 3,
        features = listOf("Standard 2D Drafting", "AutoCAD DXF Export", "Watermarked PDF Print Sheet", "Max 3 Local Drawings")
    ),
    PRO_ENGINEER(
        title = "Pro Engineer",
        badge = "POPULAR",
        monthlyPrice = "$29 / mo",
        maxProjects = 50,
        features = listOf("Full 2D & 3D Isometric Viewport", "Manual 3D Solid Model CSG Builder", "Real-time Cloud Sync & Accounts", "Bill of Materials Auto-Calculation", "High-Resolution Engineering Title Blocks")
    ),
    ENTERPRISE_FABRICATOR(
        title = "Enterprise Fabricator",
        badge = "UNLIMITED",
        monthlyPrice = "$89 / mo",
        maxProjects = 9999,
        features = listOf("Unlimited Cloud Drawings & Backups", "Multi-Seat Engineering Collaboration", "Laser & Plasma CNC Nesting Cut Lists", "Custom SS 304/316 Flange & Nozzle Libraries", "Priority Engineering Tech Support")
    )
}
