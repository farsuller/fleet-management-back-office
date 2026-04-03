package org.solodev.fleet.mngt.features.architecture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.ic_database
import fleetmanagementbackoffice.composeapp.generated.resources.ic_mobile_app
import fleetmanagementbackoffice.composeapp.generated.resources.ic_redis
import fleetmanagementbackoffice.composeapp.generated.resources.ic_sql_file
import fleetmanagementbackoffice.composeapp.generated.resources.ic_web_app

data class ArchitectureNode(
    val id: String,
    val label: String,
    val tier: NodeTier,
    val x: Float,
    val y: Float,
    val icon: String? = null
)

data class ArchitectureEdge(
    val fromId: String,
    val toId: String
)

enum class NodeTier {
    CLIENT, GATEWAY, BUSINESS, STORAGE
}

@Composable
fun ComposeArchitectureMap(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Nodes for the high-density layout
    val nodes = remember {
        listOf(
            // External Clients
            ArchitectureNode("MOBILE", "Mobile Driver App", NodeTier.CLIENT, 300f, 60f, "ic_mobile_app.png"),
            ArchitectureNode("WEB", "BackOffice Web App", NodeTier.CLIENT, 1200f, 60f, "ic_web_app.png"),
            
            // Interfaces
            ArchitectureNode("REST", "REST API /v1", NodeTier.GATEWAY, 300f, 180f),
            ArchitectureNode("WS", "WebSocket Live Stream", NodeTier.GATEWAY, 1200f, 180f),
            
            // Core Domains
            ArchitectureNode("LEDGER", "Double-Entry Ledger", NodeTier.BUSINESS, 60f, 440f, "ic_sql_file.png"),
            ArchitectureNode("RENTAL", "Rental & Availability", NodeTier.BUSINESS, 220f, 440f, "ic_sql_file.png"),
            ArchitectureNode("VEHICLE", "Vehicle Lifecycle", NodeTier.BUSINESS, 380f, 440f, "ic_sql_file.png"),
            ArchitectureNode("SERVICE", "Service Workflows", NodeTier.BUSINESS, 540f, 440f, "ic_sql_file.png"),
            ArchitectureNode("USER", "User Identity", NodeTier.BUSINESS, 700f, 440f, "ic_sql_file.png"),
            
            // Hardening
            ArchitectureNode("SEC", "JWT/RBAC Security", NodeTier.BUSINESS, 980f, 320f),
            ArchitectureNode("RATE", "Token-Bucket Rate Limiter", NodeTier.BUSINESS, 980f, 380f),
            ArchitectureNode("IDEM", "Idempotency Layer", NodeTier.BUSINESS, 980f, 440f),
            ArchitectureNode("LOCK", "Advisory Locking Engine", NodeTier.BUSINESS, 980f, 500f),
            
            // Spatial
            ArchitectureNode("SNAP", "PostGIS Snap-to-Route", NodeTier.BUSINESS, 1240f, 440f),
            ArchitectureNode("DELTA", "Delta-Encoded Broadcasts", NodeTier.BUSINESS, 1400f, 440f),
            
            // Storage
            ArchitectureNode("DB", "PostgreSQL 15", NodeTier.STORAGE, 300f, 740f, "ic_database.png"),
            ArchitectureNode("REDIS", "Redis Cache/Pub-Sub", NodeTier.STORAGE, 1200f, 740f, "ic_redis.png")
        )
    }

    // Connections representing the data flow
    val edges = remember {
        listOf(
            ArchitectureEdge("MOBILE", "REST"),
            ArchitectureEdge("WEB", "REST"),
            ArchitectureEdge("WEB", "WS"),
            
            ArchitectureEdge("REST", "LEDGER"),
            ArchitectureEdge("REST", "RENTAL"),
            ArchitectureEdge("REST", "VEHICLE"),
            ArchitectureEdge("REST", "SERVICE"),
            
            ArchitectureEdge("REST", "SEC"), // Cross-cutting flow
            
            ArchitectureEdge("WS", "DELTA"),
            ArchitectureEdge("SNAP", "DELTA"),
            
            ArchitectureEdge("LEDGER", "DB"),
            ArchitectureEdge("RENTAL", "DB"),
            ArchitectureEdge("VEHICLE", "DB"),
            
            ArchitectureEdge("DELTA", "REDIS")
        )
    }

    Box(modifier = modifier.fillMaxWidth().height(840.dp)) {
        // --- Layer 1: Container Groups ---
        // External Clients Group
        DiagramGroup("External Clients", Modifier.offset(230.dp, 10.dp).size(1150.dp, 110.dp))
        
        // Main Backend Group
        Box(Modifier.offset(40.dp, 140.dp).size(1550.dp, 560.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))) {
            Text("Fleet BackOffice (Ktor Monolith)", Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            
            // Sub-groups
            DiagramGroup("Interfaces", Modifier.offset(80.dp, 10.dp).size(1300.dp, 80.dp))
            DiagramGroup("Core Domain Modules", Modifier.offset(10.dp, 110.dp).size(860.dp, 400.dp))
            DiagramGroup("Infrastructure & Service Hardening", Modifier.offset(880.dp, 110.dp).size(300.dp, 400.dp))
            DiagramGroup("Spatial Visualization Engine", Modifier.offset(1200.dp, 110.dp).size(340.dp, 400.dp))
        }
        
        // Persistence Group
        DiagramGroup("Data Persistence", Modifier.offset(230.dp, 710.dp).size(1150.dp, 110.dp))

        // --- Layer 2: The Connectors ---
        val connectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            edges.forEach { edge ->
                val start = nodes.find { it.id == edge.fromId }
                val end = nodes.find { it.id == edge.toId }

                if (start != null && end != null) {
                    val startOff = Offset(start.x.dp.toPx() + 65.dp.toPx(), start.y.dp.toPx() + 50.dp.toPx())
                    val endOff = Offset(end.x.dp.toPx() + 65.dp.toPx(), end.y.dp.toPx())

                    val path = Path().apply {
                        moveTo(startOff.x, startOff.y)
                        cubicTo(
                            startOff.x, startOff.y + 40.dp.toPx(),
                            endOff.x, endOff.y - 40.dp.toPx(),
                            endOff.x, endOff.y
                        )
                    }
                    
                    drawPath(
                        path = path,
                        color = connectionColor,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // --- Layer 3: The Component Nodes ---
        nodes.forEach { node ->
            val tierColor = when(node.tier) {
                NodeTier.CLIENT -> MaterialTheme.colorScheme.outline
                NodeTier.GATEWAY -> MaterialTheme.colorScheme.secondary
                NodeTier.BUSINESS -> MaterialTheme.colorScheme.primary
                NodeTier.STORAGE -> MaterialTheme.colorScheme.tertiary
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(node.x.dp.toPx().roundToInt(), node.y.dp.toPx().roundToInt()) }
                    .size(width = 130.dp, height = 50.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(tierColor.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        NodeIcon(node.icon)
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            modifier = Modifier.padding(vertical = 4.dp).weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeIcon(iconName: String?) {
    if (iconName == null) return
    
    val painter = when (iconName) {
        "ic_mobile_app.png" -> painterResource(Res.drawable.ic_mobile_app)
        "ic_web_app.png" -> painterResource(Res.drawable.ic_web_app)
        "ic_database.png" -> painterResource(Res.drawable.ic_database)
        "ic_redis.png" -> painterResource(Res.drawable.ic_redis)
        "ic_sql_file.png" -> painterResource(Res.drawable.ic_sql_file)
        else -> null
    }

    painter?.let {
        Image(
            painter = it,
            contentDescription = null,
            modifier = Modifier.size(24.dp).padding(end = 8.dp)
        )
    }
}

@Composable
fun DiagramGroup(label: String, modifier: Modifier = Modifier) {
    Box(modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(4.dp))) {
        Text(
            text = label,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
