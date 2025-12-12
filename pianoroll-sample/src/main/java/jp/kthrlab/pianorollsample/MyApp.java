//processingのGUIで、曲を選択できるようにする
//pdfを全て読み込む
//1曲目は問題なくpdfを表示できた。2曲目以降がうまくいかない。pdfの切り替えをやって、midiとも対応させる
//allsongs.addの1つ目はうまくいくと思う
//鍵盤の色を決定する

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

    // MyReceiver midiRecorder;
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

        // midiを指定
        musicData = new MusicData(
                // "ex1.mid",
                // "ex2.mid",
                // "ex3.mid",
                // "ex4.mid",
                //"ex5.mid",
                "ex6.mid",

                // "ex1_0to8-midi.mid",
                // "kirakira2.mid",

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
                part.addControlChange(0, 7, 0); // pc操作の時に指定
                // part.addControlChange(1, 7, 0); // piano操作の時

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

        // pdfを指定
        String[] pdfs = {
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
                // "/ex1_72to74.pdf

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
                // "/ex3_58to61.pdf",
                // "/ex3_62to65.pdf",
                // "/ex3_66to70.pdf",
                // "/ex3_71.pdf"

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
                //"/ex5_0to2.pdf",
                //"/ex5_3to7.pdf",
                //"/ex5_8to13.pdf",
                //"/ex5_14to15.pdf",
                //"/ex5_16to20.pdf",
                //"/ex5_21to27.pdf",
                //"/ex5_28to32.pdf",
                //"/ex5_33to35.pdf",
                //"/ex5_36to40.pdf",
                //"/ex5_41to44.pdf",
                //"/ex5_45to50.pdf",
                //"/ex5_51to56.pdf",
                //"/ex5_57to62.pdf",
                //"/ex5_63to66.pdf",
                //"/ex5_67to74.pdf",
                //"/ex5_75to76.pdf"

                // ex6
                 "/ex6_0to3.pdf",
                 "/ex6_4to6.pdf",
                 "/ex6_7to9.pdf",
                 "/ex6_10.pdf",
                 "/ex6_11to14.pdf",
                 "/ex6_15to17.pdf",
                 "/ex6_18to21.pdf",
                 "/ex6_22to23.pdf",
                 "/ex6_24to28.pdf",
                 "/ex6_29to31.pdf",
                 "/ex6_32to36.pdf",
                 "/ex6_37to39.pdf",
                 "/ex6_40to44.pdf",
                 "/ex6_45to50.pdf",
                 "/ex6_51to54.pdf",
                 "/ex6_55to57.pdf"

                // "/kirakira2_first2-midi.pdf",
                // "/kirakira2_first4-midi.pdf",
                // "/kirakira2_first8-midi.pdf",
                // "/kirakira2_5_7-midi.pdf",
                // "/ex1.pdf",
        };

        loadMultiplePdfSlices(pdfs);

        List<int[]> allSongs = new ArrayList<>();

        // 1小節分の音数を指定する
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
        // 3, 3, 6, 4,
        // 4, 5, 1
        // });

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

        // ex6
        allSongs.add(new int[] {
                4, 3, 3, 1,
                4, 3, 4, 2,
                5, 3, 5, 3,
                5, 6, 4, 3
        });

        // --- PdfRange を構築 ---
        List<PdfRange> pdfRanges = new ArrayList<>();

        setupPdfRanges(pdfRanges, allSongs);

        // --- 今回表示したい曲番号 ---
        // int showSong = 2;

        //// --- PdfDisplayRule 設定 ---
        // int songStart = getSongStartIdx(allSongs, showSong);
        // int songEnd = getSongEndIdx(allSongs, showSong);

        List<Integer> highlightList = new ArrayList<>();
        double highlightRate = 0.3; // カラーバーを隠す割合を指定

        for (PdfRange pr : pdfRanges) {
            if (Math.random() < highlightRate) {
                for (int i = pr.startNoteIdx; i <= pr.endNoteIdx; i++) {
                    highlightList.add(i);
                }
            }
        }

        // 以下はシステム1ではコメントアウト
        // カラーバーを隠すかどうかを指定（システム2-カラーバー非表示モードで使用）
        // setHighlightIndexes(highlightList);

        // pdfを表示するかどうかを指定（システム2-楽譜表示モードで使用）
        setPdfDisplayRule(noteIdx -> {

            // if (noteIdx < songStart || noteIdx > songEnd) {
            // return null;
            // }

            for (PdfRange pr : pdfRanges) {
                if (pr.startNoteIdx == noteIdx) {
                    return new ImageNotePianoRoll.PdfDisplay(pr.pdfIndex, 1);
                }
            }

            return null;
        });

        // onSongChanged();

        // カラーバーを隠す部分
        // 1
        // setHighlightIndexes(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7 , 8));

        //// 2
        // setHighlightIndexes(Arrays.asList(0, 1, 2, 3));

        // 3
        // setHighlightIndexes(Arrays.asList(4, 5, 6));

        //// pdf表示部分
        // setPdfDisplayRule(noteIdx -> {
        // // 1
        // if (noteIdx == 0)
        // return new ImageNotePianoRoll.PdfDisplay(1, 1);
        // // 2
        // if (noteIdx == 6)
        // return new ImageNotePianoRoll.PdfDisplay(2, 1);
        // // 3
        // if (noteIdx == 9)
        // return new ImageNotePianoRoll.PdfDisplay(3, 1);
        // // 4
        // if (noteIdx == 13)
        // return new ImageNotePianoRoll.PdfDisplay(4, 1);
        //
        // // 5
        // if (noteIdx == 16)
        // return new ImageNotePianoRoll.PdfDisplay(5, 1);
        //
        // return null;
        // });

        // try {
        // midiRecorder = new MyReceiver();
        // midiTransmitter = MidiSystem.getTransmitter();
        // midiTransmitter.setReceiver(midiRecorder);
        // } catch (MidiUnavailableException e) {
        // e.printStackTrace();
        // }
    }

    @Override
    public void draw() {
        super.draw();

        long tickPosition = cmx.getTickPosition();
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

    }

    // @Override
    // public void stop() {
    // super.stop();
    //
    // if (midiRecorder != null && midiRecorder.getSequence().getTracks()[0].size()
    // > 0) {
    // Sequence seq = midiRecorder.getSequence();
    // File outFile = new
    // File("C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\output.mid");
    // try {
    // int[] types = MidiSystem.getMidiFileTypes(seq);
    // if (types.length > 0) {
    // MidiSystem.write(seq, types[0], outFile);
    // println("MIDI を保存しました: " + outFile.getAbsolutePath());
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // if (midiTransmitter != null) {
    // midiTransmitter.close();
    // }
    // }
    //
    // class MyReceiver implements Receiver {
    // private Sequence sequence;
    // private Track track;
    // private long startTime;
    //
    // public MyReceiver() {
    // try {
    // sequence = new Sequence(Sequence.PPQ, 96);
    // track = sequence.createTrack();
    // startTime = System.currentTimeMillis();
    // } catch (InvalidMidiDataException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // @Override
    // public void send(MidiMessage message, long timeStamp) {
    // if (message instanceof ShortMessage) {
    // long tick = (System.currentTimeMillis() - startTime) / 10;
    // try {
    // track.add(new MidiEvent(message, tick));
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    //
    // @Override
    // public void close() {
    // }
    //
    // public Sequence getSequence() {
    // return sequence;
    // }
    // }

    // public class MidiRecorder {
    //
    // public static void main(String[] args) {
    // try {
    // Sequencer sequencer = MidiSystem.getSequencer();
    // sequencer.open();
    //
    // Transmitter trans = MidiSystem.getTransmitter();
    //
    // // try-catch で囲む
    // MyReceiver myRec;
    // try {
    // myRec = new MyReceiver();
    // } catch (InvalidMidiDataException e) {
    // e.printStackTrace();
    // return; // 例外時は終了
    // }
    //
    // trans.setReceiver(myRec);
    //
    // Sequence sequence = myRec.getSequence();
    //
    // File fileOut = new File(
    // "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\output.mid");
    // int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
    // if (fileTypes.length > 0) {
    // MidiSystem.write(sequence, fileTypes[0], fileOut);
    // }
    //
    // sequencer.close();
    // trans.close();
    //
    // } catch (MidiUnavailableException | IOException e) {
    // e.printStackTrace();
    // }
    // }
    // }
    //
    // static class MyReceiver implements Receiver {
    // private Sequence sequence;
    // private Track track;
    // private long startTime;
    //
    // public MyReceiver() throws MidiUnavailableException, InvalidMidiDataException
    // {
    // // 分解能96のPPQシーケンスを作成
    // this.sequence = new Sequence(Sequence.PPQ, 96);
    // this.track = sequence.createTrack();
    // this.startTime = System.currentTimeMillis();
    // }
    //
    // @Override
    // public void send(MidiMessage message, long timeStamp) {
    // // タイムスタンプは実装依存の場合があるため、システム時刻からの相対時間等に変換してMidiEventを作成する
    // long tick = (System.currentTimeMillis() - startTime) / 10; // 簡単な例
    // track.add(new MidiEvent(message, tick));
    // }
    //
    // @Override
    // public void close() {
    // // リソース解放処理
    // }
    //
    // public Sequence getSequence() {
    // return sequence;
    // }
    // }

    // -----------------------------------------------------------------------------------------------------------------pdf関係--------------

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

    // int getSongStartIdx(List<int[]> songs, int songNumber) {
    // int idx = 0;
    // for (int i = 0; i < songNumber - 1; i++) {
    // for (int len : songs.get(i)) {
    // idx += len;
    // }
    // }
    // return idx;
    // }
    //
    // int getSongEndIdx(List<int[]> songs, int songNumber) {
    // int start = getSongStartIdx(songs, songNumber);
    // int sum = 0;
    // for (int len : songs.get(songNumber - 1)) {
    // sum += len;
    // }
    // return start + sum - 1;
    // }
    //
    // int calculateSongStart(List<int[]> allSongs, int showSong) {
    // // 曲は 1 からスタートしていると仮定（あなたのコードと同じ）
    // int songIndex = showSong - 1;
    //
    // int start = 0;
    // for (int i = 0; i < songIndex; i++) {
    // int[] lengths = allSongs.get(i);
    // for (int len : lengths) {
    // start += len;
    // }
    // }
    //
    // return start; // ← この値が「その曲の最初の noteIdx」
    // }
    //
    // void setCurrentPdfIndex(int pdfIndex) {
    // // 範囲チェック（pdfImage 配列を使う想定）
    // if (pdfImage == null || pdfIndex < 0 || pdfIndex >= pdfImage.length) {
    // // println("setCurrentPdfIndex: invalid index=" + pdfIndex);
    // return;
    // }
    //
    // if (currentPdfDisplayedIndex == pdfIndex)
    // return; // 変化なし
    //
    // currentPdfDisplayedIndex = pdfIndex;
    // // println("PDF を切り替え: index=" + pdfIndex);
    // }
    //
    //// 曲変更時に呼ぶ処理
    // void onSongChanged() {
    // int newStartIdx = calculateSongStart(allSongs, showSong);
    // noteIdx = newStartIdx;
    //
    // // println("曲" + showSong + " の開始 noteIdx = " + noteIdx);
    // updatePdfForNoteIdx(noteIdx);
    // }
    //
    // void updatePdfForNoteIdx(int idx) {
    // for (PdfRange pr : pdfRanges) {
    // if (idx >= pr.startNoteIdx && idx <= pr.endNoteIdx) {
    // // PDF を切り替え
    // setCurrentPdfIndex(pr.pdfIndex);
    // break;
    // }
    // }
    // }

    // -----------------------------------------------------------------------------------------------------------------pdf関係--------------

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

            // println("モジュール構成完了");

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
    }
}
