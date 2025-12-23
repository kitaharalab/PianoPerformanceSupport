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
import jp.kthrlab.pianorollsample.MyApp.PdfRange;
import jp.kthrlab.pianorollsample.MyApp.PdfRangeBuilder;
import processing.core.PApplet;
import processing.core.PImage;

public class MyApp extends ImageNotePianoRoll {
    List<int[]> allSongs = new ArrayList<>();
    int showSong = 1; // 表示する曲番号（デフォルト 1）
    int noteIdx = 0; // 現在のノート位置
    int currentPdfDisplayedIndex = -1;

    private MidiRecorder midiRecorder;

    int loopCount = 0; // 何周目か
    double hideStep = 0.1; // 1周ごとに増やす割合 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    double currentHideRate = 0.0; // 現在の非表示率
    boolean loopJustReset = false; // 先頭に戻った瞬間フラグ

    int subjectId = 2; // 被験者番号を指定
    int takeCount = 16; // 保存番号の始まり（連番）

    private Transmitter midiTransmitter;

    CMXController cmx = CMXController.getInstance();

    List<Integer> highlightCache = new ArrayList<>();

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

        cmx.showMidiInChooser(this);
        cmx.showMidiOutChooser(this);

        midiRecorder = new MidiRecorder();

        // midiを指定 曲
        musicData = new MusicData(
                "kirakira2.mid",
                // "ex1.mid",
                // "ex2.mid",
                // "ex3.mid",
                // !!!!!!!!!!!!!!!!!!!!!!!
                // "ex4.mid",
                // "ex5.mid",
                // "ex6.mid",
                // "test.mid",

                // "ex1_0to8-midi.mid",

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
                part.addControlChange(0, 7, 0); // pc操作の時に指定 音
                part.addControlChange(1, 7, 0); // piano操作の時
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

        // pdfを指定 曲
        String[] pdfs = {
                // kirakira
                "/kirakira_0to3.pdf",
                "/kirakira_4to6.pdf",
                "/kirakira_7to10.pdf",
                "/kirakira_11to13.pdf"

                //// ex1
                // "/ex1_0to5.pdf",
                // "/ex1_6to8.pdf",
                // "/ex1_9to12.pdf",
                // "/ex1_13to15.pdf",
                // "/ex1_16to21.pdf",
                // "/ex1_22to24.pdf",
                // "/ex1_25to29.pdf",
                // "/ex1_30to31.pdf",
                // "/ex1_32to37.pdf",
                // "/ex1_38to40.pdf",
                // "/ex1_41to46.pdf",
                // "/ex1_47to49.pdf",
                // "/ex1_50to53.pdf",
                // "/ex1_54to57.pdf",
                // "/ex1_58to61.pdf",
                // "/ex1_62to65.pdf",
                // "/ex1_66.pdf",
                // "/ex1_67.pdf",
                // "/ex1_68to71.pdf",
                // "/ex1_72to74.pdf"

                //// ex2
                // "/ex2_0to4.pdf",
                // "/ex2_5to6.pdf",
                // "/ex2_7to10.pdf",
                // "/ex2_11to14.pdf",
                // "/ex2_15to18.pdf",
                // "/ex2_19to22.pdf",
                // "/ex2_23to26.pdf",
                // "/ex2_27to30.pdf",
                // "/ex2_31to34.pdf",
                // "/ex2_35to38.pdf",
                // "/ex2_39to44.pdf",
                // "/ex2_45to47.pdf",
                // "/ex2_48to51.pdf",
                // "/ex2_52to53.pdf",
                // "/ex2_54to57.pdf",
                // "/ex2_58to60.pdf",
                // "/ex2_61to64.pdf",
                // "/ex2_65to68.pdf",
                // "/ex2_69to73.pdf",
                // "/ex2_74.pdf"

                //// ex3
                // "/ex3_0to2.pdf",
                // "/ex3_3to5.pdf",
                // "/ex3_6to11.pdf",
                // "/ex3_12.pdf",
                // "/ex3_13to15.pdf",
                // "/ex3_16to18.pdf",
                // "/ex3_19to24.pdf",
                // "/ex3_25to26.pdf",
                // "/ex3_27to32.pdf",
                // "/ex3_33to38.pdf",
                // "/ex3_39to44.pdf",
                // "/ex3_45.pdf",
                // "/ex3_46to48.pdf",
                // "/ex3_49to51.pdf",
                // "/ex3_52to57.pdf",
                // "/ex3_58.pdf"

                //// ex4
                // "/ex4_0to5.pdf",
                // "/ex4_6to8.pdf",
                // "/ex4_9to13.pdf",
                // "/ex4_14to15.pdf",
                // "/ex4_16to21.pdf",
                // "/ex4_22to24.pdf",
                // "/ex4_25to29.pdf",
                // "/ex4_30.pdf",
                // "/ex4_31to36.pdf",
                // "/ex4_37to39.pdf",
                // "/ex4_40to45.pdf",
                // "/ex4_46to48.pdf",
                // "/ex4_49to51.pdf",
                // "/ex4_52to57.pdf",
                // "/ex4_58to62.pdf",
                // "/ex4_63to64.pdf",
                // "/ex4_65to70.pdf",
                // "/ex4_71to73.pdf",
                // "/ex4_74to78.pdf",
                // "/ex4_79to80.pdf",
                // "/ex4_81to86.pdf",
                // "/ex4_87to89.pdf",
                // "/ex4_90to94.pdf",
                // "/ex4_95.pdf"

                //// ex5
                // "/ex5_0to2.pdf",
                // "/ex5_3to7.pdf",
                // "/ex5_8to13.pdf",
                // "/ex5_14to15.pdf",
                // "/ex5_16to20.pdf",
                // "/ex5_21to27.pdf",
                // "/ex5_28to32.pdf",
                // "/ex5_33to35.pdf",
                // "/ex5_36to40.pdf",
                // "/ex5_41to44.pdf",
                // "/ex5_45to50.pdf",
                // "/ex5_51to56.pdf",
                // "/ex5_57to62.pdf",
                // "/ex5_63to66.pdf",
                // "/ex5_67to74.pdf",
                // "/ex5_75to76.pdf"

                //// ex6
                // "/ex6_0to3.pdf",
                // "/ex6_4to6.pdf",
                // "/ex6_7to9.pdf",
                // "/ex6_10.pdf",
                // "/ex6_11to14.pdf",
                // "/ex6_15to17.pdf",
                // "/ex6_18to21.pdf",
                // "/ex6_22to23.pdf",
                // "/ex6_24to28.pdf",
                // "/ex6_29to31.pdf",
                // "/ex6_32to36.pdf",
                // "/ex6_37to39.pdf",
                // "/ex6_40to44.pdf",
                // "/ex6_45to50.pdf",
                // "/ex6_51to54.pdf",
                // "/ex6_55to57.pdf"

                // "/kirakira2_first2-midi.pdf",
                // "/kirakira2_first4-midi.pdf",
                // "/kirakira2_first8-midi.pdf",
                // "/kirakira2_5_7-midi.pdf",
                // "/ex1.pdf",
        };

        loadMultiplePdfSlices(pdfs);

        List<int[]> allSongs = new ArrayList<>();

        // 1小節分の音数を指定 曲

        // kirakira
        allSongs.add(new int[] {
                4, 3, 4, 3
        });

        //// ex1
        // allSongs.add(new int[] {
        // 6, 3, 4, 3,
        // 6, 3, 5, 2,
        // 6, 3, 6, 3,
        // 4, 4, 4, 4,
        // 1, 1, 4, 3
        // });

        //// ex2
        // allSongs.add(new int[] {
        // 5, 2, 4, 4,
        // 4, 4, 4, 4,
        // 4, 4, 6, 3,
        // 4, 2, 4, 3,
        // 4, 4, 5, 1
        // });

        //// ex3
        // allSongs.add(new int[] {
        // 3, 3, 6, 1,
        // 3, 3, 6, 2,
        // 6, 6, 6, 1,
        // 3, 3, 6, 1
        // });

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //// ex4
        // allSongs.add(new int[] {
        // 6, 3, 5, 2,
        // 6, 3, 5, 1,
        // 6, 3, 6, 3,
        // 3, 6, 5, 2,
        // 6, 3, 5, 2,
        // 6, 3, 5, 1
        // });

        //// ex5
        // allSongs.add(new int[] {
        // 3, 5, 6, 2,
        // 5, 7, 5, 3,
        // 5, 4, 6, 6,
        // 6, 4, 8, 2
        // });

        //// ex6 !!!!!!!!!!!!!!!!!!ImageNotePianoRollの分割数を11に変える!!!!!!!!!!!!!!!!!!!!
        // allSongs.add(new int[] {
        // 4, 3, 3, 1,
        // 4, 3, 4, 2,
        // 5, 3, 5, 3,
        // 5, 6, 4, 3
        // });

        setupPdfRanges(pdfRanges, allSongs);

        //// pdfを表示するかどうかを指定 システム2で使用
        //setPdfDisplayRule(noteIdx -> {
//
        //    // if (noteIdx < songStart || noteIdx > songEnd) {
        //    // return null;
        //    // }
//
        //    for (PdfRange pr : pdfRanges) {
        //        if (pr.startNoteIdx == noteIdx) {
        //            return new ImageNotePianoRoll.PdfDisplay(pr.pdfIndex, 1);
        //        }
        //    }
//
        //    return null;
        //});

        // List<Integer> highlightList = new ArrayList<>();
        // double highlightRate = 0.3; // カラーバーを隠す割合
        //
        // for (PdfRange pr : pdfRanges) {
        // if (Math.random() < highlightRate) {
        // for (int i = pr.startNoteIdx; i <= pr.endNoteIdx; i++) {
        // highlightList.add(i);
        // }
        // }
        // }
        //
        //// 以下はシステム1ではコメントアウト
        //// カラーバーを隠すかどうか
        // setHighlightIndexes(highlightList);
    }

