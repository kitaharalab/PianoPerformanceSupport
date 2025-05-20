package pianoroll;

import processing.core.PApplet;

public class PAppletPianoRoll extends PApplet implements PianoRoll {
    // Variables
    protected Keyboard keyboard;
    protected Timeline timeline;
    boolean playheadFixed = false; // 再生位置線の固定フラグ
    

    // // Constructor
    // public PAppletPianoRoll() {
    // }

    @Override
    public void settings() {
        // fullScreen();
        size(displayWidth, displayHeight);    
    }

    @Override
    public void setup() {
        background(255);
    }


    // Draw method
    @Override
    public void draw() {
        // Drawing logic here
        drawKeyboard();
        drawTimeline();
        drawNote();
        drawPlayhead();
    }

    @Override
    public void drawTimeline() {
        // Do nothing
    }

    @Override
    public void drawKeyboard() {
    }

    @Override
    public void drawNote() {
        // Note drawing logic here
    }

    @Override
    public void drawPlayhead() {
        // Playhead drawing logic here
    }


}
