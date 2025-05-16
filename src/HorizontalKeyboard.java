import processing.core.*;

public class HorizontalKeyboard {
    static final int NUM_KEYS = 88;
    static final int WHITE_KEYS = 52;
    static final int FIRST_MIDI = 21;
    static final int LAST_MIDI = 108;

    float keyWidth, keyHeight, blackKeyHeight;
    float y; // 鍵盤のY座標

    public HorizontalKeyboard(float width, float height, float keyHeightRatio, float blackKeyHeightRatio) {
        keyWidth = width / WHITE_KEYS;
        keyHeight = height * keyHeightRatio;
        blackKeyHeight = height * blackKeyHeightRatio;
        y = height - keyHeight;
    }

    public void draw(PApplet app) {
        // 白鍵
        float[] whiteKeyX = new float[WHITE_KEYS];
        int nWhite = 0;
        for (int midi = FIRST_MIDI; midi <= LAST_MIDI; midi++) {
            int noteInOctave = midi % 12;
            if (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
                noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11) {
                float x = nWhite * keyWidth;
                whiteKeyX[nWhite] = x;
                app.fill(255);
                app.stroke(0);
                app.rect(x, y, keyWidth, keyHeight);
                nWhite++;
            }
        }
        // 黒鍵
        nWhite = 0;
        for (int midi = FIRST_MIDI; midi <= LAST_MIDI; midi++) {
            int noteInOctave = midi % 12;
            boolean isWhite = (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
                               noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11);
            if (isWhite) {
                nWhite++;
            } else {
                if (nWhite == 0 || nWhite >= WHITE_KEYS) continue;
                float x = whiteKeyX[nWhite - 1] + keyWidth * 0.75f;
                app.fill(0);
                app.stroke(0);
                app.rect(x, y, keyWidth / 2, blackKeyHeight);
            }
        }
    }

    public float getKeyHeight() {
        return keyHeight;
    }
}