    @Override
    public void draw() {
        super.draw();

        long tickPosition = cmx.getTickPosition();
        // ===== 周回検出 =====
        if (tickPosition < lastTickPosition) {
            loopJustReset = true;
        }

        // tick に対応するノートがまだ演奏されていないかチェック
        LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
            // println(tick + " " + performanceData.hasNotesToPlay(tick));
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

            if (millis() - flashStartTime < 120) {

                pushStyle();
                noStroke();
                fill(0, 120, 255, 150);

                int thickness = 30;

                // 上枠
                rect(thickness, 0, width - thickness, thickness);

                // 左枠
                rect(0, 0, thickness, height - 105);

                // 右枠
                rect(width - thickness, thickness, thickness, height - 105 - thickness);

                popStyle();

            } else {
                flash = false;
            }
        }

        //// カラーバーを隠すかどうかを指定 システム2で使用
        //if (loopJustReset) {
        //    loopJustReset = false;
//
        //    loopCount++;
        //    currentHideRate = Math.min(1.0, loopCount * hideStep);
//
        //    updateHighlightByRate(currentHideRate);
        //}

    }

    void updateHighlightByRate(double rate) {

        // List<Integer> newHighlightList = new ArrayList<>();
        highlightCache.clear();

        // === 小節数 ===
        int totalMeasures = pdfRanges.size();
        int hideMeasureCount = (int) Math.round(totalMeasures * rate);

        // 小節インデックスをシャッフル
        List<Integer> measureIndexes = new ArrayList<>();
        for (int i = 0; i < totalMeasures; i++) {
            measureIndexes.add(i);
        }
        java.util.Collections.shuffle(measureIndexes);

        // 隠す小節を選ぶ
        for (int i = 0; i < hideMeasureCount; i++) {
            PdfRange pr = pdfRanges.get(measureIndexes.get(i));

            // その小節に含まれるノートをすべて隠す
            for (int noteIdx = pr.startNoteIdx; noteIdx <= pr.endNoteIdx; noteIdx++) {
                highlightCache.add(noteIdx);
            }
        }

        setHighlightIndexes(highlightCache);
    }

    //// カラーバーを隠す割合を設定して更新
    // void updateHighlightByRate(double rate) {
    //
    // List<Integer> newHighlightList = new ArrayList<>();
    //
    // // === 最大ノート番号を pdfRanges から取得 ===
    // int totalNotes = pdfRanges.stream()
    // .mapToInt(pr -> pr.endNoteIdx)
    // .max()
    // .orElse(0) + 1;
    //
    // int hideCount = (int) (totalNotes * rate);
    //
    // List<Integer> allIndexes = new ArrayList<>();
    // for (int i = 0; i < totalNotes; i++) {
    // allIndexes.add(i);
    // }
    //
    // java.util.Collections.shuffle(allIndexes);
    //
    // for (int i = 0; i < hideCount; i++) {
    // newHighlightList.add(allIndexes.get(i));
    // }
    //
    // setHighlightIndexes(newHighlightList);
    // }

    List<PdfRange> pdfRanges = new ArrayList<>();

    public class PdfRange {
        public final int startNoteIdx;
        public final int endNoteIdx;
        public final int pdfIndex;

        public PdfRange(int startNoteIdx, int endNoteIdx, int pdfIndex) {
            this.startNoteIdx = startNoteIdx;
            this.endNoteIdx = endNoteIdx;
            this.pdfIndex = pdfIndex;
        }
    }

    public class PdfRangeBuilder {

        private final List<PdfRange> list;
        private int currentNoteIdx = 0;

        public PdfRangeBuilder(List<PdfRange> list) {
            this.list = list;
        }

        public PdfRangeBuilder add(int length, int pdfIndex) {

            int start = currentNoteIdx;
            int end = currentNoteIdx + length - 1;

            list.add(new PdfRange(start, end, pdfIndex));
            currentNoteIdx += length;

            return this;
        }

    }

    public void setupPdfRanges(List<PdfRange> pdfRanges, List<int[]> songLengthsList) {

        PdfRangeBuilder b = new PdfRangeBuilder(pdfRanges);
        int pdfIndex = 0;

        for (int[] lengths : songLengthsList) {
            for (int len : lengths) {
                b.add(len, pdfIndex);
                pdfIndex++;
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

            ReceiverModule receiver = new ReceiverModule(musicData.getScc().toDataSet(), midiRecorder);

            cmx.addSPModule(mi);
            cmx.addSPModule(receiver);
            cmx.addSPModule(pianoTeacherModule);
            cmx.addSPModule(mo);

            cmx.connect(mi, 0, receiver, 0);
            cmx.connect(receiver, 0, pianoTeacherModule, 0);
            cmx.connect(pianoTeacherModule, 0, mo, 0);

            // cmx.addSPModule(mi);
            // cmx.addSPModule(pianoTeacherModule);
            // cmx.addSPModule(mo);
            //
            //// モジュール接続
            // cmx.connect(mi, 0, pianoTeacherModule, 0);
            // cmx.connect(pianoTeacherModule, 0, mo, 0); // OUT にも流す場合

            cmx.startSP();

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
        // System.out.println("startMusic");
        if (!cmx.isNowPlaying()) {
            cmx.playMusic();
        }
        flash = true;
        flashStartTime = millis();
    }

    void stopMusic() {
        // System.out.println("stopMusic");
        if (cmx.isNowPlaying()) {
            cmx.stopMusic();
        }
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

        if (key == 's') {
            stopMusic();
            String filename = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\midi\\" +
                    "subject" + subjectId + "_take" + takeCount + ".mid";

            midiRecorder.save(filename);
            takeCount++;

            // cmx.setTickPosition(0);
            midiRecorder.reset();
            // midiRecorder.reset();
        }
    }
}
