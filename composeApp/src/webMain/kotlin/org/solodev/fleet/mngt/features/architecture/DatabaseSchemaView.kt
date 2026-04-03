package org.solodev.fleet.mngt.features.architecture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_sql_file
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

data class DbTable(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val columns: List<DbColumn>,
)

data class DbColumn(
    val name: String,
    val type: String,
    val isPK: Boolean = false,
    val isFK: Boolean = false,
    val isUK: Boolean = false,
)

data class DbRelation(
    val fromTableId: String,
    val toTableId: String,
)

@Composable
fun DatabaseSchemaSection() {
    val tables = remember {
        listOf(
            // IDENTITY MODULE
            DbTable(
                "users",
                "USERS",
                520f,
                40f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("email", "varchar", isUK = true),
                    DbColumn("password_hash", "varchar"),
                    DbColumn("first_name", "varchar"),
                    DbColumn("last_name", "varchar"),
                    DbColumn("is_active", "boolean"),
                ),
            ),
            DbTable(
                "roles",
                "ROLES",
                850f,
                40f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("name", "varchar", isUK = true),
                    DbColumn("description", "text"),
                ),
            ),
            DbTable(
                "user_roles",
                "USER_ROLES",
                685f,
                280f,
                listOf(
                    DbColumn("user_id", "uuid", isPK = true, isFK = true),
                    DbColumn("role_id", "uuid", isPK = true, isFK = true),
                ),
            ),
            DbTable(
                "staff",
                "STAFF_PROFILES",
                250f,
                160f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("user_id", "uuid", isFK = true),
                    DbColumn("employee_id", "varchar", isUK = true),
                    DbColumn("department", "varchar"),
                ),
            ),
            DbTable(
                "tokens",
                "VERIFICATION_TOKENS",
                520f,
                280f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("user_id", "uuid", isFK = true),
                    DbColumn("token", "varchar"),
                    DbColumn("expiry", "tstz"),
                ),
            ),

            // FLEET MODULE
            DbTable(
                "vehicles",
                "VEHICLES",
                150f,
                450f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("plate_number", "varchar", isUK = true),
                    DbColumn("make", "varchar"),
                    DbColumn("model", "varchar"),
                    DbColumn("status", "varchar"),
                    DbColumn("current_odo", "int"),
                ),
            ),
            DbTable(
                "odo",
                "ODOMETER_READINGS",
                20f,
                650f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("reading_km", "integer"),
                    DbColumn("recorded_at", "tstz"),
                ),
            ),
            DbTable(
                "maint_sched",
                "MAINT_SCHEDULES",
                380f,
                650f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("schedule_type", "varchar"),
                    DbColumn("next_due", "tstz"),
                ),
            ),
            DbTable(
                "incidents",
                "VEHICLE_INCIDENTS",
                180f,
                820f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("description", "text"),
                    DbColumn("severity", "varchar"),
                ),
            ),

            // RENTAL MODULE
            DbTable(
                "customers",
                "CUSTOMERS",
                820f,
                450f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("user_id", "uuid", isFK = true, isUK = true),
                    DbColumn("email", "varchar"),
                    DbColumn("license_no", "varchar"),
                ),
            ),
            DbTable(
                "rentals",
                "RENTALS",
                1020f,
                620f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("customer_id", "uuid", isFK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("status", "varchar"),
                    DbColumn("start_date", "tstz"),
                ),
            ),
            DbTable(
                "charges",
                "RENTAL_CHARGES",
                800f,
                850f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("rental_id", "uuid", isFK = true),
                    DbColumn("amount", "integer"),
                    DbColumn("type", "varchar"),
                ),
            ),
            DbTable(
                "payments",
                "RENTAL_PAYMENTS",
                1120f,
                850f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("rental_id", "uuid", isFK = true),
                    DbColumn("amount", "integer"),
                    DbColumn("status", "varchar"),
                ),
            ),

            // DRIVER MODULE
            DbTable(
                "drivers",
                "DRIVERS",
                150f,
                1050f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("user_id", "uuid", isFK = true),
                    DbColumn("license_no", "varchar", isUK = true),
                    DbColumn("is_active", "boolean"),
                ),
            ),
            DbTable(
                "shifts",
                "DRIVER_SHIFTS",
                50f,
                1250f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("driver_id", "uuid", isFK = true),
                    DbColumn("start_time", "tstz"),
                ),
            ),
            DbTable(
                "assignments",
                "DRIVER_ASSIGNMENTS",
                450f,
                1050f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("driver_id", "uuid", isFK = true),
                ),
            ),

            // TRACKING
            DbTable(
                "loc_hist",
                "LOCATION_HISTORY",
                1050f,
                1050f,
                listOf(
                    DbColumn("id", "bigint", isPK = true),
                    DbColumn("vehicle_id", "uuid", isFK = true),
                    DbColumn("lon", "float"),
                    DbColumn("lat", "float"),
                ),
            ),
            DbTable(
                "routes",
                "GEO_ROUTES",
                820f,
                1250f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("name", "varchar"),
                    DbColumn("geom", "geometry"),
                ),
            ),

            // ACCOUNTING
            DbTable(
                "invoices",
                "INVOICES",
                1050f,
                1250f,
                listOf(
                    DbColumn("id", "uuid", isPK = true),
                    DbColumn("customer_id", "uuid", isFK = true),
                    DbColumn("amount", "integer"),
                ),
            ),
        )
    }

    val relations = remember {
        listOf(
            DbRelation("user_roles", "users"),
            DbRelation("user_roles", "roles"),
            DbRelation("staff", "users"),
            DbRelation("tokens", "users"),
            DbRelation("odo", "vehicles"),
            DbRelation("maint_sched", "vehicles"),
            DbRelation("incidents", "vehicles"),
            DbRelation("customers", "users"),
            DbRelation("drivers", "users"),
            DbRelation("rentals", "customers"),
            DbRelation("rentals", "vehicles"),
            DbRelation("charges", "rentals"),
            DbRelation("payments", "rentals"),
            DbRelation("shifts", "drivers"),
            DbRelation("assignments", "vehicles"),
            DbRelation("assignments", "drivers"),
            DbRelation("loc_hist", "vehicles"),
            DbRelation("invoices", "customers"),
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
        Text(
            "Database Table Schemas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            "Comprehensive entity-relationship diagram of the fleet management ecosystem",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        Box(modifier = Modifier.fillMaxWidth().height(1450.dp)) {
            // Background grid / Groups
            DiagramGroup("Identity & Access Management", Modifier.offset(200.dp, 10.dp).size(850.dp, 400.dp))
            DiagramGroup("Fleet & Maintenance Ecosystem", Modifier.offset(10.dp, 410.dp).size(550.dp, 550.dp))
            DiagramGroup("Rental & Customer Operations", Modifier.offset(750.dp, 410.dp).size(600.dp, 600.dp))
            DiagramGroup("Driver & Workforce Management", Modifier.offset(10.dp, 1000.dp).size(650.dp, 400.dp))
            DiagramGroup("Spatial Tracking & Invoicing", Modifier.offset(750.dp, 1000.dp).size(600.dp, 400.dp))

            // Layer 1: Connections
            val connectionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                relations.forEach { rel ->
                    val from = tables.find { it.id == rel.fromTableId }
                    val to = tables.find { it.id == rel.toTableId }
                    if (from != null && to != null) {
                        val path = Path().apply {
                            moveTo(from.x.dp.toPx() + 100.dp.toPx(), from.y.dp.toPx() + 60.dp.toPx())
                            cubicTo(
                                from.x.dp.toPx() + 100.dp.toPx(),
                                (from.y + to.y).dp.toPx() / 2,
                                to.x.dp.toPx() + 100.dp.toPx(),
                                (from.y + to.y).dp.toPx() / 2,
                                to.x.dp.toPx() + 100.dp.toPx(),
                                to.y.dp.toPx() + 60.dp.toPx(),
                            )
                        }
                        drawPath(path, connectionColor, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

                        // Small arrow head at the "to" end
                        drawCircle(connectionColor, radius = 3.dp.toPx(), center = Offset(to.x.dp.toPx() + 100.dp.toPx(), to.y.dp.toPx() + 60.dp.toPx()))
                    }
                }
            }

            // Layer 2: Tables
            tables.forEach { table ->
                TableCard(table)
            }
        }
    }
}

@Composable
fun TableCard(table: DbTable) {
    Card(
        modifier = Modifier
            .offset { IntOffset(table.x.dp.toPx().roundToInt(), table.y.dp.toPx().roundToInt()) }
            .width(220.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        ),
    ) {
        Column {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_sql_file),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    table.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Columns
            Column(Modifier.padding(8.dp)) {
                table.columns.forEach { col ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (col.isPK) {
                                Text("PK", color = Color(0xFFE91E63), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                            }
                            if (col.isFK) {
                                Text("FK", color = Color(0xFF2196F3), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                            }
                            if (col.isUK) {
                                Text("UK", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                            }
                            Text(col.name, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, fontWeight = if (col.isPK) FontWeight.Bold else FontWeight.Normal)
                        }
                        Text(col.type, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
