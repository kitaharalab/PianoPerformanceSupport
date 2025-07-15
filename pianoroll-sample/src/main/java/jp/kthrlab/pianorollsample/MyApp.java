package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.processing.CMXController;
import jp.crestmuse.cmx.amusaj.sp.MidiInputModule; //<>//
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
import jp.crestmuse.cmx.amusaj.sp.SPModule;
import jp.crestmuse.cmx.amusaj.sp.TimeSeriesCompatible;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.processing.CMXController;
import jp.kthrlab.pianoroll.Channel;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import jp.kthrlab.pianorollsample.ImageNotePianoRoll;
//import main.java.jp.kthrlab.pianorollsample.PDFToImage;
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
import java.util.HashSet;
import java.util.Set;


public class MyApp extends ImageNotePianoRoll {

    private Transmitter midiTransmitter;

    CMXController cmx = CMXController.getInstance();

    PImage[] pdfImage;
    int currentImageIndex = 0;
    long lastSwitchTime = 0;
    int switchIntervalMillis = 2000; // 2秒
    boolean pdfSwitching = false;
    long prevTickPosition = 0;
    Long waitingOnset = null;
    Set<Long> waitingOnsets = new HashSet<>();

    IMusicData musicData;

    @Override
    public void draw() {
        super.draw();
        //println("cmx.isNowPlaying() = " + cmx.isNowPlaying());

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

        //tickPositionを取得して、ReceiverModuleのexecute()内で正誤判定を行う
        long tickPosition = cmx.getTickPosition();
        ReceiverModule.setTickPosition(tickPosition);

        //startMusic();

        try {
            //println("[draw] tryブロック開始");

            List<MutableNote> noteList = Arrays.asList(musicData.getScc().toDataSet().getPart(0).getNoteOnlyList());
            
            //println("[draw] noteList size: " + noteList.size());
            //println("[draw] prevTickPosition=" + prevTickPosition + ", tickPosition=" + tickPosition);

            // 1. 前回のtickPositionよりも大きく、現在のtickPosition以下のonsetを抽出
            for (MutableNote note : noteList) {
                //全てのノートのonsetが0になってしまい、正しいonsetが取得できない問題が発生している
                long onset = note.onset();
                int noteNum = note.notenum();
                //println("[draw] notenum:" + noteNum + ", onset:" + onset);

                if (prevTickPosition < onset && onset <= tickPosition) {
                    //ここのif文が機能しない問題が発生している
                    println("new note: onset = " + onset);
                    waitingOnsets.add(onset);
                    if (cmx.isNowPlaying()) {
                        println("stopMusic()1");
                        stopMusic();
                        pdfSwitching = false;
                        println("stop: onset = " + onset);
                    }
                }
            }
        
            // 2. 正しいノートが弾かれたら、該当onsetをwaitingから削除
            // ここでReceiverModuleから「最後に正しく弾かれたonset」を取得
            long lastCorrectOnset = ReceiverModule.getLastCorrectOnset();
            if (waitingOnsets.contains(lastCorrectOnset)) {
                waitingOnsets.remove(lastCorrectOnset);
                println("remove waiting: onset = " + lastCorrectOnset);
            }

            //ReceiverModule.isStopRequested()はいらないのでは？再生されているかどうかはcmx.isNowPlaying()で確認する
            //次に弾くべき音があるのに再生が停止されない
        
            // 3. 正しいノートが弾かれていれば再開、そうでなければ停止維持
            if (!waitingOnsets.isEmpty()) {
                if (ReceiverModule.isStopRequested()) {
                    if (cmx.isNowPlaying()) {
                        println("stopMusic()2");
                        stopMusic();
                        pdfSwitching = false;
                        //println("まだ待機ノートあり & 誤ノート → 停止維持");
                    }
                } else {
                    if (!cmx.isNowPlaying()) {
                        startMusic();
                        pdfSwitching = true;
                        //println("正しいノート取得 → 再生再開");
                    }
                }
            }
        
        } catch (Exception e) {
            e.printStackTrace();
        }

        prevTickPosition = tickPosition;

    }

    @Override
    public void setup() {
        super.setup();

        cmx.showMidiInChooser(this);
        cmx.showMidiOutChooser(this);

        musicData = new MusicData(
                // "kirakira2.mid",
                "kirakira2.mid",
                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12);

        
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

                part.addControlChange(0, 7, 0); // チャンネルのコントロールチェンジを追加

                // test imageNotes
                //addImageNote(part.getNoteOnlyList()[1]);
                addImageNote(part.getNoteOnlyList()[6]);
            });
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        cmx.smfread(musicData.getScc());
        setupModules();

        dataModel = new PianoRollDataModelMultiChannel(
                2,
                2 + 12,
                4,
                channels,
                musicData.getScc());

        ((PianoRollDataModelMultiChannel) dataModel).setPixelPerTick(0.05);

        // PDF画像の読み込み（配列対応）
        try {
            BufferedImage[] bufferedImages = PDFToImage.loadFirstPageSplitHorizontally("kirakira2-midi.pdf");
            //BufferedImage[] bufferedImages = PDFToImage.loadFirstPageSplitHorizontally("TchaikovskyPletnevMarch-midi.pdf");
            pdfImage = new PImage[bufferedImages.length];
            for (int i = 0; i < bufferedImages.length; i++) {
                BufferedImage img = bufferedImages[i];
                pdfImage[i] = new PImage(img.getWidth(), img.getHeight(), ARGB);
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pdfImage[i].pixels, 0,
                        img.getWidth());
                pdfImage[i].updatePixels();
            }
            //System.out.println("PDF画像配列読み込み成功: " + pdfImage.length + "ページ");
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

            ReceiverModule receiver = new ReceiverModule(musicData.getScc().toDataSet());

            cmx.addSPModule(mi);
            cmx.addSPModule(receiver);
            cmx.addSPModule(mo);

            // モジュール接続
            //cmx.connect(mi, 0, mo, 0);
            cmx.connect(mi, 0, receiver, 0); // MIDI入力を受け取る
            cmx.connect(receiver, 0, mo, 0); // OUT にも流す場合

            cmx.startSP();

            //println("モジュール構成完了");

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
            //println("startMusic() 呼び出し！");
            cmx.playMusic();
        }
    }

    void stopMusic() {
        println("stopMusic()3");
        cmx.stopMusic();
    }

    void createMenu() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();

        JMenuBar menuBar = new JMenuBar();

        Button btnStart = new Button("Start");
        btnStart.addActionListener(e -> {
            //startMusic();
        });
        menuBar.add(btnStart);

        Button btnStop = new Button("Stop");
        btnStop.addActionListener(e -> {
            //stopMusic();
        });
        menuBar.add(btnStop);

        frame.setJMenuBar(menuBar);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { MyApp.class.getName() });
    }

    //@Override
    //public void keyPressed() {
    //    super.keyPressed();
    //    switch (keyCode) {
    //        case ENTER -> {
    //            startMusic();
    //            pdfSwitching = true;
    //        }
    //        case BACKSPACE -> {
    //            stopMusic();
    //            pdfSwitching = false;
    //        }
    //    }
    //}
}
