package pianoroll;
import processing.core.PApplet;

public class HorizontalPAppletPianoRoll extends PAppletPianoRoll {
    @Override
    public void setup() {
        keyboard = new HorizontalKeyboard(width, height);
        timeline = new VerticalTimeline((keyboard.getWhiteKeyWidth() * 7f) / 12f);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { HorizontalPAppletPianoRoll.class.getName() });
    }

    @Override
    public void drawTimeline() {
        int numKeys = keyboard.getNumKeys();
        stroke(180);
        // for (int i = 0; i <= numKeys; i++) {
        for (int i = 0; i <= numKeys; i++) {
            float x = i * timeline.getKeyWidth() + keyboard.getOffset();
            line(x, 0, x, height - keyboard.getWhiteKeyHeight());
        }
    }

    @Override
    public void drawKeyboard() {
        // Draw white keys
        float[] whiteKeyX = new float[keyboard.getWhiteKeys()];
        int nWhite = 0;
        
                
        for (int nn = keyboard.getFirstNn(); nn <= keyboard.getLastNn(); nn++) {
            if (Keyboard.isWhiteKey(nn)) {
                float x = nWhite * keyboard.getWhiteKeyWidth();
                whiteKeyX[nWhite] = x;
                drawWhiteKey(x);
                nWhite++;
            }
            // int noteInOctave = nn % 12;
            // if (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
            //     noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11) {
            //     float x = nWhite * keyboard.getWhiteKeyWidth();
            //     whiteKeyX[nWhite] = x;
            //     drawWhiteKey(x);
            //     nWhite++;
            // } 
        }

        // Draw black keys
        int index = 0;

        for (int nn = keyboard.getFirstNn(); nn <= keyboard.getLastNn(); nn++) {
            int noteInOctave = nn % 12;
            if (noteInOctave == 1 || noteInOctave == 3 || noteInOctave == 6 ||
                noteInOctave == 8 || noteInOctave == 10) {
                if (nWhite == 0) {
                    drawBlackKey(keyboard.getOffset(), keyboard.getBlackKeyWidth()/2);
                // } else if (noteInOctave == 1 || noteInOctave == 6) {
                //     drawBlackKey((index) * keyboard.getBlackKeyWidth() + offset, keyboard.getBlackKeyWidth());

                // } else if (noteInOctave == 10) {
                //     drawBlackKey((index) * keyboard.getBlackKeyWidth() + offset, keyboard.getBlackKeyWidth());
                } else {
                    // float x = index * keyboard.getBlackKeyWidth();
                    drawBlackKey(index * keyboard.getBlackKeyWidth() + keyboard.getOffset(), keyboard.getBlackKeyWidth());
                }
            } //else {
            //     nWhite++;
            // }
            index++;
        }

    }

    private void drawWhiteKey(float x) {
                fill(255);
                stroke(0);
                rect(x, keyboard.getKeyboardY(), keyboard.getWhiteKeyWidth(), keyboard.getWhiteKeyHeight());
    }

    private void drawBlackKey(float x, float keyWidth) {
                fill(0);
                stroke(0);
                rect(x, keyboard.getKeyboardY(), keyWidth, keyboard.getBlackKeyHeight());
    }
}
