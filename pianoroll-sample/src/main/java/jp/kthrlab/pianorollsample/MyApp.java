//processingのGUIで、曲を選択できるようにする
//pdfを全て読み込む
//startのカラーバーの部分に赤線を弾く
//1曲目は問題なくpdfを表示できた。2曲目以降がうまくいかない。pdfの切り替えをやって、midiとも対応させる
//allsongs.addの1つ目はうまくいくと思う

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
import processing.core.PApplet;
import processing.core.PImage;

public class MyApp extends ImageNotePianoRoll {
    List<int[]> allSongs = new ArrayList<>();
    int showSong = 1; // 表示する曲番号（デフォルト 1）
    int noteIdx = 0; // 現在のノート位置

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
                "ex1.mid",
                // "ex2.mid",
                // "ex3.mid",
                // "ex4.mid",
                // "ex5.mid",
                // "ex6.mid",

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
                part.addControlChange(0, 7, 0); // pc操作の時
                // part.addControlChange(1, 7, 0); //piano操作の時

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

        // 複数PDFを読み込む
        String[] pdfs = {
                // "/kirakira2_first2-midi.pdf",
                // "/kirakira2_first4-midi.pdf",
                // "/kirakira2_first8-midi.pdf",
                // "/kirakira2_5_7-midi.pdf",

                "/ex1.pdf",
                "/ex1_0to5.pdf",
                "/ex1_6to8.pdf",
                "/ex1_9to12.pdf",
                "/ex1_13to15.pdf",
                "/ex1_16to21.pdf",
                "/ex1_22to24.pdf",
                "/ex1_25to29.pdf",
                "/ex1_30to31.pdf",
                "/ex1_32to37.pdf",
                "/ex1_38to40.pdf",
                "/ex1_41to46.pdf",
                "/ex1_47to49.pdf",
                "/ex1_50to53.pdf",
                "/ex1_54to57.pdf",
                "/ex1_58to61.pdf",
                "/ex1_62to65.pdf",
                "/ex1_66.pdf",
                "/ex1_67.pdf",
                "/ex1_68to71.pdf",
                "/ex1_72to74.pdf",

                "/ex2_0to4.pdf",
                "/ex2_5to6.pdf",
                "/ex2_7to10.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                // "/ex2_to.pdf",
                //
                // "/ex1_0to5.pdf",
                // "/ex1_6to8.pdf",
                // "/ex1_9to12.pdf",
                // "/ex1_13to15.pdf",
                // "/ex1_16to21.pdf",
                // "/ex1_0to5.pdf",
                // "/ex1_6to8.pdf",
                // "/ex1_9to12.pdf",
                // "/ex1_13to15.pdf",
                // "/ex1_16to21.pdf",
        };

        loadMultiplePdfSlices(pdfs);

        // List<int[]> allSongs = new ArrayList<>();

        allSongs.add(new int[] {
                6, 3, 4, 3,
                6, 3, 5, 2,
                6, 3, 6, 3,
                4, 4, 4, 4,
                1, 1, 4, 3
        });

        allSongs.add(new int[] {
                5, 2, 4
        });

        allSongs.add(new int[] {
                8, 2, 2, 2
        });

        // --- PdfRange を構築 ---
        List<PdfRange> pdfRanges = new ArrayList<>();
        setupPdfRanges(pdfRanges, allSongs);

        // --- 今回表示したい曲番号 ---
        // int showSong = 2;

        // --- PdfDisplayRule 設定 ---
        int songStart = getSongStartIdx(allSongs, showSong);
        int songEnd = getSongEndIdx(allSongs, showSong);

        setPdfDisplayRule(noteIdx -> {

            if (noteIdx < songStart || noteIdx > songEnd) {
                return null;
            }

            for (PdfRange pr : pdfRanges) {
                if (pr.startNoteIdx == noteIdx) {
                    return new ImageNotePianoRoll.PdfDisplay(pr.pdfIndex, 1);
                }
            }

            return null;
        });

        onSongChanged();

        // setupPdfRanges(pdfRanges);
        //
        // setPdfDisplayRule(noteIdx -> {
        //
        // for (PdfRange pr : pdfRanges) {
        //
        // // ① noteIdx が PdfRange の start と一致したときだけ表示を切り替える
        // if (noteIdx == pr.start) {
        // return new ImageNotePianoRoll.PdfDisplay(pr.pdfIndex, 1);
        // }
        // }
        //
        // return null; // 該当なし → 何も表示しない
        // });

        // カラーバーを隠す部分を指定
        // 1
        // setHighlightIndexes(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7 , 8));

        //// 2
        // setHighlightIndexes(Arrays.asList(0, 1, 2, 3));

