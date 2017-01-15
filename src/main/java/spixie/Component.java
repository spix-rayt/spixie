package spixie;

public class Component {
    private String name;
    public ComponentBody componentBody = new ComponentBody();

    public Component(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
