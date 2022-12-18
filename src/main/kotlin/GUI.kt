import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers

import tornadofx.*

import java.io.File
import java.time.LocalDate

fun main(args: Array<String>) {
        launch<GUI>(args)
    }

class GUI : App(MainView::class){
    override fun start(stage: Stage) {
        stage.width = 800.0
        stage.height = 600.0
        super.start(stage)
    }
}

class MainView : View("MyFirstTornadoApp") {

    private var fromDate = LocalDate.now().toProperty()
    private var toDate = LocalDate.now().toProperty()

     private fun eQs() = getEarthQuakes(fromDate.value,toDate.value).features.map { i -> i.properties }.asObservable()

     private var eq = eQs()
     private fun updateEqs(){
        eq.asyncItems { eQs() }
    }

    override val root = borderpane {
        top {
            hbox(spacing = 20) {
                hboxConstraints {
                    minHeight = 35.0
                }
                hbox(spacing = 8) {
                    hboxConstraints {
                        alignment = Pos.CENTER_LEFT
                    }
                    label { text = "From:" }
                    val fromDP = datepicker(fromDate) {
                        valueProperty().bindBidirectional(fromDate)
                        setDayCellFactory {
                            object : DateCell() {
                                override fun updateItem(item: LocalDate, empty: Boolean) {
                                    super.updateItem(item, empty)
                                    isDisable =
                                        item.isAfter(LocalDate.now()) || item.isBefore(LocalDate.now().minusYears(10)) || item.isAfter(toDate.value)
                                }
                            }
                        }
                        setOnAction{ runAsync{
                            updateEqs()
                        }fail{
                            println("error")
                        } }
                    }
                    hbox(spacing = 8) {
                        hboxConstraints {
                            alignment = Pos.CENTER_LEFT
                        }
                        label { text = "To:" }
                        val toDP = datepicker(toDate) {
                            valueProperty().bindBidirectional(toDate)
                            setDayCellFactory {
                                object : DateCell() {
                                    override fun updateItem(item: LocalDate, empty: Boolean) {
                                        super.updateItem(item, empty)
                                        isDisable = item.isAfter(LocalDate.now()) || item.isBefore(fromDP.value) || item.isBefore(fromDate.value)
                                    }
                                }
                            }
                            setOnAction{ runAsync{
                                updateEqs()
                            }fail{
                                println("error")
                            } }
                        }
                    }
                }
            }
        }
            center {
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
                    alignment = Pos.CENTER_LEFT
                    hboxConstraints {
                        minHeight = 35.0
                    }
                    button("Export CSV") {
                        action {
                          val fileName = getFileName()
                            val writer = File(fileName).bufferedWriter()

                          for(e in eq){
                              writer.append("${e.title},${e.type},${e.place},${e.timeLD},${e.mag}\n")
                          }
                          writer.flush()
                          writer.close()
                    }
                }
            }
        }
    }

    private fun getFileName(): String {
        return if(fromDate.value == toDate.value){
            "data${File.separator}Earthquakes_${fromDate.value}.csv"
        } else {
            "data${File.separator}Earthquakes_${fromDate.value}-${toDate.value}.csv"
        }
    }
    }




