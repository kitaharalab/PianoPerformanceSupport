package jp.kthrlab.pianoroll.processing;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

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
    }

    @Override
    public void drawTimeline() {
    }

    @Override
    public void drawKeyboard() {
    }

    @Override
    public void drawNote() {
    }

    @Override
    public void drawPlayhead() {
    }
}
