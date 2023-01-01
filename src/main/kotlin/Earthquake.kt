import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter

// Query all available events with parameters (URL for remote API)
const val urlQuery =
    "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime="


/**
 * Properties of an earthquake events
 *
 * @property mag - Magnitude
 * @property place - place
 * @property time
 * @property type
 * @constructor Create empty Properties
 *
 * You can add more properties when needed
 */
//Class wich Objects store the Properties of Earthquake events recieved from API
@Serializable
data class Properties(
    val mag: Double?,
    val place: String?,
    var time: Long,
    val type: String,
){
    @Contextual
    val timeLD: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(time),ZoneId.systemDefault())
    val locTime: LocalTime
        get() = timeLD.toLocalTime()

    val date: LocalDate
        get() = timeLD.toLocalDate()

    val location:String
        get() =  if(place != null){
            place.split(",")[0]
        } else ""
    val region:String
         get() = if(place != null && place.split(",").size > 1){
                place.split(",")[1]
            } else ""
    }


/**
 * Feature - describes one earthquake event
 *
 * @property type = "Feature"
 * @property properties
 * @constructor Create empty Feature
 */

//Calss Representing the feautures including the Properties of Earthquake wvents
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

//class to hold deserialised objects from API-Call
@Serializable
data class FeatureCollection(
    val type: String,
    val features: Array<Feature>
)

//API-Call converts json-data from API to FeatureCollection
fun getEarthQuakes(from: LocalDate, to: LocalDate): FeatureCollection {
    //API-Call
    val jsonString = URL("$urlQuery${from}T00:00:00%2B01:00&endtime=${to}T23:59:59%2B01:00&limit=20000").readText()
    //Prepare Json Object for deserialization
    val json = Json { ignoreUnknownKeys = true }
    //deserialize JsonObject to a FeatureCollection
    return json.decodeFromString(jsonString)
}


