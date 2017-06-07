package spixie;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ParticlesBuilder {
    public static final int PARTICLE_FLOAT_SIZE = 3;

    private ArrayList<Float> floats = new ArrayList();

    public ParticlesBuilder() {

    }

    public void addParticle(float x, float y, float size){
        floats.add(x);
        floats.add(y);
        floats.add(size);
    }

    public int particlesCount(){
        return floats.size()/PARTICLE_FLOAT_SIZE;
    }

    public FloatBuffer toFloatBuffer(){
        FloatBuffer buffer = FloatBuffer.allocate(floats.size());
        for (Float aFloat : floats) {
            buffer.put(aFloat);
        }
        buffer.rewind();
        return buffer;
    }
}
