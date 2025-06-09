package jp.kthrlab.pianoroll.processing;

import jp.kthrlab.pianoroll.HorizontalKeyboard;
import jp.kthrlab.pianoroll.VerticalTimeline;
import processing.core.PApplet;

public class HorizontalPAppletPianoRoll extends PAppletPianoRoll {
    protected HorizontalKeyboard horizontalKeyboard;
    protected boolean isNoteVisible = true;
    protected void setNoteVisible(boolean noteVisible) {
        isNoteVisible = noteVisible;
    }


    @Override
    public void setup() {
        super.setup();
        keyboard = new HorizontalKeyboard(width, height);
        timeline = new VerticalTimeline(height - keyboard.getWhiteKeyHeight(), keyboard.getOctaveWidth() / 12f);
        horizontalKeyboard = (HorizontalKeyboard) this.keyboard;
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { HorizontalPAppletPianoRoll.class.getName() });
    }

    @Override
    public void drawTimeline() {
        super.drawTimeline();
        int numKeys = keyboard.getNumKeys();
        stroke(180);
        // for (int i = 0; i <= numKeys; i++) {
        for (int i = 0; i <= numKeys; i++) {
            float x = i * timeline.getSemitoneWidth() + keyboard.getOffset();
            line(x, 0, x, height - keyboard.getWhiteKeyHeight() - 1);
        }
    }

    @Override
    public void drawKeyboard() {
        super.drawKeyboard();

        horizontalKeyboard.whiteKeyX.forEach(this::drawWhiteKey);
        horizontalKeyboard.blackKeyX.forEach(this::drawBlackKey);
    }

    private void drawWhiteKey(float x) {
                fill(255);
                stroke(0);
                rect(x, horizontalKeyboard.getKeyboardY(), keyboard.getWhiteKeyWidth(), keyboard.getWhiteKeyHeight());
    }

    private void drawBlackKey(float x) {
                fill(0);
                stroke(0);
                rect(x, horizontalKeyboard.getKeyboardY(), keyboard.getBlackKeyWidth(), keyboard.getBlackKeyHeight());
    }
}
