import processing.core.*;

public class PianoRoll extends PApplet {
    HorizontalKeyboard keyboard;
    Timeline timeline;
    static final float KEY_HEIGHT_RATIO = 0.12f;
    static final float BLACK_KEY_HEIGHT_RATIO = 0.07f;

    public void settings() {
        fullScreen();
    }

    public void setup() {
        keyboard = new HorizontalKeyboard(width, height, KEY_HEIGHT_RATIO, BLACK_KEY_HEIGHT_RATIO);
        timeline = new Timeline(keyboard.getKeyHeight());
        background(255);
    }

    public void draw() {
        background(255);
        timeline.draw(this, width, height);
        keyboard.draw(this);
        // ここにノート描画処理を追加できます
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { PianoRoll.class.getName() });
    }
}