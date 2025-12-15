package jp.kthrlab.pianoroll.processing;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import jp.kthrlab.pianoroll.CurrentInputState;
import jp.kthrlab.pianoroll.HorizontalKeyboard;
import jp.kthrlab.pianoroll.Keyboard;
import jp.kthrlab.pianoroll.PianoRoll;
import jp.kthrlab.pianoroll.Timeline;
import processing.core.PApplet;

public class PAppletPianoRoll extends PApplet implements PianoRoll {
    protected Keyboard keyboard;
    protected Timeline timeline;

    @Override
    public void settings() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        Frame frame = new Frame();
        frame.setUndecorated(false);
        frame.setSize(0, 0);
        frame.setVisible(true);

        size((int) usableBounds.width, (int) (usableBounds.height - frame.getInsets().top));

        frame.dispose();
    }

    @Override
    public void setup() {
        background(255);
        windowMove(0, 0);
    }

    @Override
    public void draw() {
        background(255);

        drawTimeline();
        drawNote();
        drawPlayhead();
        drawKeyboard();
        drawCurrentNote();
    }

    @Override
    public void drawTimeline() {
    }

    @Override
    public void drawKeyboard() {
    }

    public void drawCurrentNote() {
        if (!(keyboard instanceof HorizontalKeyboard))
            return;

        int currentNote = CurrentInputState.getCurrentNote();
        if (currentNote < 0)
            return;

        HorizontalKeyboard hk = (HorizontalKeyboard) keyboard;

        float x;

        if (Keyboard.isWhiteKey(currentNote)) {
            int whiteIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (Keyboard.isWhiteKey(nn))
                    whiteIndex++;
            }
            x = hk.whiteKeyX.get(whiteIndex);
        } else {
            int blackIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (!Keyboard.isWhiteKey(nn))
                    blackIndex++;
            }
            x = hk.blackKeyX.get(blackIndex);
        }

        float w = Keyboard.isWhiteKey(currentNote)
                ? hk.getWhiteKeyWidth()
                : hk.getBlackKeyWidth();

        float h = Keyboard.isWhiteKey(currentNote)
                ? hk.getWhiteKeyHeight()
                : hk.getBlackKeyHeight();

        float y = hk.getKeyboardY();
        float yOffset = 22;

        pushStyle();
        fill(255, 0, 0);
        noStroke();
        ellipse(
                x + w / 2,
                y + h / 2 + yOffset,
                w * 0.6f,
                w * 0.6f);
        popStyle();
    }

    @Override
    public void drawNote() {
    }

    @Override
    public void drawPlayhead() {
    }
}
