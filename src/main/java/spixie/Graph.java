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
import javafx.util.Pair;

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
    private Point gravityPoint = null;


    private ArrayList<Point> points=new ArrayList<Point>();



    private HBox control = new HBox();
    private ComboBox<Value.Item> inputComboBox;
    private ComboBox<Value.Item> outValueComboBox;
    private Value maxOutputValue = new Value(1.0,1.0, "Max Output");
    public Graph(Node parrent, Value outValue) {
        super();
        setHeight(200);
        canvas.setHeight(200);
        points.add(new Point(0,0));
        sortPoints();
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
                double mouseValueX = (mouseEvent.getX()+startX)/unitWidthInPixels;
                double mouseValueY = mouseEvent.getY()/canvas.getHeight()*maxOutput;
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    startDragX = startX + (long)mouseEvent.getX();
                }
                if(mouseEvent.isControlDown()){
                    if(mouseEvent.isControlDown()) {
                        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                            for (Point point : points) {
                                if (point.next != null && point.next.getX() > mouseValueX) {
                                    gravityPoint = point;
                                    break;
                                }
                            }
                        }
                    }
                }else{
                    if(mouseEvent.getButton() == MouseButton.PRIMARY){
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
                                sortPoints();
                                if(newPoint.prev != null && newPoint.next != null){
                                    Pair<Point,Double> gravityPointByX = findGravityPointByX(newPoint.prev, newPoint.next, mouseValueX);
                                    double oldGravityX = newPoint.prev.getX() + (newPoint.next.getX() - newPoint.prev.getX()) * newPoint.prev.gravity_x;
                                    double oldGravityY = newPoint.prev.getY() + (newPoint.next.getY() - newPoint.prev.getY()) * newPoint.prev.gravity_y;
                                    Point gravityPoint = findGravityPoint(newPoint.prev, new Point(oldGravityX, oldGravityY), newPoint.next,newPoint.prev, newPoint ,gravityPointByX.getValue(), false);
                                    newPoint.prev.gravity_x = (gravityPoint.getX() - newPoint.prev.getX())/(newPoint.getX() - newPoint.prev.getX());
                                    newPoint.prev.gravity_y = (gravityPoint.getY() - newPoint.prev.getY())/(newPoint.getY() - newPoint.prev.getY());


                                    gravityPoint = findGravityPoint(newPoint.prev, new Point(oldGravityX, oldGravityY), newPoint.next, newPoint, newPoint.next, gravityPointByX.getValue(), true);
                                    newPoint.gravity_x = (gravityPoint.getX() - newPoint.getX())/(newPoint.next.getX() - newPoint.getX());
                                    newPoint.gravity_y = (gravityPoint.getY() - newPoint.getY())/(newPoint.next.getY() - newPoint.getY());
                                }
                            }
                        }
                        paint();
                    }
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
                                sortPoints();
                            }
                        }
                    }
                    mousePointGrub=null;
                }
                if(gravityPoint != null){
                    gravityPoint = null;
                }
                paint();
            }
        });
        canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                double mouseValueX = (mouseEvent.getX()+startX)/unitWidthInPixels;
                double mouseValueY = mouseEvent.getY()/canvas.getHeight()* maxOutput;
                if(mouseEvent.getButton() == MouseButton.SECONDARY){
                    startX = startDragX - (long)mouseEvent.getX();
                    if(startX<0){
                        startX=0;
                    }
                    paint();
                }
                if(gravityPoint!=null) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        double mouseLimitedValueX = mouseValueX;
                        double minx = Math.min(gravityPoint.getX(), gravityPoint.next.getX());
                        double maxx = Math.max(gravityPoint.getX(), gravityPoint.next.getX());
                        if (mouseLimitedValueX < minx) mouseLimitedValueX = minx;
                        if (mouseLimitedValueX > maxx) mouseLimitedValueX = maxx;
                        double mouseLimitedValueY = mouseValueY;
                        double miny = Math.min(gravityPoint.getY(), gravityPoint.next.getY());
                        double maxy = Math.max(gravityPoint.getY(), gravityPoint.next.getY());
                        if (mouseLimitedValueY < miny) mouseLimitedValueY = miny;
                        if (mouseLimitedValueY > maxy) mouseLimitedValueY = maxy;
                        gravityPoint.gravity_x = (mouseLimitedValueX - gravityPoint.getX()) / (gravityPoint.next.getX() - gravityPoint.getX());
                        gravityPoint.gravity_y = (mouseLimitedValueY - gravityPoint.getY()) / (gravityPoint.next.getY() - gravityPoint.getY());
                    }
                }else{
                    if(mouseEvent.getButton() == MouseButton.PRIMARY){
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
                            sortPoints();
                        }
                        paint();
                    }
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

    private void sortPoints(){
        points.sort(new Comparator<Point>() {
            public int compare(Point p1, Point p2) {
                return Double.compare(p1.getX(), p2.getX());
            }
        });
        Point last = null;
        for (Point point : points) {
            point.prev = last;
            if(last!=null){
                last.next = point;
            }
            last = point;
        }
        last.next=null;
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
        Double mouseValueX = null;
        Double mouseValueY = null;
        if(mouseX != null && mouseY != null){
            mouseValueX = (mouseX+startX)/unitWidthInPixels;
            mouseValueY = mouseY/canvas.getHeight()* maxOutput;
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
        boolean gravityPointPainted = gravityPoint != null;
        for (Point point : points) {
            g.setFill(Color.BLACK);
            if(point==nearestPoint){
                g.fillOval(point.getX()*unitWidthInPixels -4 - startX, point.getY()*(canvas.getHeight()/ maxOutput)-4,8, 8);
            }else{
                g.fillOval(point.getX()*unitWidthInPixels -3 - startX, point.getY()*(canvas.getHeight()/ maxOutput)-3,6, 6);
            }
            if(mouseValueX!=null && !gravityPointPainted){
                if(point.next!=null && point.next.getX() >= mouseValueX){
                    g.setFill(Color.MEDIUMPURPLE.brighter());
                    double gravityX = point.getX() + (point.next.getX() - point.getX()) * point.gravity_x;
                    double gravityY = point.getY() + (point.next.getY() - point.getY()) * point.gravity_y;
                    g.fillOval(gravityX*unitWidthInPixels -3 - startX, gravityY*(canvas.getHeight()/ maxOutput)-3,6, 6);
                    gravityPointPainted = true;
                }
            }
        }
        if(gravityPoint != null){
            g.setFill(Color.MEDIUMPURPLE.brighter());
            double gravityX = gravityPoint.getX() + (gravityPoint.next.getX() - gravityPoint.getX()) * gravityPoint.gravity_x;
            double gravityY = gravityPoint.getY() + (gravityPoint.next.getY() - gravityPoint.getY()) * gravityPoint.gravity_y;
            g.fillOval(gravityX*unitWidthInPixels -3 - startX, gravityY*(canvas.getHeight()/ maxOutput)-3,6, 6);
        }
    }

    private double getValue(double param){
        Point p1 = null;
        Point p3 = null;
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
                    if(p3!=null){
                        p1 = p3;
                    }
                    p3 = point;
                    if(p3.getX()>=param){
                        Pair<Point, Double> gravityPointByX = findGravityPointByX(p1, p3, param);
                        return gravityPointByX.getKey().getY();
                    }
                }
            }
            return p3.getY();
        }
    }

    private Pair<Point,Double> findGravityPointByX(Point p1, Point p3, double x){
        double min_t = 0.0;
        double max_t = 1.0;
        while(true){
            double t = (min_t + max_t)/2;
            double megaT = Math.pow(t, 1);
            double p2_x = p1.getX() + (p3.getX() - p1.getX()) * p1.gravity_x;
            double p2_y = p1.getY() + (p3.getY() - p1.getY()) * p1.gravity_y;
            double p1p2x = p1.getX()+(p2_x - p1.getX())*megaT;
            double p1p2y = p1.getY()+(p2_y - p1.getY())*megaT;
            double p2p3x = p2_x+(p3.getX() - p2_x)*megaT;
            double p2p3y = p2_y+(p3.getY() - p2_y)*megaT;
            double bx = p1p2x + (p2p3x - p1p2x)*t;
            double by = p1p2y + (p2p3y - p1p2y)*t;
            if(Math.abs(bx-x) < 0.00001){
                return new Pair<Point,Double>(new Point(bx,by), t);
            }
            if(x > bx){
                min_t = (max_t + min_t) / 2;
            }
            if(x < bx){
                max_t = (max_t + min_t) / 2;
            }
        }
    }

    private Point calcCoords(double x0, double y0, double x1, double y1, double x2, double y2, double t){
        double l0x = x0 + (x1 - x0) * t;
        double l1x = x1 + (x2 - x1) * t;
        double x = l0x + (l1x - l0x) * t;

        double l0y = y0 + (y1 - y0) * t;
        double l1y = y1 + (y2 - y1) * t;
        double y = l0y + (l1y - l0y) * t;
        return new Point(x,y);
    }

    private Point findGravityPoint(Point p0, Point p1, Point p2, Point p3,Point p5,double k, boolean secondPart){
        double dist = Double.MAX_VALUE;
        Point result = null;
        double xstart,xend,ystart,yend;
        if(p5.getX() < p3.getX()){
            xstart = p5.getX();
            xend = p3.getX();
        }else{
            xstart = p3.getX();
            xend = p5.getX();
        }
        if(p5.getY()<p3.getY()){
            ystart = p5.getY();
            yend = p3.getY();
        }else{
            ystart = p3.getY();
            yend = p5.getY();
        }
        double xstep = (xend - xstart)/1000;
        double ystep = (yend - ystart)/1000;
        for(double x=xstart;x<xend;x+=xstep){
            for(double y=ystart;y<yend;y+=ystep){
                Point c0;
                if(secondPart){
                    c0 = calcCoords(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(),p2.getY(),(k + 1.0)/2);
                }else{
                    c0 = calcCoords(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(),p2.getY(),0.5*k);
                }

                Point c1 = calcCoords(p3.getX(),p3.getY(),x,y,p5.getX(),p5.getY(), 0.5);
                double d = (c1.getX() - c0.getX())*(c1.getX() - c0.getX()) + (c1.getY() - c0.getY())*(c1.getY() - c0.getY());
                if(d<dist){
                    dist = d;
                    result = new Point(x,y);
                }
            }
        }
        return result;
    }
}
