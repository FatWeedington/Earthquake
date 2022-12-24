import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.StringConverter
import javafx.util.converter.LocalDateStringConverter
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

//variables which limits API-Call in a certain timeframe
private var fromDate = LocalDate.now().toProperty()
private var toDate = LocalDate.now().toProperty()

//variable which filters results of API call to a specific region
private var regionFilter = "".toProperty()

//List of earthquake-properties Class which is used to update GUI Elements automatically
private var earthQuakes = mutableListOf<Properties>().asObservable()

//main-function: launches GUI
fun main(args: Array<String>) {
    launch<GUI>(args)
}

//Starts Stage/GUI
class GUI : App(MainView::class) {
    override fun start(stage: Stage) {
        stage.width = 800.0
        stage.height = 600.0
        stage.onCloseRequest = EventHandler {
            exitProcess(0)
        }
        super.start(stage)
    }

}


//class which holds central window of the Application
class MainView : View("Earthquakes") {

    //init block starts the reoccurring API Call throughout runtime
    init {
            startTimer()
        }

    //Borderpane which holds all elements of main Window
    override val root = borderpane {
        top {
            //Menubar for the Actions which can be done
            menubar {
                //File Menu
                menu("File") {
                    //Menu Item which on Action starts the exportCSV Task and shows a Warning alert Message on failure
                    item("Export CSV") {
                        action {
                            try {
                                exportCSV()
                            } catch (e: Exception) {
                                alert(Alert.AlertType.WARNING, "File could not be saved", content = "${e.message}")
                            }
                        }
                    }
                    //Menu Item which on Action starts the importCSV Task and shows a Warning alert Message on failure
                    item("Import CSV") {
                        action {
                            try {
                                find<CsvWindow>().openWindow()
                            } catch (e: Exception) {
                                alert(Alert.AlertType.WARNING, "File could not be read", content = "${e.message}")
                            }
                        }
                    }
                }
                //Tools menu for showing graph of current Data
                menu("Tools") {
                    // Menu Item which on Action starts showGraph Task and shows a Warning alert Message on failure
                    item("Show Graph")
                    action {
                        if (getDailyEvents().size < 2) {
                            alert(
                                Alert.AlertType.WARNING,
                                "Not enough Days",
                                content = "Please specify a time-range from at least two Days"
                            )
                        } else {
                            find<ChartWindow>().openWindow()
                        }
                    }
                }
            }
        }

        center {
            //creates Tableview which is bind to automatically updated earthQuakes List
            tableview(earthQuakes) {
                readonlyColumn("Type", Properties::type).minWidth(80).maxWidth(100)
                readonlyColumn("Location", Properties::location).minWidth(100)
                readonlyColumn("Region", Properties::region).minWidth(130).maxWidth(180)
                readonlyColumn("Date", Properties::dateText).minWidth(70).maxWidth(100)
                readonlyColumn("Time", Properties::timeText).minWidth(50).maxWidth(100)
                readonlyColumn("Magnitude", Properties::mag).minWidth(70).maxWidth(100)

                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }
        bottom {
            //defines date-pickers bind to fromDate/toDate variables to limit api-call and displayed Properties
            hbox(spacing = 20) {
                minHeight = 35.0
                hbox(spacing = 8) {
                    hboxConstraints {
                        alignment = Pos.CENTER_LEFT
                    }
                    label { text = "From:" }
                    datepicker(fromDate) {
                        valueProperty().bindBidirectional(fromDate)
                        converter = LocalDateStringConverter(DateTimeFormatter.ofPattern("dd.MM.yyyy"),null)
                        //update Datepicker Cells to limit closable dates
                        setDayCellFactory {
                            object : DateCell() {
                                override fun updateItem(item: LocalDate, empty: Boolean) {
                                    super.updateItem(item, empty)
                                    isDisable =
                                        item.isAfter(LocalDate.now()) || item.isBefore(
                                            LocalDate.now().minusYears(5)
                                        ) || item.isAfter(toDate.value)
                                }
                            }
                        }
                        setOnAction {
                            updateEarthQuakes()
                        }
                    }
                    hbox(spacing = 8) {
                        hboxConstraints {
                            alignment = Pos.CENTER_LEFT
                        }
                        label { text = "To:" }
                        datepicker(toDate) {
                            valueProperty().bindBidirectional(toDate)
                            converter = LocalDateStringConverter(DateTimeFormatter.ofPattern("dd.MM.yyyy"),null)
                            setDayCellFactory {
                                object : DateCell() {
                                    override fun updateItem(item: LocalDate, empty: Boolean) {
                                        super.updateItem(item, empty)
                                        isDisable =
                                            item.isAfter(LocalDate.now()) || item.isBefore(
                                                fromDate.value
                                            )
                                    }
                                }
                            }
                            setOnAction {
                                updateEarthQuakes()
                            }
                        }
                    }
                    hbox(spacing = 8) {
                        hboxConstraints {
                            alignment = Pos.CENTER_RIGHT
                        }
                        label { text = "Region:" }
                        textfield {
                            textProperty().bindBidirectional(regionFilter)
                            }
                            setOnKeyTyped {
                                updateEarthQuakes()
                            }
                    }
                }
            }
        }
    }
}

//starts Timer to update tableview items automatically, delay can be provided in case of error
fun startTimer(delay: Long = 0L):Timer{
    val timer = Timer()
    timer.scheduleAtFixedRate(object: TimerTask() {
        override fun run() {
            updateEarthQuakes(timer)
        }

    },
        delay, 5000 )
    return timer
}

/*Update Items of Earthquake List asynchronously and shows an Error alert message if failed
Additional filters list to given search string in Region */
private fun updateEarthQuakes(timer: Timer? = null) {
    earthQuakes.asyncItems {
        if(regionFilter.value != ""){
            getEarthQuakes(fromDate.value, toDate.value)
                .features.map { i -> i.properties }
                .filter { it.region.contains(regionFilter.value,true) }
        }
        else getEarthQuakes(fromDate.value, toDate.value)
            .features.map { i -> i.properties }
    } fail {
        if (timer != null) {
            timer.cancel()
            timer.purge()
        }
        val alert = alert(Alert.AlertType.ERROR, "Error", it.message) {
        }.showAndWait()
        if (alert.get() == ButtonType.OK) {
            startTimer(10000)
        }
    }
}



//class which represents a new window with a tableview to display results of a saved CSV File
class CsvWindow : Fragment("Imported Data") {
    override val root =
        tableview(importCSV()) {
            readonlyColumn("Type", Properties::type).minWidth(80).maxWidth(100)
            readonlyColumn("Location", Properties::location).minWidth(100)
            readonlyColumn("Region", Properties::region).minWidth(130).maxWidth(180)
            readonlyColumn("Date", Properties::dateText).minWidth(70).maxWidth(100)
            readonlyColumn("Time", Properties::timeText).minWidth(50).maxWidth(100)
            readonlyColumn("Magnitude", Properties::mag).minWidth(70).maxWidth(100)

            setPrefSize(600.0,400.0)
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vboxConstraints {
                vGrow = Priority.ALWAYS
            }
        }
}

//Creates a Map of the event count within a day to display the data in a line chart
private fun getDailyEvents():Map<Long, Int>{
    val results = mutableMapOf<Long, Int>()
    earthQuakes.forEach {

        val date = it.timeLD.toLocalDate().toEpochDay()
        if (results.containsKey(date)) {
            results.replace(date, results.getValue(date) + 1)
        } else {
            results.putIfAbsent(date, 1)
        }
    }
    return results
}

//sets Min/Max date for better presentation of x-Axis
private fun getMinDate() = earthQuakes.minOfOrNull { it.timeLD }?.toLocalDate()?.toEpochDay()?.toDouble()!!
private fun getMaxDate() = earthQuakes.maxOfOrNull { it.timeLD }?.toLocalDate()?.toEpochDay()?.toDouble()!!

//function to format xAxis of line chart to chronologically order results
private fun setXAxis(min: Double, max: Double): NumberAxis {
    val xAxis = NumberAxis()
    xAxis.isAutoRanging = false
    xAxis.tickUnit = 1.0
    xAxis.lowerBound = min
    xAxis.upperBound = max
    xAxis.tickLabelFormatter = object : StringConverter<Number?>() {
        override fun toString(num: Number?): String {
            return LocalDate.ofEpochDay(num!!.toLong()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }

        override fun fromString(string: String?): Number? {
            return null
        }
    }
    return xAxis
}

//class which represents a new window with a Line chart to display progress of earthquake events within the in the mainWindow defined Timeframe
class ChartWindow : Fragment("Chart") {
    override val root =
        linechart(
            "Earthquake Events ${fromDate.value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} - ${
                toDate.value.format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                )
            }", setXAxis(getMinDate(), getMaxDate()), NumberAxis()
        )
        {
            setPrefSize(600.0, 400.0)
            series("Daily earthquake events ${regionFilter.value}") {
                getDailyEvents().forEach {
                    data(it.key, it.value)
                }
            }

        }
}

    // Lets user pick a path to store the on the main window displayed list in csv file
    private fun exportCSV() {
        if(!File("data").isDirectory){
            Files.createDirectory(Paths.get("data"))}

        val fileName =  chooseFile("Choose Folder", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")),
                        File("data"),
                        FileChooserMode.Save){
            initialFileName = getFileName()
        }
        if(fileName.size == 1){
            val writer = fileName[0].bufferedWriter()
            writer.use {
                for (e in earthQuakes) {
                    writer.append("${e.type},${e.location},${e.region},${e.time},${e.mag}\n")
                }
            }
        }
    }

// Lets user pick a file to import the results of a previously exported list
private fun importCSV():ObservableList<Properties> {
    if(!File("data").isDirectory){
    Files.createDirectory(Paths.get("data"))}

    val fileName =  chooseFile("Choose Folder",
                    arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")),
                    File("data"),FileChooserMode.Single)

    if(fileName.size == 1){
        val prop = mutableListOf<Properties>()
        val lines = fileName[0].useLines { it.toList() }

        lines.forEach {
            val fields = it.split(",")
            if(fields.size != 5)throw Exception("given file is invalid")
            val type = fields[0]
            val place = "${fields[1]},${fields[2]}"
            val time = (fields[3]).toLong()
            val mag = if(fields[4] == "null"){
                null
            }
            else{fields[4].toDouble()}

            prop.add(Properties(mag,place,time,type))
        }
        return prop.toObservable()
    }
    else{return observableListOf()}
}

    //sets filename which is shown in the file-chooser regarding the selection in the main view
    private fun getFileName(): String {
        return if (fromDate.value == toDate.value) {
            "Earthquakes_${fromDate.value}_${regionFilter.value}.csv"
        } else {
            "Earthquakes_${fromDate.value}-${toDate.value}_${regionFilter.value}.csv"
        }
    }