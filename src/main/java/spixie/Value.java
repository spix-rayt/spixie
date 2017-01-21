package spixie;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.apache.commons.math3.fraction.Fraction;

import java.util.ArrayList;

public class Value extends HBox {
    private Fraction value = new Fraction(0);
    private Fraction startDragValue = new Fraction(0);
    private double mul = 1.0;
    private String name;
    private ArrayList<ValueChanger> subscribers = new ArrayList<>();
    private final Label labelName = new Label();
    private final Label labelValue = new Label();
    private final TextField textFieldValue = new TextField();
    private boolean dragged = false;
    public Value(double initial,double mul, String name) {
        super();
        this.mul=mul;
        this.name = name;
        labelName.setText(name+": ");
        set(initial);
        labelValue.getStyleClass().add("label-value");

        labelValue.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    startDragValue = value.add(new Fraction(Value.this.mul).multiply((int)mouseEvent.getY()));
                    dragged = false;
                }
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    Parent parent = getParent();
                    if(parent instanceof Multiplier){
                        ((Multiplier) parent).addGraph(Value.this);
                    }
                }
            }
        });

        labelValue.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    set(startDragValue.subtract(new Fraction(Value.this.mul).multiply((int)mouseEvent.getY())));
                    dragged = true;
                }
            }
        });

        labelValue.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    if(!dragged){
                        textFieldValue.setText(String.valueOf(Value.this.value.doubleValue()));
                        getChildren().remove(labelValue);
                        getChildren().addAll(textFieldValue);
                        textFieldValue.requestFocus();
                        textFieldValue.selectAll();
                    }
                }
            }
        });

        textFieldValue.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                if(!t1.booleanValue()){
                    try {
                        set(Double.parseDouble(textFieldValue.getText()));
                    }catch (NumberFormatException e){}
                    getChildren().remove(textFieldValue);
                    getChildren().addAll(labelValue);
                }
            }
        });

        textFieldValue.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if(keyEvent.getCode() == KeyCode.ENTER){
                    getChildren().remove(textFieldValue);
                }
            }
        });



        getChildren().addAll(labelName, labelValue);
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
        labelValue.setText(String.valueOf(this.value.doubleValue()));
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

    final private Item itemForCheckbox = new Item(this);
    public Item item(){
        return itemForCheckbox;
    }

    public static class Item {
        final public Value value;

        public Item(Value value) {
            this.value = value;
        }

        public void subscribeChanger(ValueChanger valueChanger){
            if(!value.subscribers.contains(valueChanger)){
                value.subscribers.add(valueChanger);
            }
        }

        public void unsubscribeChanger(ValueChanger valueChanger){
            value.subscribers.remove(valueChanger);
        }

        public boolean checkCycle(ValueChanger valueChanger){
            return valueChanger.getValueToBeChanged().checkCycleInternal(new Item[] {this}, null, null);
        }

        public boolean checkCycle(ValueChanger fakeValueChanger, Item fakeItem){
            return checkCycleInternal(new Item[] {}, fakeValueChanger, fakeItem);
        }

        private boolean checkCycleInternal(Item[] values, ValueChanger fakeValueChanger, Item fakeItem){
            for (Item item : values) {
                if(item == this){
                    return false;
                }
            }
            Item[] newValues = new Item[values.length + 1];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[i];
            }
            newValues[newValues.length-1] = this;
            for (ValueChanger subscriber : value.subscribers) {
                Item valueToBeChanged;
                if(subscriber == fakeValueChanger){
                    valueToBeChanged = fakeItem;
                }else {
                    valueToBeChanged = subscriber.getValueToBeChanged();
                }
                if(valueToBeChanged!=null){
                    if(!valueToBeChanged.checkCycleInternal(newValues, fakeValueChanger, fakeItem)){
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return value.name;
        }
    }
}
