import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Alert
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.StringConverter
import tornadofx.*
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer

private var fromDate = LocalDate.now().toProperty()
private var toDate = LocalDate.now().toProperty()
private var earthQuakes = mutableListOf<Properties>().asObservable()

fun main(args: Array<String>) {
    launch<GUI>(args)
}

class GUI : App(MainView::class) {
    override fun start(stage: Stage) {
        stage.width = 800.0
        stage.height = 600.0
        super.start(stage)
    }
}

class MainView : View("Earthquakes") {
    init {
        fixedRateTimer("Timer", true, 0L, 5000) {
            updateEarthQuakes()
            }
        }

    override val root = borderpane {
        top {
            menubar {
                menu("Files") {
                    item("Export CSV") {
                        action {
                            try {
                                exportCSV()
                            } catch (e: Exception) {
                                alert(Alert.AlertType.WARNING, "File could not be saved", content = "${e.message}")
                            }
                        }
                    }
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
                menu("Tools") {
                    item("Show Graph")
                    action {
                        if (getDailyEvents().size < 2){
                            alert(Alert.AlertType.WARNING, "Not enough Days", content = "Please specify a time-range from at least two Days")
                        }
                        else {
                            find<ChartWindow>().openWindow()
                        }
                    }
                }
            }
        }

        center() {
            tableview(earthQuakes) {
                readonlyColumn("Type", Properties::type)
                readonlyColumn("Location", Properties::location)
                readonlyColumn("Region", Properties::region)
                readonlyColumn("Date", Properties::dateText)
                readonlyColumn("Time", Properties::timeText)
                readonlyColumn("Magnitude", Properties::mag)

                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }
        bottom {
            hbox(spacing = 20) {
                minHeight = 35.0
                hbox(spacing = 8) {
                    hboxConstraints {
                        alignment = Pos.CENTER_LEFT
                    }
                    label { text = "From:" }
                    datepicker(fromDate) {
                        valueProperty().bindBidirectional(fromDate)
                        setDayCellFactory {
                            object : DateCell() {
                                override fun updateItem(item: LocalDate, empty: Boolean) {
                                    super.updateItem(item, empty)
                                    isDisable =
                                        item.isAfter(LocalDate.now()) || item.isBefore(
                                            LocalDate.now().minusYears(10)
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
                }
            }
        }
    }



    private fun updateEarthQuakes() = runAsync {
                                                    earthQuakes.asyncItems{getEarthQuakes(fromDate.value, toDate.value).features.map { i -> i.properties }.toObservable()}
                                                } fail {
                                                    alert(Alert.AlertType.ERROR,"Error",it.message).showAndWait()
                                                }

    private fun exportCSV() {
        val fileName = chooseFile("Choose Folder", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")),File("data${File.separator}"),FileChooserMode.Save){
            initialFileName = getFileName()
        }
        if(fileName.size == 1){
            val writer = fileName[0].bufferedWriter()
            writer.use {
                for (e in earthQuakes) {
                    writer.append("${e.type},${e.location},${e.region},${e.timeLD},${e.mag}\n")
                }
            }
        }
    }

    private fun getFileName(): String {
        return if (fromDate.value == toDate.value) {
            "Earthquakes_${fromDate.value}.csv"
        } else {
            "Earthquakes_${fromDate.value}-${toDate.value}.csv"
        }
    }
}

private fun importCSV():ObservableList<Properties> {
    val fileName = chooseFile("Choose Folder", arrayOf(FileChooser.ExtensionFilter("CSV Files", "*.csv")),File("data${File.separator}"),FileChooserMode.Single)
    if(fileName.size == 1){
        val prop = mutableListOf<Properties>()
        val lines = fileName[0].useLines { it.toList() }

        lines.forEach {
            val fields = it.split(",")
            val type = fields[0]
            val place = "${fields[1]},${fields[2]}"
            val time = LocalDateTime.parse(fields[3]).toInstant(ZoneId.systemDefault().rules.getOffset(LocalDateTime.now())).toEpochMilli()
            val mag = if(fields[4] == "null"){
                null
            }
            else{fields[4].toDouble()}

            prop.add(Properties(mag,place,time,type))
        }
        return prop.toObservable()
    }
    else{return observableListOf<Properties>()}
}


class CsvWindow : Fragment("Graph") {
    override val root =
        tableview(importCSV()) {
            readonlyColumn("Type", Properties::type)
            readonlyColumn("Location", Properties::location)
            readonlyColumn("Region", Properties::region)
            readonlyColumn("Date", Properties::dateText)
            readonlyColumn("Time", Properties::timeText)
            readonlyColumn("Magnitude", Properties::mag)

            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vboxConstraints {
                vGrow = Priority.ALWAYS
            }
            setPrefSize(600.0, 400.0)
        }
}

class ChartWindow : Fragment("Imported Data") {
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
            series("Daily Events") {
                getDailyEvents().forEach {
                    data(it.key, it.value)
                }
            }

        }
}

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

    private fun getMinDate() = earthQuakes.minOfOrNull { it.timeLD }?.toLocalDate()?.toEpochDay()?.toDouble()!!

    private fun getMaxDate() = earthQuakes.maxOfOrNull { it.timeLD }?.toLocalDate()?.toEpochDay()?.toDouble()!!

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

