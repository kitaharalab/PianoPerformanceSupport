package jp.kthrlab.pianorollsample;

import java.awt.Button;
import java.awt.Color; //<>//
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

                // for (int i = 0; i < 4; i++) {
                // addImageNote(part.getNoteOnlyList()[i]);
                // }

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

        //複数PDFを読み込む
        String[] pdfs = {
                "/kirakira2_first2-midi.pdf",
                "/kirakira2_first4-midi.pdf",
                "/kirakira2_first8-midi.pdf"
        };

        loadMultiplePdfSlices(pdfs);

        // カラーバーを隠す部分を指定
        //1
        setHighlightIndexes(Arrays.asList(0, 1, 2, 3, 4, 5, 6));

        ////2
        //setHighlightIndexes(Arrays.asList(0, 1, 2, 3));

        //pdf表示部分を指定
        setPdfDisplayRule(noteIdx -> {
            //1
            if (noteIdx == 6)
               return new ImageNotePianoRoll.PdfDisplay(2, 0);

            ////2
            //if (noteIdx == 3)
            //    return new ImageNotePianoRoll.PdfDisplay(1, 0);

            return null;
        });
    }

    @Override
    public void draw() {
        super.draw();

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
