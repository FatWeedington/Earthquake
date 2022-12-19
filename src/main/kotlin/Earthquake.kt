import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
    val timeText: String = timeLD.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateText: String = timeLD.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    
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

fun getEarthQuakes(from: LocalDate, to: LocalDate): FeatureCollection {
    val jsonString = URL("$urlQuery${from}T00:00:00%2B01:00&endtime=${to}T23:59:59%2B01:00").readText()
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(jsonString)
}


