import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

// Count all available event
const val urlCount = "https://earthquake.usgs.gov/fdsnws/event/1/count?format=geojson"

// Count all available event in a time frame
const val urlCount2 =
    "https://earthquake.usgs.gov/fdsnws/event/1/count?format=geojson&starttime=2022-01-01&endtime=2022-01-31"

// Count all available event from a given start time & updateed after a given time
const val urlCount3 =
    "https://earthquake.usgs.gov/fdsnws/event/1/count?format=geojson&starttime=2022-10-21&updatedafter=2022-10-21T8:00:00"

// Query all available events with parameters
const val urlQuery =
    "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2022-12-4&updatedafter=2022-12-4T8:00:00"

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
    val timeLD = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
    
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

fun getEarthQuakesCount(): EarthQuakesCount {
    val jsonString = URL(urlCount3).readText()
    return Json.decodeFromString(jsonString)
}

fun getEarthQuakes(): FeatureCollection {
    val jsonString = URL(urlQuery).readText()
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(jsonString)
}