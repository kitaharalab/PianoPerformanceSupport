import processing.core.*;

public class Timeline {
    float keyHeight;
    public Timeline(float keyHeight) {
        this.keyHeight = keyHeight;
    }
    public void draw(PApplet app, float width, float height) {
        int numKeys = HorizontalKeyboard.NUM_KEYS;
        float keyWidth = width / numKeys;
        app.stroke(180);
        for (int i = 0; i <= numKeys; i++) {
            float x = i * keyWidth;
            app.line(x, 0, x, height - keyHeight);
        }
    }
}