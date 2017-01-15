package spixie;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

public class World {
    final public Component root = new Component("Root");
    final public Value frame = new Value(0.0,1.0,"Frame");
    final public Value time = new Value(0.0,1.0, "Root Time");
    public volatile boolean allowRender = true;
    private volatile Thread currentRenderThread=null;

    public World() {
        RootTimeChanger rootTimeChanger = new RootTimeChanger(frame,new Value(140,1.0,"BPM"), time);
        frame.item().subscribeChanger(rootTimeChanger);
    }

    public void render(final ImageView imageView){
        allowRender = true;
        currentRenderThread = new Thread(new Runnable() {
            public void run() {
                while (true){
                    try {
                        if(allowRender){
                            allowRender = false;
                            final WritableImage image = render((int)(imageView.getFitWidth()/2), (int)(imageView.getFitHeight()/2)).toImage();
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    imageView.setImage(image);
                                    allowRender = true;
                                }
                            });
                        }
                        Thread.sleep(40);
                    }catch (ConcurrentModificationException e){
                        continue;
                    }catch (InterruptedException e){
                        return;
                    }
                }
            }
        });
        currentRenderThread.start();
    }

    public Thread getCurrentRenderThread() {
        return currentRenderThread;
    }

    private Layer render(int w, int h){
        Layer layer = new Layer(w, h);
        double ratio = ((double)h / (double)w);
        double mulX = w/1000.0*ratio;
        double mulY = h/1000.0;
        double globalSize = h/1000.0;
        for (Node node : root.componentBody.elements.getChildren()) {
            if(node instanceof Multiplier){
                double radius = ((Multiplier) node).radius.get();
                double phase = ((Multiplier) node).phase.get();
                double size = ((Multiplier) node).size.get();
                double count = ((Multiplier) node).count.get();
                for (int i = 0; i < count; i++) {
                    layer.drawCircle(
                            (500/ratio + Math.cos(Math.PI*2/count*i + phase*Math.PI*2)*radius)*mulX,
                            (500 + Math.sin(Math.PI*2/count*i + phase*Math.PI*2)*radius)*mulY,
                            size*globalSize,
                            1.0f, 0.0f, 1.0f, 0.7f
                    );
                }
            }
        }
        return layer;
    }

    public void renderToFile(){
        IMediaWriter iMediaWriter = ToolFactory.makeWriter("out.mp4");
        iMediaWriter.addVideoStream(0,0, ICodec.ID.CODEC_ID_MPEG4, 1920, 1024);

        int lastFrame = (int)frame.get() + 1;
        for (int frame = 0; frame < lastFrame; frame++) {
            this.frame.set(frame);
            iMediaWriter.encodeVideo(0, render(1920, 1024).toBufferedImage(), Math.round(frame*(1000.0/60.0)), TimeUnit.MILLISECONDS);
            System.out.println(frame + " / " + lastFrame);
        }

        iMediaWriter.close();
    }
}
