package pianoroll;


public class HorizontalKeyboard extends Keyboard {

    public HorizontalKeyboard(float width, float height) {
        super(width, height, 88, 21);
    }

    // public void draw(PApplet app) {
    //     // 白鍵
    //     float[] whiteKeyX = new float[getWhiteKeys()];
    //     int nWhite = 0;
    //     for (int midi = getFirstNn(); midi <= getLastNn(); midi++) {
    //         int noteInOctave = midi % 12;
    //         if (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
    //             noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11) {
    //             float x = nWhite * getWhiteKeyWidth();
    //             whiteKeyX[nWhite] = x;
    //             app.fill(255);
    //             app.stroke(0);
    //             app.rect(x, app.height - getWhiteKeyHeight(), getWhiteKeyWidth(), getWhiteKeyHeight());
    //             nWhite++;
    //         } else {
    //             drawBlackKey(app, whiteKeyX[nWhite - 1] + getWhiteKeyWidth());
    //         }
    //     }
    //     // 黒鍵
    //     nWhite = 0;
    //     for (int midi = getFirstNn(); midi <= getLastNn(); midi++) {
    //         int noteInOctave = midi % 12;
    //         boolean isWhite = (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
    //                            noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11);
    //         if (isWhite) {
    //             nWhite++;
    //         } else {
    //             if (nWhite == 0 || nWhite >= getWhiteKeys()) continue;
    //             float x = whiteKeyX[nWhite - 1] + getWhiteKeyWidth(); //* 0.75f;
    //             app.fill(0);
    //             app.stroke(0);
    //             app.rect(x, app.height - getWhiteKeyHeight(), getWhiteKeyWidth() / 2, getBlackKeyHeight());
    //         }
    //     }
    // }

}