package spixie;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class World {
    final public Component root = new Component("Root");
    final public Value frame = new Value(0.0,1.0,"Frame");
    final public Value time = new Value(0.0,1.0, "Root Time");
    public volatile boolean allowRender = true;
    public volatile boolean renderingToFile = false;
    private volatile Thread currentRenderThread=null;

    public World() {
        RootTimeChanger rootTimeChanger = new RootTimeChanger(frame,new Value(140,1.0,"BPM"), time);
        frame.item().subscribeChanger(rootTimeChanger);
    }

    private GLProfile glProfile;
    private GLCapabilities glCapabilities;
    private GLDrawableFactory factory;
    private GLOffscreenAutoDrawable drawable;

    public void initGLDrawable(){
        glProfile = GLProfile.getDefault();
        glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setOnscreen(false);
        glCapabilities.setPBuffer(true);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(1);
        factory = GLDrawableFactory.getFactory(glProfile);
        drawable = factory.createOffscreenAutoDrawable(null, glCapabilities, null, 1, 1);
        drawable.addGLEventListener(new OffscreenGL());
    }

    public void resizeIfNotCorrect(int width, int height){
        if(drawable.getSurfaceWidth() != width || drawable.getSurfaceHeight() != height){
            drawable.setSurfaceSize(width,height);
        }
    }

    public void renderStart(final ImageView imageView){
        allowRender = true;
//        BroadcastRender.broadcastRender.start();
        currentRenderThread = new Thread(new Runnable() {
            public void run() {
                initGLDrawable();
                while (true){
                    try {
                        if(allowRender && !renderingToFile){
                            allowRender = false;
                            resizeIfNotCorrect((int)imageView.getFitWidth(), (int)imageView.getFitHeight());
                            Image image = SwingFXUtils.toFXImage(openglRender(drawable),null);
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    imageView.setImage(image);
                                    /*try {
                                        BroadcastRender.renderedImageByteArray = layer.toJpg();
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }*/
                                    allowRender = true;
                                }
                            });
                        }
                        Thread.sleep(20);
                    }catch (ConcurrentModificationException e){
                        allowRender = true;
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

    private BufferedImage openglRender(GLOffscreenAutoDrawable drawable){
        drawable.display();
        return new AWTGLReadBufferUtil(drawable.getGLProfile(), true).readPixelsToBufferedImage(drawable.getGL(), true);
    }

    public void renderToFile(FrameRenderedToFileEvent frameRenderedToFileEventHandler, RenderToFileCompleted renderToFileCompleted){
        renderingToFile = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                GLProfile glp = GLProfile.getDefault();
                GLCapabilities glc = new GLCapabilities(glp);
                glc.setOnscreen(false);
                glc.setPBuffer(true);
                glc.setSampleBuffers(true);
                glc.setNumSamples(16);
                GLDrawableFactory fc = GLDrawableFactory.getFactory(glp);
                GLOffscreenAutoDrawable offscreenAutoDrawable = fc.createOffscreenAutoDrawable(null, glc, null, 1920, 1024);
                offscreenAutoDrawable.addGLEventListener(new OffscreenGL());

                IMediaWriter iMediaWriter = ToolFactory.makeWriter("out.mp4");
                iMediaWriter.addVideoStream(0,0, ICodec.ID.CODEC_ID_MPEG4, 1920, 1024);

                int countFrames = (int)frame.get() + 1;
                for (int frame = 0; frame < countFrames; frame++) {
                    int finalFrame = frame;
                    final CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            World.this.frame.set(finalFrame);
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BufferedImage bufferedImage = openglRender(offscreenAutoDrawable);
                    BufferedImage bufferedImage1 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    bufferedImage1.getGraphics().drawImage(bufferedImage,0,0,null);
                    iMediaWriter.encodeVideo(0, bufferedImage1, Math.round(frame*(1000.0/60.0)), TimeUnit.MILLISECONDS);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            frameRenderedToFileEventHandler.handle(finalFrame+1 ,countFrames);
                        }
                    });
                }

                iMediaWriter.close();
                renderingToFile = false;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        renderToFileCompleted.handle();
                    }
                });
            }
        }).start();
    }

    public interface FrameRenderedToFileEvent{
        public void handle(int currentFrame, int framesCount);
    }

    public interface RenderToFileCompleted{
        public void handle();
    }
}
