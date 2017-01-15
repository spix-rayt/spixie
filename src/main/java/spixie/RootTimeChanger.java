package spixie;

public class RootTimeChanger implements ValueChanger {
    private Value frame;
    private Value bpm;
    private Value time;

    public RootTimeChanger(Value frame, Value bpm, Value time) {
        this.frame = frame;
        this.bpm = bpm;
        this.time = time;
    }

    @Override
    public void updateOutValue() {
        time.set(bpm.getFraction().divide(3600).multiply(frame.getFraction()));
    }
}
