package jp.kthrlab.pianorollsample;

import java.awt.Button;
import java.awt.Color; //<>//
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import javax.sound.midi.Transmitter;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.xml.transform.TransformerException;

import jp.crestmuse.cmx.amusaj.sp.MidiInputModule;
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.crestmuse.cmx.processing.CMXController;
import jp.kthrlab.pianoroll.Channel;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import processing.core.PApplet;
import processing.core.PImage;

public class MyApp extends ImageNotePianoRoll {

    private Transmitter midiTransmitter;

    CMXController cmx = CMXController.getInstance();

    PImage[] pdfImage;
    PImage[] pdfImage2;
    int currentImageIndex = 0;
    long lastSwitchTime = 0;
    int switchIntervalMillis = 2000; // 2秒

    IMusicData musicData;
    PerformanceData performanceData;
    long lastTickPosition = 0;

    boolean flash = false;
    long flashStartTime = 0;

    @Override
    public void setup() {
        super.setup();

        MidiSave.main(null);

        cmx.showMidiInChooser(this);
        cmx.showMidiOutChooser(this);

        musicData = new MusicData(
                "kirakira2.mid",
                // "025500b_.mid",
                // "ICantGetStarted.mid",
                // "TchaikovskyPletnevMarch.mid",
                // "MerryChristmasMr.Lawrence.mid",
                // "Elise.mid",
                // "Debussy_Arabesque_1.mid",
                // "NOC21.mid",
                // "LIEBESTD91.mid",
                // "ETU31.mid",

                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12);

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

                // mute
                part.addControlChange(0, 7, 0); // チャンネルのコントロールチェンジを追加

                // test imageNotes
                // for (int i = 0; i < part.getNoteOnlyList().length; i++) {
                // addImageNote(part.getNoteOnlyList()[i]);
                // }

                //for (int i = 0; i < 4; i++) {
                //    addImageNote(part.getNoteOnlyList()[i]);
                //}

                // addImageNote(part.getNoteOnlyList()[1]);
                // addImageNote(part.getNoteOnlyList()[5]);
                // addImageNote(part.getNoteOnlyList()[7]);

            });
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        cmx.smfread(musicData.getScc());

        // performanceData
        try {
            performanceData = new PerformanceData(musicData.getScc().toDataSet());
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        setupModules();

        dataModel = new PianoRollDataModelMultiChannel(
                2,
                2 + 12,
                4,
                channels,
                musicData.getScc());

        // PDF画像の読み込み（配列対応）
        try {
            BufferedImage[] bufferedImages = PDFToImage.loadFirstPageSplitHorizontally("kirakira2_first4-midi.pdf");
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

        try {
            BufferedImage[] bufferedImages2 = PDFToImage.loadFirstPageSplitHorizontally("kirakira2_first2-midi.pdf");
            pdfImage2 = new PImage[bufferedImages2.length];
            for (int i = 0; i < bufferedImages2.length; i++) {
                BufferedImage img2 = bufferedImages2[i];
                pdfImage2[i] = new PImage(img2.getWidth(), img2.getHeight(), ARGB);
                img2.getRGB(0, 0, img2.getWidth(), img2.getHeight(), pdfImage2[i].pixels, 0,
                        img2.getWidth());
                pdfImage2[i].updatePixels();
            }
            System.out.println("PDF画像配列読み込み成功: " + pdfImage2.length + "ページ");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void draw() {
        //blendMode(1);
        //// PDFToImageを呼び出してpdfを表示する
        //if (pdfImage != null && pdfImage.length > 0) {
        //    long now = millis();
        //    if (now - lastSwitchTime > switchIntervalMillis) {
        //        currentImageIndex = (currentImageIndex + 1) % pdfImage.length;
        //        lastSwitchTime = now;
        //    }
//
        //    PImage img = pdfImage[1]; // kirakira3.pdfは2
//
        //    float maxW = width;
        //    float maxH = height / 2.0f;
//
        //    float imgW = img.width;
        //    float imgH = img.height;
//
        //    float scale = min(maxW / imgW, maxH / imgH);
//
        //    float drawW = imgW * scale;
        //    float drawH = imgH * scale;
//
        //    float x = 0; // ← 左側に表示
        //    float y = 0;
//
        //    float scrollSpeed = 2.0f;
        //    float scrollY = ((millis() / 10) * scrollSpeed) % (drawH + maxH);
//
        //    long microLen = cmx.getMicrosecondLength();
        //    long microPos = cmx.getMicrosecondPosition();
        //    float frac = (microLen > 0) ? (float) microPos / (float) microLen : 0f;
        //    scrollY = frac * (drawH + maxH);
        //    scrollY = constrain(scrollY, 0, drawH + maxH);
//
        //    float mainY = y - drawH + scrollY + 150f;
//
        //    float hideThresholdY = 150;
//
        //    if (mainY < hideThresholdY) {
        //        float visibleH = min(drawH, hideThresholdY - mainY);
        //        if (visibleH > 0) {
        //            int srcH = max(1, (int) (img.height * (visibleH / drawH)));
        //            int srcW = max(1, img.width / 3);
//
        //            image(img, x, mainY + 625, drawW / 3, visibleH,
        //                    0, 0, srcW, srcH);
        //        }
        //    }
        //}
//
        //// === 2つめのPDF（pdfImage2） ===
        //if (pdfImage2 != null && pdfImage2.length > 0) {
        //    long now = millis();
        //    if (now - lastSwitchTime > switchIntervalMillis) {
        //        currentImageIndex = (currentImageIndex + 1) % pdfImage2.length;
        //        lastSwitchTime = now;
        //    }
//
        //    PImage img2 = pdfImage2[4];
//
        //    float maxW = width;
        //    float maxH = height / 2.0f;
//
        //    float imgW = img2.width;
        //    float imgH = img2.height;
//
        //    float scale = min(maxW / imgW, maxH / imgH);
//
        //    float drawW = imgW * scale;
        //    float drawH = imgH * scale;
//
        //    float x = 500;
        //    float y = 0;
//
        //    float scrollSpeed = 2.0f;
        //    float scrollY = ((millis() / 10) * scrollSpeed) % (drawH + maxH);
//
        //    long microLen = cmx.getMicrosecondLength();
        //    long microPos = cmx.getMicrosecondPosition();
        //    float frac = (microLen > 0) ? (float) microPos / (float) microLen : 0f;
        //    scrollY = frac * (drawH + maxH);
        //    scrollY = constrain(scrollY, 0, drawH + maxH);
//
        //    float mainY = y - drawH + scrollY + 150f;
//
        //    float hideThresholdY = 150;
//
        //    if (mainY < hideThresholdY) {
        //        float visibleH = min(drawH, hideThresholdY - mainY);
        //        if (visibleH > 0) {
        //            int srcH = max(1, (int) (img2.height * (visibleH / drawH)));
        //            int srcW = max(1, img2.width / 3);
        //            image(img2, x, mainY + 625, drawW / 3, visibleH,
        //                    0, 0, srcW, srcH);
        //        }
        //    }
        //}

        super.draw();
        //pianoroll/HorizontalPAppletCmxPianoRoll.javaにある
        //PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        //long tickPosition = getCmx().getTickPosition();
        ////Object tickLock = tickPosition;
        //Long relativeOnset = note.onset() - tickPosition;
        //float h = (float) ((note.offset() - note.onset()) * dataModelMultiChannel.getPixelPerTick());
        //float y = timeline.getSpan() - (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;

        long tickPosition = cmx.getTickPosition();
        // tick に対応するノートがまだ演奏されていないかチェック
        LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
            println(tick + " " + performanceData.hasNotesToPlay(tick));
            if (performanceData.hasNotesToPlay(tick)) {
                stopMusic(); // ノートが残っていれば停止
            } else {
                if (cmx.getMicrosecondPosition() == cmx.getMicrosecondLength()) {
                    performanceData.setNotesToPlay();
                    cmx.setTickPosition(0); // 再生位置を先頭に戻す
                }
                startMusic(); // すべて演奏済みなら再開
            }
        });
        lastTickPosition = tickPosition;

        // === 停止時フラッシュ表示 ===
        if (flash) {
            // 120ミリ秒だけ光る
            if (millis() - flashStartTime < 120) {
                pushStyle();
                fill(255, 255, 0, 120); // 黄色っぽい光（透明）
                noStroke();
                rect(0, 0, width, height); // 全画面を覆う
                popStyle();
            } else {
                flash = false; // フラッシュ終了
            }
        }

    }

    private void setupModules() {
        try {
            MidiInputModule mi = cmx.createMidiIn(); // 入力モジュール
            MidiOutputModule mo = cmx.createMidiOut(); // 出力モジュール

            PianoTeacherModule pianoTeacherModule = new PianoTeacherModule(
                    musicData.getScc().toDataSet(),
                    performanceData);

            // ReceiverModule receiver = new ReceiverModule(this); // NoteInputListener を渡す

            cmx.addSPModule(mi);
            cmx.addSPModule(pianoTeacherModule);
            cmx.addSPModule(mo);

            // モジュール接続
            cmx.connect(mi, 0, pianoTeacherModule, 0);
            cmx.connect(pianoTeacherModule, 0, mo, 0); // OUT にも流す場合

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
        System.out.println("startMusic");
        if (!cmx.isNowPlaying()) {
            cmx.playMusic();
        }
        flash = true;
        flashStartTime = millis();
    }

    void stopMusic() {
        System.out.println("stopMusic");
        if (cmx.isNowPlaying()) {
            cmx.stopMusic();
        }
        // flash = true;
        // flashStartTime = millis();
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
            case ENTER -> startMusic();
            case BACKSPACE -> stopMusic();
        }
    }

}
