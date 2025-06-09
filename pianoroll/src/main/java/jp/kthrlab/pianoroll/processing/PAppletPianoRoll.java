package jp.kthrlab.pianoroll.processing;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import jp.kthrlab.pianoroll.Keyboard;
import jp.kthrlab.pianoroll.PDFToImage;
import jp.kthrlab.pianoroll.PianoRoll;
import jp.kthrlab.pianoroll.Timeline;
import processing.core.PApplet;
import processing.core.PImage;


public class PAppletPianoRoll extends PApplet implements PianoRoll {
    // Variables
    protected Keyboard keyboard;
    protected Timeline timeline;
    boolean playheadFixed = false; // 再生位置線の固定フラグ

    PImage pdfImage;

    @Override
    public void settings() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        AffineTransform at = gd.getDefaultConfiguration().getDefaultTransform();
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

        // PDF画像の読み込み
        try {
            BufferedImage bufferedImage = PDFToImage.loadFirstPage("kirakira2-midi.pdf");
            pdfImage = new PImage(bufferedImage.getWidth(), bufferedImage.getHeight(), ARGB);
            bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
                    pdfImage.pixels, 0, bufferedImage.getWidth());
            pdfImage.updatePixels();
            System.out.println("PDF画像読み込み成功: " + pdfImage.width + "x" + pdfImage.height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Draw method
    @Override
    public void draw() {
        // Drawing logic here
        background(255); // 画面クリア
        
        if (pdfImage != null) {
            image(pdfImage, 0, 0, width, height);
        } else {
            fill(0);
            text("PDF画像がありません", 10, 20);
        }

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
