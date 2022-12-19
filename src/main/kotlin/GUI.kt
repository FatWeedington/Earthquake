import javafx.beans.binding.BooleanExpression
import javafx.geometry.Pos
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Alert
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Stage
import javafx.util.StringConverter
import kotlinx.coroutines.currentCoroutineContext
import tornadofx.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    override val root = borderpane {
        updateEqs()

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
                }
                menu("Tools") {
                    item("Show Graph")
                    action {
                        find<NewWindow>().openWindow()
                    }
                }
            }
        }

        center() {
            tableview(earthQuakes) {
                readonlyColumn("Title", Properties::title)
                readonlyColumn("Type", Properties::type)
                readonlyColumn("Place", Properties::place)
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
                            updateEqs()
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
                                updateEqs()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportCSV() {
        val fileName = getFileName()
        val writer = File(fileName).bufferedWriter()

        writer.use {
            for (e in earthQuakes) {
                writer.append("${e.title},${e.type},${e.place},${e.timeLD},${e.mag}\n")
            }
        }
    }

    private fun getFileName(): String {
        return if (fromDate.value == toDate.value) {
            "data${File.separator}Earthquakes_${fromDate.value}.csv"
        } else {
            "data${File.separator}Earthquakes_${fromDate.value}-${toDate.value}.csv"
        }
    }


    private fun eQs() = getEarthQuakes(fromDate.value, toDate.value).features.map { i -> i.properties }.asObservable()


    private fun updateEqs() {
        earthQuakes.asyncItems { eQs() }.fail {
            alert(
                Alert.AlertType.ERROR,
                "Data could not be received",
                content = "${it.message}"
            ).showAndWait()
        }
    }
}

class NewWindow : Fragment("Graph") {
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

