package spixie;

import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.commons.math3.fraction.Fraction;

import java.util.ArrayList;

public class Value extends Label {
    private Fraction value = new Fraction(0);
    private Fraction startDragValue = new Fraction(0);
    private double mul = 1.0;
    private String name;
    private ArrayList<ValueChanger> subscribers = new ArrayList<>();
    public Value(double initial,double mul, String name) {
        super();
        this.value = new Fraction(initial);
        this.mul=mul;
        this.name = name;
        setText(name + ": " + String.valueOf(value.doubleValue()));

        setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    startDragValue = value.add(new Fraction(Value.this.mul).multiply((int)mouseEvent.getY()));
                }
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    Parent parent = getParent();
                    if(parent instanceof Multiplier){
                        ((Multiplier) parent).addGraph(Value.this);
                    }
                }
            }
        });

        setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    set(startDragValue.subtract(new Fraction(Value.this.mul).multiply((int)mouseEvent.getY())));
                }
            }
        });
    }

    public void set(double value){
        set(new Fraction(value));
    }

    public void set(Fraction value){
        if(value.compareTo(Fraction.ZERO) < 0){
            this.value = new Fraction(0);
        }else{
            this.value = value;
        }
        setText(Value.this.name + ": " + String.valueOf(this.value.doubleValue()));
        for (ValueChanger valueChanger : subscribers) {
            valueChanger.updateOutValue();
        }
    }

    public double get(){
        return value.doubleValue();
    }

    public Fraction getFraction(){
        return value;
    }

    public Item item(){
        return new Item(this);
    }

    public static class Item {
        public Value value;

        public Item(Value value) {
            this.value = value;
        }

        public void subscribeChanger(ValueChanger valueChanger){
            value.subscribers.add(valueChanger);
        }

        public void unsubscribeChanger(ValueChanger valueChanger){
            value.subscribers.remove(valueChanger);
        }

        @Override
        public String toString() {
            return value.name;
        }
    }
}
