import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.DateCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Stage
import tornadofx.*
import tornadofx.Stylesheet.Companion.empty
import java.io.BufferedWriter
import java.io.File
import java.time.LocalDate
import javax.naming.Binding
import javax.swing.GroupLayout.Alignment

var fromDate = LocalDate.now().toProperty()
var toDate = LocalDate.now().toProperty()
private fun eQs() = getEarthQuakes().features.map { i -> i.properties }.asObservable()

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

    override val root = borderpane {
        top() {
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
                                        item.isAfter(LocalDate.now()) || item.isBefore(LocalDate.now().minusYears(10))
                                }
                            }
                        }
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
                                        isDisable = item.isAfter(LocalDate.now()) || item.isBefore(fromDP.value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            center {
                //Build Fields of Tableview
                val tblView = tableview(eQs()) {
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

                          for(e in eQs()){
                              writer.append("${e.title},${e.type},${e.place},${e.timeLD},${e.mag}\n")
                          }
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




