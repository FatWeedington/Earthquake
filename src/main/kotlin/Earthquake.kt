import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.TimeZone

// Query all available events with parameters
const val urlQuery =
    "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime="

@Serializable
data class EarthQuakesCount(
    val count: Int,
    val maxAllowed: Int
)

/**
 * Properties of an earthquake events
 *
 * @property mag - Magnitude
 * @property place - place
 * @property time
 * @property type
 * @property title
 * @constructor Create empty Properties
 *
 * You can add more properties when needed
 */
@Serializable
data class Properties(
    val mag: Double?,
    val place: String?,
    var time: Long,
    val type: String,
    val title: String
){
    @Contextual
    val timeLD: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time),ZoneId.systemDefault())
    
}

/**
 * Feature - describes one earthquake event
 *
 * @property type = "Feature"
 * @property properties
 * @constructor Create empty Feature
 */
@Serializable
data class Feature(
    val type: String,
    val properties: Properties
)

/**
 * Feature collection - Collection of earthquake events
 *
 * @property type = "FeatureCollection"
 * @property features - Array of earthquake events
 * @constructor Create empty Feature collection
 */
@Serializable
data class FeatureCollection(
    val type: String,
    val features: Array<Feature>
)


