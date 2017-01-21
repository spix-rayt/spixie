package spixie;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import sun.java2d.pipe.BufferedTextPipe;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;

public class Layer {
    private float[][] pixelsR;
    private float[][] pixelsG;
    private float[][] pixelsB;
    private float[][] pixelsA;

    private int w;
    private int h;
    public Layer(int w, int h) {
        this.w=w;
        this.h=h;
        pixelsR = new float[w][h];
        pixelsG = new float[w][h];
        pixelsB = new float[w][h];
        pixelsA = new float[w][h];
    }

    public void add(int x, int y, float r, float g, float b, float a){
        float reverseA = 1 - a;
        float oldA = pixelsA[x][y];
        float newA = a + oldA * reverseA;
        pixelsR[x][y] = (r * a + pixelsR[x][y] * oldA * reverseA) / newA;
        pixelsG[x][y] = (g * a + pixelsG[x][y] * oldA * reverseA) / newA;
        pixelsB[x][y] = (b * a + pixelsB[x][y] * oldA * reverseA) / newA;
        pixelsA[x][y] = newA;
    }

    public WritableImage toImage(){
        WritableImage result = new WritableImage(w,h);
        PixelWriter pixelWriter = result.getPixelWriter();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float a = pixelsA[x][y];
                float red = pixelsR[x][y] * a;
                float green = pixelsG[x][y] * a;
                float blue = pixelsB[x][y] * a;

                if(red>1.0f) red= 1.0f;
                if(red<0.0f) red = 0.0f;

                if(green>1.0f) green= 1.0f;
                if(green<0.0f) green = 0.0f;

                if(blue>1.0f) blue= 1.0f;
                if(blue<0.0f) blue = 0.0f;
                pixelWriter.setColor(x,y,new Color(red, green, blue,1.0f));
            }
        }

        return result;
    }

    public BufferedImage toBufferedImage(){
        BufferedImage result = new BufferedImage(w,h, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = result.getRaster();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float a = pixelsA[x][y];
                float red = pixelsR[x][y] * a;
                float green = pixelsG[x][y] * a;
                float blue = pixelsB[x][y] * a;

                if(red>1.0f) red= 1.0f;
                if(red<0.0f) red = 0.0f;

                if(green>1.0f) green= 1.0f;
                if(green<0.0f) green = 0.0f;

                if(blue>1.0f) blue= 1.0f;
                if(blue<0.0f) blue = 0.0f;

                int i_red = Math.round(red*255);
                int i_green = Math.round(green*255);
                int i_blue = Math.round(blue*255);
                raster.setPixel(x,y,new int[]{i_red,i_green,i_blue});
            }
        }

        return result;
    }

    public byte[] toJpg() throws Exception{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(toBufferedImage(), "jpg", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public void drawCircle(double xPos, double yPos, double size, float r, float g, float b, float a){
        size -= 1.0;
        int xFrom = (int) Math.floor(xPos - size - 1.0f);
        if(xFrom<0) xFrom = 0;
        int xTo = (int) Math.ceil(xPos + size + 1.0f) + 1;
        if(xTo > w) xTo = w;

        int yFrom = (int) Math.floor(yPos - size - 1.0f);
        if(yFrom< 0) yFrom = 0;
        int yTo = (int) Math.ceil(yPos + size + 1.0f) + 1;
        if(yTo > h) yTo = h;
        for (int x = xFrom; x < xTo; x++) {
            for (int y = yFrom; y < yTo; y++) {
                double hypot = Math.hypot(xPos - x, yPos - y);
                if(hypot < size){
                    add(x,y,r,g,b,a);
                }else if(hypot < size + 1.0f){
                    add(x,y,r,g,b,(float)(a * (size - hypot + 1)));
                }
            }
        }
    }
}
