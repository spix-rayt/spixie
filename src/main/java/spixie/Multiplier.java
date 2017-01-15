package spixie;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class Multiplier extends VBox implements Element {
    final public Value radius = new Value(30.0 ,1.0, "Radius");
    final public Value phase = new Value(0 ,0.001, "Phase");
    final public Value size = new Value(15,1.0, "Size");
    final public Value count = new Value(5,0.1,"Count");
    public Multiplier() {
        super();
        getChildren().addAll(new Label("spixie.Multiplier"));
        getChildren().addAll(radius, phase, size, count);
    }

    public Value.Item[] getValues(){
        return new Value.Item[]{radius.item(), phase.item(), size.item(), count.item()};
    }

    @Override
    public void addGraph(Value outValue) {
        Graph graph = new Graph(this, outValue);
        getChildren().addAll(graph);
    }
}
