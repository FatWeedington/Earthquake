import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.stage.Stage
import tornadofx.*


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
    override val root = borderpane{
        center{
            //Build Fields of Tableview
            tableview(eQs()) {
                readonlyColumn("Title",Properties::title)
                readonlyColumn("Type",Properties::type)
                readonlyColumn("Place",Properties::place)
                readonlyColumn("Time",Properties::timeLD)
                readonlyColumn("Magnitude",Properties::mag)

                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }
    }
}

private fun eQs() = getEarthQuakes().features.map { i -> i.properties }.asObservable()