        // 3
        // setHighlightIndexes(Arrays.asList(4, 5, 6));

        //// pdf表示部分を指定
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
    }

    @Override
    public void draw() {
        super.draw();

        long tickPosition = cmx.getTickPosition();
        // tick に対応するノートがまだ演奏されていないかチェック
        LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
            //println(tick + " " + performanceData.hasNotesToPlay(tick));
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

        //// === 停止時フラッシュ表示 ===
        // if (flash) {
        // // 120ミリ秒だけ光る
        // if (millis() - flashStartTime < 120) {
        // pushStyle();
        // fill(255, 255, 0, 120); // 黄色っぽい光（透明）
        // noStroke();
        // rect(0, 0, width, height); // 全画面を覆う
        // popStyle();
        // } else {
        // flash = false; // フラッシュ終了
        // }
        // }

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

        @Override
        public String toString() {
            return "PdfRange[start=" + startNoteIdx +
                    ", end=" + endNoteIdx +
                    ", pdfIndex=" + pdfIndex + "]";
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
        int pdfIndex = 1;

        for (int[] lengths : songLengthsList) {
            for (int len : lengths) {
                b.add(len, pdfIndex);
                pdfIndex++;
            }
        }
    }

    int getSongStartIdx(List<int[]> songs, int songNumber) {
        int idx = 0;
        for (int i = 0; i < songNumber - 1; i++) {
            for (int len : songs.get(i)) {
                idx += len;
            }
        }
        return idx;
    }

    int getSongEndIdx(List<int[]> songs, int songNumber) {
        int start = getSongStartIdx(songs, songNumber);
        int sum = 0;
        for (int len : songs.get(songNumber - 1)) {
            sum += len;
        }
        return start + sum - 1;
    }

    int calculateSongStart(List<int[]> allSongs, int showSong) {
        // 曲は 1 からスタートしていると仮定（あなたのコードと同じ）
        int songIndex = showSong - 1;

        int start = 0;
        for (int i = 0; i < songIndex; i++) {
            int[] lengths = allSongs.get(i);
            for (int len : lengths) {
                start += len;
            }
        }

        return start; // ← この値が「その曲の最初の noteIdx」
    }

    // 曲変更時に呼ぶ処理
    void onSongChanged() {
        int newStartIdx = calculateSongStart(allSongs, showSong);
        noteIdx = newStartIdx;

        //println("曲" + showSong + " の開始 noteIdx = " + noteIdx);
        //updatePdfForNoteIdx(noteIdx);
    }

    //void updatePdfForNoteIdx(int idx) {
    //    for (PdfRange pr : pdfRanges) {
    //        if (idx >= pr.startNoteIdx && idx <= pr.endNoteIdx) {
    //            // PDF を切り替え
    //            setCurrentPdfIndex(pr.pdfIndex);
    //            break;
    //        }
    //    }
    //}

    // class PdfRange {
    // int start;
    // int end;
    // int pdfIndex;
    //
    // PdfRange(int start, int end, int pdfIndex) {
    // this.start = start;
    // this.end = end;
    // this.pdfIndex = pdfIndex;
    // }
    //
    // boolean contains(int idx) {
    // return idx >= start && idx <= end;
    // }
    // }
    //
    // class PdfRangeBuilder {
    // private int current = 0;
    // private List<PdfRange> list;
    //
    // PdfRangeBuilder(List<PdfRange> list) {
    // this.list = list;
    // }
    //
    // public PdfRangeBuilder add(int length, int pdfIndex) {
    // int start = current;
    // int end = start + length - 1;
    // list.add(new PdfRange(start, end, pdfIndex));
    // current = end + 1;
    // return this;
    // }
    // }
    //
    // void setupPdfRanges(List<PdfRange> pdfRanges) {
    //
    // PdfRangeBuilder b = new PdfRangeBuilder(pdfRanges);
    //
    // b.add(6, 1) // noteIdx 0〜5 → PDF1
    // .add(3, 2) // noteIdx 6〜8 → PDF2
    // .add(4, 3)
    // .add(3, 4)
    // .add(6, 3)
    // .add(3, 4)
    // .add(5, 3)
    // .add(2, 4)
    // .add(6, 3)
    // .add(3, 4)
    // .add(6, 3)
    // .add(3, 4)
    // .add(4, 3)
    // .add(4, 4)
    // .add(4, 4)
    // .add(4, 3)
    // .add(1, 4)
    // .add(1, 3)
    // .add(4, 4)
    // .add(3, 3);
    //
    // }

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
        //System.out.println("startMusic");
        if (!cmx.isNowPlaying()) {
            cmx.playMusic();
        }
        flash = true;
        flashStartTime = millis();
    }

    void stopMusic() {
        //System.out.println("stopMusic");
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
