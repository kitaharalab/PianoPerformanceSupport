package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.processing.CMXController;
import jp.kthrlab.pianoroll.Channel;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import jp.kthrlab.pianorollsample.MidiReceiver;
import jp.kthrlab.pianoroll.PDFToImage;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.core.PImage;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import processing.awt.PSurfaceAWT;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Transmitter;
import javax.swing.*;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyApp extends HorizontalPAppletCmxPianoRoll {

    private Transmitter midiTransmitter;

    CMXController cmx = CMXController.getInstance();

    PImage pdfImage;
    // PImage[] pdfImages;
    PImage[] pdfImage;
    int currentImageIndex = 0;
    long lastSwitchTime = 0;
    int switchIntervalMillis = 2000; // 2秒

    IMusicData musicData;  

    @Override
    public void draw() {
        super.draw();
        // PDFToImageを呼び出してpdfを表示する

        if (pdfImage != null) {
            // 切り取る範囲（例: 左上からウィンドウ幅、高さ/2）
            int cropH = Math.min(height * 2 / 5, pdfImage.height);
            // image(img, dx, dy, dwidth, dheight, sx, sy, swidth, sheight)
            image(pdfImage, 0, 0, width, cropH, 0, 0, pdfImage.width, cropH);
        if (pdfImage != null && pdfImage.length > 0) {
            long now = millis();
            if (now - lastSwitchTime > switchIntervalMillis) {
                currentImageIndex = (currentImageIndex + 1) % pdfImage.length;
                lastSwitchTime = now;
            }

            // 選択された画像のみを描画
            float imgHeight = height / 2.0f;
            image(pdfImage[currentImageIndex], 0, 0, width, imgHeight);
        } else {
            fill(0);
            text("PDF画像がありません", 10, 20);
        }

        // if (pdfImages != null && pdfImages.length > 0) {
        // float imgHeight = (height / 2.0f) / pdfImages.length;
        // for (int i = 0; i < pdfImages.length; i++) {
        // image(pdfImages[i], 0, i * imgHeight, width, imgHeight);
        // }
        // } else {
        // fill(0);
        // text("PDF画像がありません", 10, 20);
        // }
    }

    @Override
    public void setup() {
        super.setup();

        initMidi();

        musicData = new MusicData(
                // "kirakira2.mid",
                "TchaikovskyPletnevMarch.mid",
                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12);

        cmx.smfread(musicData.getScc());
        //startMusic();

        List<Color> colors = new ArrayList<>(Arrays.asList(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN));
        List<Channel> channels = new ArrayList<Channel>();
        try {
            Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEachOrdered(part -> {
                Channel channel = new Channel(
                        part.channel(),
                        part.prognum(),
                        "",
                        colors.get(part.channel()));
                channels.add(channel);
            });
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        dataModel = new PianoRollDataModelMultiChannel(
                2,
                2 + 12,
                4,
                channels,
                musicData.getScc());

        // PDF画像の読み込み
        try {
            BufferedImage bufferedImage = PDFToImage.loadFirstPage("TchaikovskyPletnevMarch-midi.pdf");
            pdfImage = new PImage(bufferedImage.getWidth(), bufferedImage.getHeight(), ARGB);
            bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
                    pdfImage.pixels, 0, bufferedImage.getWidth());
            pdfImage.updatePixels();
            System.out.println("PDF画像読み込み成功: " + pdfImage.width + "x" + pdfImage.height);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // PDF画像の読み込み（配列対応）
        // try {
        // BufferedImage[] bufferedImages =
        // PDFToImage.loadFirstPageSplitHorizontally("kirakira2-midi.pdf");
        // pdfImages = new PImage[bufferedImages.length];
        // for (int i = 0; i < bufferedImages.length; i++) {
        // BufferedImage img = bufferedImages[i];
        // pdfImages[i] = new PImage(img.getWidth(), img.getHeight(), ARGB);
        // img.getRGB(0, 0, img.getWidth(), img.getHeight(), pdfImages[i].pixels, 0,
        // img.getWidth());
        // pdfImages[i].updatePixels();
        // }
        // System.out.println("PDF画像配列読み込み成功: " + pdfImages.length + "ページ");
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    private void initMidi() {
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0) {
                    device.open();
                    midiTransmitter = device.getTransmitter();
                    midiTransmitter.setReceiver(new MidiReceiver(this));
                    println("接続されたMIDIデバイス: " + info.getName());
                    break;
                }
            BufferedImage[] bufferedImages = PDFToImage.loadFirstPageSplitHorizontally("kirakira2-midi.pdf");
            pdfImage = new PImage[bufferedImages.length];
            for (int i = 0; i < bufferedImages.length; i++) {
                BufferedImage img = bufferedImages[i];
                pdfImage[i] = new PImage(img.getWidth(), img.getHeight(), ARGB);
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pdfImage[i].pixels, 0,
                        img.getWidth());
            }
            System.out.println("PDF画像配列読み込み成功: " + pdfImage.length + "ページ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MIDIノート入力時に呼ばれる
    public void onNoteInput(int pitch, int velocity) {
        println("ノート入力: pitch=" + pitch + ", velocity=" + velocity);

        if (isCorrectNote(pitch)) {
            println("正しいノートです（続行）");
            // 続行処理（必要に応じて）
        } else {
            println("間違ったノートです（停止）");
            stopMusic();  // 演奏停止
        }
    }

    public void onNoteOff(int pitch) {
        // 必要があれば処理を追加
    }

    private boolean isCorrectNote(int pitch) {
        // 仮実装：60(C4) のみ正解とする
        return pitch == 60;
    }

    private void hideDefaultTitleBar() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();
        frame.removeNotify();
        frame.setUndecorated(true); // デフォルトのタイトルバーを隠す
        frame.addNotify();
    }

    void startMusic() {
        if (!cmx.isNowPlaying()) {
            cmx.playMusic();
        }
    }

    void stopMusic() {
        cmx.stopMusic();
    }

    void createMenu() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();

        JMenuBar menuBar = new JMenuBar();

        Button btnStart = new Button("Start");
        btnStart.addActionListener(e -> {
            startMusic();
        });
        menuBar.add(btnStart);

        Button btnStop = new Button("Stop");
        btnStop.addActionListener(e -> {
            stopMusic();
        });
        menuBar.add(btnStop);

        frame.setJMenuBar(menuBar);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { MyApp.class.getName() });
    }

    // @Override
    // public void keyPressed() {
    // super.keyPressed();
    // switch (keyCode) {
    // case ENTER -> startMusic();
    // case BACKSPACE -> stopMusic();
    // }
    // }

    @Override
    public void keyPressed() {
        //super.keyPressed();
        switch (keyCode) {
            case 'a' -> onNoteInput(60, 100);  // 'a'キーでC4のノート入力（正解）
            case 's' -> onNoteInput(62, 100);  // 's'キーでD4のノート入力（不正解で停止）
        }
    }
}
