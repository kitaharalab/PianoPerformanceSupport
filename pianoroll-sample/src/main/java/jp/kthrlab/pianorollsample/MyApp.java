package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.processing.CMXController;
import jp.crestmuse.cmx.amusaj.sp.MidiInputModule; //<>//
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.kthrlab.pianoroll.Channel;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
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

    PImage[] pdfImage;
    int currentImageIndex = 0;
    long lastSwitchTime = 0;
    int switchIntervalMillis = 2000; // 2秒
    boolean pdfSwitching = false;

    IMusicData musicData;

    @Override
    public void draw() {
        super.draw();
        //System.out.println("draw loop running");

        // PDFToImageを呼び出してpdfを表示する

        if (pdfImage != null && pdfImage.length > 0) {
            long now = millis();
            if (pdfSwitching && now - lastSwitchTime > switchIntervalMillis) {
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

        //// 停止リクエストが出ていたら、処理停止
        //if (ReceiverModule.isStopRequested()) {
        //    stopMusic();
        //    pdfSwitching = false;
        //    ReceiverModule.requestStop(false);
        //    println("ReceiverModuleによって停止されました");
        //}
    }

    @Override
    public void setup() {
        super.setup();

        cmx.showMidiInChooser(this);
        cmx.showMidiOutChooser(this);

        setupModules();

        musicData = new MusicData(
                // "kirakira2.mid",
                "kirakira2.mid",
                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12);

        cmx.smfread(musicData.getScc());
        // startMusic();

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

        // PDF画像の読み込み（配列対応）
        try {
            BufferedImage[] bufferedImages = PDFToImage.loadFirstPageSplitHorizontally("kirakira2-midi.pdf");
            pdfImage = new PImage[bufferedImages.length];
            for (int i = 0; i < bufferedImages.length; i++) {
                BufferedImage img = bufferedImages[i];
                pdfImage[i] = new PImage(img.getWidth(), img.getHeight(), ARGB);
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pdfImage[i].pixels, 0,
                        img.getWidth());
                pdfImage[i].updatePixels();
            }
            System.out.println("PDF画像配列読み込み成功: " + pdfImage.length + "ページ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupModules() {
        //System.out.println("setupModules() called");
        try {
            //System.out.println("module setup start");
            MidiInputModule mi = cmx.createMidiIn(); // 入力モジュール
            MidiOutputModule mo = cmx.createMidiOut(); // 出力モジュール

            //ReceiverModule receiver = new ReceiverModule(musicData.getScc().toDataSet());

            cmx.addSPModule(mi);
            //cmx.addSPModule(receiver);
            cmx.addSPModule(mo);

            // モジュール接続
            cmx.connect(mi, 0, mo, 0);
            //cmx.connect(mi, 0, receiver, 0); // MIDI入力を受け取る
            //cmx.connect(receiver, 0, mo, 0); // OUT にも流す場合

            cmx.startSP();

            println("モジュール構成完了");

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public void keyPressed() {
        super.keyPressed();
        switch (keyCode) {
            case ENTER -> {
                startMusic();
                pdfSwitching = true;
            }
            case BACKSPACE -> {
                stopMusic();
                pdfSwitching = false;
            }
        }
    }

    // @Override
    // public void keyPressed() {
    // //super.keyPressed();
    // switch (keyCode) {
    // case 'a' -> onNoteInput(60, 100); // 'a'キーでC4のノート入力（正解）
    // case 's' -> onNoteInput(62, 100); // 's'キーでD4のノート入力（不正解で停止）
    // }
    // }
}
