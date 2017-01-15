package spixie;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Comparator;

public class Graph extends BorderPane implements ValueChanger {
    private Canvas canvas=new Canvas();
    private GraphicsContext g;
    private double unitWidthInPixels = 50.0;
    private double startX = 0;
    private double startDragX = 0;
    private double maxOutput = 1.0;

    private Double mouseX = null;
    private Double mouseY = null;
    private Point mousePointGrub = null;
    private boolean mousePointGrubCreatedNow = false;
    private Point mousePressPoint = null;


    private ArrayList<Point> points=new ArrayList<Point>();



    private HBox control = new HBox();
    private ComboBox<Value.Item> inputComboBox;
    private ComboBox<Value.Item> outValueComboBox;
    private Value maxOutputValue = new Value(1.0,1.0, "Max Output");
    public Graph(Node parrent, Value outValue) {
        super();
        setHeight(200);
        canvas.setHeight(200);
        points.add(new Point(0,0.5));
        canvas.parentProperty().addListener(new ChangeListener<Parent>() {
            public void changed(ObservableValue<? extends Parent> observableValue, Parent parent, Parent t1) {
                t1.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
                    public void changed(ObservableValue<? extends Bounds> observableValue, Bounds bounds, Bounds t1) {
                        canvas.setWidth(t1.getWidth());
                        paint();
                    }
                });
            }
        });
        canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                mousePressPoint = new Point(mouseEvent.getX(), mouseEvent.getY());
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    startDragX = startX + (long)mouseEvent.getX();
                }
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    double mouseValueX = (mouseEvent.getX()+startX)/unitWidthInPixels;
                    double mouseValueY = mouseEvent.getY()/canvas.getHeight()*maxOutput;
                    Point nearestPoint = getNearestPoint(mouseValueX, mouseValueY);

                    if(nearestPoint!=null){
                        mousePointGrub = nearestPoint;
                        mousePointGrubCreatedNow=false;
                    }


                    if(nearestPoint == null){
                        double value = getValue(mouseValueX);
                        if(Math.abs(value - mouseValueY) < 0.02*maxOutput){
                            Point newPoint = new Point(mouseValueX, value);
                            points.add(newPoint);
                            mousePointGrub = newPoint;
                            mousePointGrubCreatedNow=true;
                            points.sort(new Comparator<Point>() {
                                public int compare(Point p1, Point p2) {
                                    return Double.compare(p1.getX(), p2.getX());
                                }
                            });
                        }
                    }
                    paint();
                }
            }
        });
        canvas.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mousePointGrub != null){
                    if(points.size()>1){
                        if(!mousePointGrubCreatedNow) {
                            if (Math.hypot(mouseEvent.getX() - mousePressPoint.getX(), mouseEvent.getY() - mousePressPoint.getY()) < 5) {
                                points.remove(mousePointGrub);
                            }
                        }
                    }
                    mousePointGrub=null;
                }
                paint();
            }
        });
        canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    startX = startDragX - (long)mouseEvent.getX();
                    if(startX<0){
                        startX=0;
                    }
                    paint();
                }
                if(mouseEvent.getButton() == MouseButton.PRIMARY){
                    double mouseValueX = (mouseEvent.getX()+startX)/unitWidthInPixels;
                    double mouseValueY = mouseEvent.getY()/canvas.getHeight()* maxOutput;
                    if(mousePointGrub != null){
                        if(mouseValueX<0){
                            mousePointGrub.setX(0);
                        }else{
                            mousePointGrub.setX(mouseValueX);
                        }

                        if(mouseValueY < 0){
                            mousePointGrub.setY(0);
                        }else if(mouseValueY > maxOutput){
                            mousePointGrub.setY(maxOutput);
                        }else{
                            mousePointGrub.setY(mouseValueY);
                        }
                        points.sort(new Comparator<Point>() {
                            public int compare(Point p1, Point p2) {
                                return Double.compare(p1.getX(), p2.getX());
                            }
                        });
                    }
                    paint();
                }
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
                paint();
            }
        });
        canvas.setOnMouseMoved(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
                paint();
            }
        });
        canvas.setOnMouseExited(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                mouseX = null;
                mouseY = null;
                paint();
            }
        });
        canvas.setOnScroll(new EventHandler<ScrollEvent>() {
            public void handle(ScrollEvent scrollEvent) {
                if(scrollEvent.getDeltaY() > 0){
                    if(unitWidthInPixels < 0x10000L){
                        unitWidthInPixels *= 2;
                        startX*=2;
                    }
                }
                if(scrollEvent.getDeltaY() < 0){
                    if(unitWidthInPixels > 0.001){
                        unitWidthInPixels /= 2;
                        startX/=2;
                    }
                }
                paint();
            }
        });
        g = canvas.getGraphicsContext2D();
        setCenter(canvas);

        setTop(control);
        inputComboBox = new ComboBox<>();
        inputComboBox.getItems().addAll(Main.world.time.item());
        inputComboBox.getItems().addAll(((Element) parrent).getValues());
        inputComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Value.Item>() {
            @Override
            public void changed(ObservableValue<? extends Value.Item> observableValue, Value.Item item, Value.Item t1) {
                if(item!=null){
                    item.unsubscribeChanger(Graph.this);
                }
                t1.subscribeChanger(Graph.this);
            }
        });
        outValueComboBox = new ComboBox<>();
        outValueComboBox.getItems().addAll(((Element) parrent).getValues());
        outValueComboBox.getSelectionModel().select(outValue.item());

        maxOutputValue.item().subscribeChanger(new ValueChanger() {
            @Override
            public void updateOutValue() {
                if(maxOutputValue.get() < 1){
                    maxOutputValue.set(1.0);
                }
                maxOutput = maxOutputValue.get();
                paint();
            }
        });

        control.getChildren().addAll(new Label("Input:"), inputComboBox, new Label("Output:"), outValueComboBox, maxOutputValue);
    }

    public void updateOutValue(){
        if(inputComboBox.getSelectionModel().getSelectedItem() != null){
            if(outValueComboBox.getSelectionModel().getSelectedItem() != null){
                outValueComboBox.getSelectionModel().getSelectedItem().value.set(getValue(inputComboBox.getSelectionModel().getSelectedItem().value.get()));
            }
        }
    }

    private void paint(){
        g.clearRect(0,0,canvas.getWidth(), canvas.getHeight());
        g.setLineWidth(1.0);
        paintRule();
        paintLines();
        updateOutValue();
    }

    private double beatStep = 1.0;

    private void paintRule(){
        double beat = 0;
        double step = unitWidthInPixels;
        beatStep = 1.0;
        while(step<50){
            step*=2;
            beatStep*=2;
        }
        while (step>=100){
            step/=2;
            beatStep/=2;
        }
        double x = - startX;
        int quarter = 0;
        while (x<-step){
            x+=step;
            quarter++;
            beat+=beatStep;
        }
        for (; x < canvas.getWidth(); x+= step, quarter++) {
            if(quarter%4 ==0){
                g.setStroke(new Color(0,0,0,1.0));
            }else{
                g.setStroke(new Color(0,0,0,0.4));
            }
            g.strokeLine(x, 0, x, canvas.getHeight());
            if(quarter%4 == 0){
                g.setStroke(new Color(0,0,0,0.3));
                g.setFont(new Font(9));
                g.strokeText(String.valueOf(beat),x+2,9,step);
            }
            beat+=beatStep;
        }
    }

    private Point getNearestPoint(double x, double y){
        double nearestDist = Double.MAX_VALUE;
        Point nearestPoint = null;
        for (Point point : points) {
            double dist = Math.hypot((point.getX() - x)*unitWidthInPixels/100,(point.getY()-y)/maxOutput);
            if(nearestPoint == null){
                nearestPoint = point;
                nearestDist = dist;
            }else{
                if(dist< nearestDist){
                    nearestPoint = point;
                    nearestDist = dist;
                }
            }
        }
        if(nearestDist<0.04){
            return nearestPoint;
        }else{
            return null;
        }
    }

    private void paintLines(){
        Point nearestPoint;
        g.setLineWidth(1.0);
        if(mouseX != null && mouseY != null){
            double mouseValueX = (mouseX+startX)/unitWidthInPixels;
            double mouseValueY = mouseY/canvas.getHeight()* maxOutput;
            nearestPoint = getNearestPoint(mouseValueX, mouseValueY);
            if(nearestPoint==null){
                if(Math.abs(getValue(mouseValueX) - mouseValueY) < 0.02*maxOutput){
                    g.setLineWidth(2.2);
                }
            }
        }else{
            nearestPoint = null;
        }

        g.setStroke(new Color(1.0,0.0,0.0,1.0));
        for (int x = 0; x < canvas.getWidth(); x++) {
            double y1 = getValue((startX+x)/unitWidthInPixels);
            double y2 = getValue((startX+x+1)/unitWidthInPixels);
            g.strokeLine(x,y1*(canvas.getHeight()/ maxOutput),x+1,y2*(canvas.getHeight()/ maxOutput));
        }
        for (Point point : points) {
            if(point==nearestPoint){
                g.fillOval(point.getX()*unitWidthInPixels -4 - startX, point.getY()*(canvas.getHeight()/ maxOutput)-4,8, 8);
            }else{
                g.fillOval(point.getX()*unitWidthInPixels -3 - startX, point.getY()*(canvas.getHeight()/ maxOutput)-3,6, 6);
            }

        }
    }

    private double getValue(double param){

        Point p1 = null;
        Point p2 = null;
        if(points.size()==1){
            return points.get(0).getY();
        }else{
            if(param < points.get(0).getX()){
                return points.get(0).getY();
            }
            if(param > points.get(points.size()-1).getX()){
                return points.get(points.size()-1).getY();
            }
            for (Point point : points) {
                if(p1==null){
                    p1 = point;
                }else{
                    if(p2!=null){
                        p1 = p2;
                    }
                    p2 = point;
                    if(p2.getX()>=param){
                        double distBetweenPoints = p2.getX()-p1.getX();
                        double distP1ToParam = param-p1.getX();
                        return p1.getY() + (p2.getY() - p1.getY())*(distP1ToParam / distBetweenPoints);
                    }
                }
            }
            return p2.getY();
        }
    }
}
