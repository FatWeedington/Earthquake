import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

import tornadofx.*

import java.io.File
import java.lang.Exception
import java.net.URL
import java.time.LocalDate

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
    private var fromDate = LocalDate.now().toProperty()
    private var toDate = LocalDate.now().toProperty()
    private var eq = mutableListOf<Properties>().asObservable()

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
                menu("Tools"){
                    item("Show Graph")
                    action {
                        find<NewWindow>().openWindow(owner = null)
                    }
                }
            }
        }

        center() {
            tableview(eq) {
                readonlyColumn("Title", Properties::title)
                readonlyColumn("Type", Properties::type)
                readonlyColumn("Place", Properties::place)
                readonlyColumn("Time", Properties::timeLD)
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
            for (e in eq) {
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
            eq.asyncItems { eQs() }.fail { alert(Alert.AlertType.ERROR, "Data could not be received", content = "${it.message}").showAndWait() }
    }
}
class NewWindow : View("Graph") {
    override val root = hbox {
        setPrefSize(800.0, 600.0)
    }
}
