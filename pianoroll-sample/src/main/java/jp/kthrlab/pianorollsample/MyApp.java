package jp.kthrlab.pianorollsample;

import java.awt.Color;
import java.util.ArrayList; //<>//
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
    List<int[]> allSongs = new ArrayList<>();
    int showSong = 1; // 表示する曲番号（デフォルト 1）
    int noteIdx = 0; // 現在のノート位置
    int currentPdfDisplayedIndex = -1;

    private MidiRecorder midiRecorder;
    private PianoTeacherModule pianoTeacherModule;
    private ReceiverModule receiver;
    private MidiOutputModule mo;

    int loopCount = 0; // 何周目か
    double hideStep = 0.2; // 1周ごとに増やす割合
    double currentHideRate = 0.0; // 現在の非表示率
    boolean loopJustReset = false; // 先頭に戻った瞬間フラグ

    int subjectId = 100; // 被験者番号を指定
    int takeCount = 0; // 保存番号の始まり（連番）

    // String rhythmState = "NONE";
    double rhythmDiff = 0.0;
    boolean rhythmMode = false;
    boolean twoMeasureRepeatMode = false; // 2小節交代リズム習得モード
    boolean isDemoTurn = true;           // true: お手本ターン, false: 演奏ターン
    int currentBlockIndex = 0;          // 現在の2小節ブロック番号
    String lastRhythmResult = "NONE";

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

    RhythmJudge rhythmJudge;

    boolean flash = false;
    long flashStartTime = 0;

    @Override
    public void setup() {
        super.setup();

        cmx.showMidiInChooser(this);
        cmx.showMidiOutChooser(this);

        midiRecorder = new MidiRecorder();

        // 最初の曲を初期読み込み
        loadSong(SONGS.get(0));

        setupModules();

        // pdfを表示するかどうかを指定 システム2で使用
        setPdfDisplayRule(noteIdx -> {
            for (PdfRange pr : pdfRanges) {
                if (pr.startNoteIdx == noteIdx) {
                    return new ImageNotePianoRoll.PdfDisplay(pr.pdfIndex, 1);
                }
            }
            return null;
        });
        createMenu();
    }

    @Override
    protected boolean isLearningMode() {
        return !rhythmMode && !twoMeasureRepeatMode;
    }

    @Override
    public void draw() {
        super.draw();

        long tickPosition = cmx.getTickPosition();

        if (twoMeasureRepeatMode) {
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
            if (dataModelMultiChannel != null) {
                int division = dataModelMultiChannel.getScc().getDivision();
                int beatNum = dataModelMultiChannel.getBeatNum();
                int measureTicks = beatNum * division;
                int twoMeasureTicks = measureTicks * 2;

                int totalMeasures = getTotalMeasures();
                int totalBlocks = (int) Math.ceil((double) totalMeasures / 2.0);
                if (totalBlocks <= 0) totalBlocks = 1;

                // 曲末尾に達した際の2周目への安全リセット
                if (cmx.getMicrosecondPosition() >= cmx.getMicrosecondLength() && cmx.getMicrosecondLength() > 0) {
                    currentBlockIndex = 0;
                    isDemoTurn = true;
                    setPartVolume(100);
                    performanceData.setNotesToPlayForRange(0, twoMeasureTicks);
                    cmx.setTickPosition(0);
                    lastTickPosition = 0;
                    tickPosition = 0;
                }

                if (currentBlockIndex >= totalBlocks) {
                    currentBlockIndex = 0;
                }

                long blockStartTick = (long) currentBlockIndex * twoMeasureTicks;
                long blockEndTick = blockStartTick + twoMeasureTicks;

                if (isDemoTurn) {
                    //  お手本ターン (2小節)  PC/モニターからお手本音を鳴らす
                    setPartVolume(100);
                    if (!cmx.isNowPlaying()) {
                        cmx.playMusic();
                    }

                    if (tickPosition >= blockEndTick && tickPosition >= blockStartTick) {
                        stopMusic();
                        silenceAllChannels();
                        setPartVolume(0);
                        performanceData.setNotesToPlayForRange(blockStartTick, blockEndTick);
                        cmx.setTickPosition(blockStartTick);
                        lastTickPosition = blockStartTick;
                        isDemoTurn = false; // 演奏ターンへ移行
                    }
                } else {
                    //  演奏ターン (2小節)  手本音は消音
                    setPartVolume(0);

                    LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
                        if (performanceData.hasNotesToPlay(tick)) {
                            stopMusic(); // 未演奏ノートがあれば停止して待機
                        } else {
                            startMusic();
                        }
                    });

                    if (tickPosition >= blockEndTick && tickPosition >= blockStartTick) {
                        stopMusic();
                        silenceAllChannels();
                        currentBlockIndex++;

                        if (currentBlockIndex >= totalBlocks) {
                            currentBlockIndex = 0;
                        }

                        long nextBlockStartTick = (long) currentBlockIndex * twoMeasureTicks;
                        long nextBlockEndTick = nextBlockStartTick + twoMeasureTicks;

                        performanceData.setNotesToPlayForRange(nextBlockStartTick, nextBlockEndTick);
                        cmx.setTickPosition(nextBlockStartTick);
                        lastTickPosition = nextBlockStartTick;
                        isDemoTurn = true; // 次のブロックのお手本ターンへ移行
                        setPartVolume(100);
                    }
                }
            }
            lastTickPosition = tickPosition;
        } else {
            //  Learning Mode & Rhythm Mode 
            setPartVolume(0);
            if (cmx.getMicrosecondPosition() >= cmx.getMicrosecondLength() && cmx.getMicrosecondLength() > 0) {
                rhythmJudge.reset();
                loopJustReset = true;
                performanceData.setNotesToPlay();
                cmx.setTickPosition(0); // 再生位置を先頭に戻す
                tickPosition = 0;
            }

            LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
                if (performanceData.hasNotesToPlay(tick)) {
                    stopMusic(); // ノートが残っていれば停止
                } else {
                    startMusic(); // すべて演奏済みなら再開
                }
            });
            lastTickPosition = tickPosition;
        }

        // === 停止時フラッシュ表示 === ★
        if (flash) {
            if (millis() - flashStartTime < 120) {

                int r = 0;
                int g = 120;
                int b = 255;

                switch (lastRhythmResult) {

                    case "OK":
                        r = 0;
                        g = 180;
                        b = 255; // 青（正解）
                        break;

                    case "EARLY":
                        r = 0;
                        g = 255;
                        b = 120; // 緑（速い）
                        break;

                    case "LATE":
                        r = 255;
                        g = 80;
                        b = 80; // 赤（遅い）
                        break;

                    case "WRONG_NOTE":
                        r = 255;
                        g = 0;
                        b = 0; // 赤強め（ミス）
                        break;

                    default:
                        r = 200;
                        g = 200;
                        b = 200;
                }

                pushStyle();
                noStroke();
                // 画面全体に上品なカラーオーバーレイ
                fill(r, g, b, 30);
                rect(0, 0, width, height);

                // 画面最上部に細いスタイリッシュなネオンバーを表示
                fill(r, g, b, 220);
                rect(0, 0, width, 5);
                popStyle();

            } else {
                flash = false;
            }
        }

        // カラーバーを隠すかどうかを指定 システム2で使用 コメントアウトでカラーバー非表示機能なし ★
        if (loopJustReset) {
            loopJustReset = false;

            if (!rhythmMode && !twoMeasureRepeatMode) {
                loopCount++;
                currentHideRate = Math.min(1.0, loopCount * hideStep);
                updateHighlightByRate(currentHideRate);
            }
        }

        // モード表示のモダン化
        pushStyle();
        textSize(14);
        textAlign(LEFT, CENTER);

        String modeName;
        if (twoMeasureRepeatMode) {
            modeName = isDemoTurn ? "2-MEASURE (DEMO)" : "2-MEASURE (PLAY)";
        } else if (rhythmMode) {
            modeName = "RHYTHM";
        } else {
            modeName = "LEARNING";
        }

        String prefixText = "MODE: ";
        String modeText = prefixText + modeName;

        // 位置
        float boxX = 20;
        float boxY = 30;
        float paddingX = 15;

        float boxW = textWidth(modeText) + paddingX * 2;
        float boxH = 36;

        // 半透明のダークネイビー背景 (グラスモフィズム風)
        fill(18, 22, 30, 210);

        // モード別に境界線色を指定
        if (twoMeasureRepeatMode) {
            if (isDemoTurn) {
                stroke(255, 180, 50); // ネオンオレンジ（お手本）
            } else {
                stroke(50, 220, 120); // ネオングリーン（演奏）
            }
        } else if (rhythmMode) {
            stroke(255, 75, 75); // ネオンレッド
        } else {
            stroke(64, 156, 255); // ネオンブルー
        }
        strokeWeight(1.5f);
        rect(width - boxW - boxX, boxY, boxW, boxH, 6);

        // テキスト描画 (コントラストを高くして見やすさを向上)
        noStroke();
        fill(240); // 白に近いグレー
        float textStartX = width - boxW - boxX + paddingX;
        float textY = boxY + boxH / 2f - 1;
        text(prefixText, textStartX, textY);

        if (twoMeasureRepeatMode) {
            if (isDemoTurn) {
                fill(255, 190, 70);
            } else {
                fill(80, 235, 140);
            }
        } else if (rhythmMode) {
            fill(255, 95, 95);
        } else {
            fill(95, 175, 255);
        }
        text(modeName, textStartX + textWidth(prefixText), textY);
        popStyle();
    }

    // ★
    public void setRhythmResult(String result) {
        lastRhythmResult = result;
        flash = true;
        flashStartTime = millis();
    }

    void updateHighlightByRate(double rate) {
        highlightCache.clear();
        if (dataModel == null) return;

        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;

        // 総小節数の取得
        int totalMeasures = getTotalMeasures();
        int hideMeasureCount = (int) Math.round(totalMeasures * rate);

        // 小節インデックスをシャッフル
        List<Integer> measureIndexes = new ArrayList<>();
        for (int i = 0; i < totalMeasures; i++) {
            measureIndexes.add(i);
        }
        java.util.Collections.shuffle(measureIndexes);

        // 隠す小節を決定
        List<Integer> targetMeasures = new ArrayList<>();
        for (int i = 0; i < hideMeasureCount; i++) {
            targetMeasures.add(measureIndexes.get(i));
        }

        // 隠す小節に属するノートインデックスを highlightCache に追加
        if (!dataModelMultiChannel.getChannels().isEmpty()) {
            int mainChannelNum = dataModelMultiChannel.getChannels().get(0).channel_number;
            var part = dataModelMultiChannel.getPart(mainChannelNum); // 主旋律パート
            if (part != null && part.getNoteOnlyList() != null) {
                var notes = part.getNoteOnlyList();
                for (int i = 0; i < notes.length; i++) {
                    int noteMeasure = (int) (notes[i].onset() / measureTicks);
                    if (targetMeasures.contains(noteMeasure)) {
                        highlightCache.add(i);
                    }
                }
            }
        }

        setHighlightIndexes(highlightCache);
    }




    private void setupModules() {
        try {
            MidiInputModule mi = cmx.createMidiIn(); // 入力モジュール
            mo = cmx.createMidiOut(); // 出力モジュール

            pianoTeacherModule = new PianoTeacherModule(
                    musicData.getScc().toDataSet(),
                    performanceData, rhythmJudge, this);

            receiver = new ReceiverModule(musicData.getScc().toDataSet(), midiRecorder, performanceData);

            cmx.addSPModule(mi);
            cmx.addSPModule(receiver);
            cmx.addSPModule(pianoTeacherModule);
            cmx.addSPModule(mo);

            cmx.connect(mi, 0, receiver, 0);
            cmx.connect(receiver, 0, pianoTeacherModule, 0);
            cmx.connect(pianoTeacherModule, 0, mo, 0);

            cmx.startSP();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideDefaultTitleBar() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();
        frame.removeNotify();
        frame.setUndecorated(true);
        frame.addNotify();
    }

    void startMusic() {
        if (!cmx.isNowPlaying()) {
            cmx.playMusic();
        }
        if (rhythmMode) {
            flash = true;
            flashStartTime = millis();
        }
    }

    void stopMusic() {
        if (cmx.isNowPlaying()) {
            cmx.stopMusic();
        }
    }

    private void setPartVolume(int volume) {
        if (musicData != null && musicData.getScc() != null) {
            try {
                long currentTick = cmx.getTickPosition();
                Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEach(part -> {
                    part.addControlChange(0, 7, volume);
                    part.addControlChange(currentTick, 7, volume);
                });
            } catch (Exception e) {
                // 無視
            }
        }
    }

    public boolean shouldMuteMidiOutput() {
        return twoMeasureRepeatMode && !isDemoTurn;
    }

    private void silenceAllChannels() {
        if (musicData != null && musicData.getScc() != null) {
            try {
                long currentTick = cmx.getTickPosition();
                Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEach(part -> {
                    part.addControlChange(currentTick, 7, 0);
                    part.addControlChange(currentTick, 120, 0); // All Sound Off
                    part.addControlChange(currentTick, 123, 0); // All Notes Off
                });
            } catch (Exception e) {
                // 無視
            }
        }
    }

    void setAppMode(int modeIndex) {
        stopMusic();
        loopCount = 0;
        currentHideRate = 0.0;
        highlightCache.clear();
        setHighlightIndexes(highlightCache);

        if (modeIndex == 0) {
            rhythmMode = false;
            twoMeasureRepeatMode = false;
            setPartVolume(0);
        } else if (modeIndex == 1) {
            rhythmMode = true;
            twoMeasureRepeatMode = false;
            setPartVolume(0);
        } else if (modeIndex == 2) {
            rhythmMode = false;
            twoMeasureRepeatMode = true;
            isDemoTurn = true;
            currentBlockIndex = 0;
            setPartVolume(100);
            if (dataModel != null) {
                PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
                int division = dataModelMultiChannel.getScc().getDivision();
                int beatNum = dataModelMultiChannel.getBeatNum();
                int twoMeasureTicks = beatNum * division * 2;
                if (performanceData != null) {
                    performanceData.setNotesToPlayForRange(0, twoMeasureTicks);
                }
            }
        }

        cmx.setTickPosition(0);
        lastTickPosition = 0;
        if (modeIndex != 2 && performanceData != null) {
            performanceData.setNotesToPlay();
        }
        if (rhythmJudge != null) {
            rhythmJudge.reset();
        }
    }

    @Override
    public void drawCurrentNote() {
        super.drawCurrentNote();

        // お手本ターン中は、今鳴っているお手本の鍵盤位置を鮮やかなネオンブルーで光らせる
        if (twoMeasureRepeatMode && isDemoTurn && dataModel != null) {
            long tickPosition = cmx.getTickPosition();
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

            for (jp.kthrlab.pianoroll.Channel channel : dataModelMultiChannel.getChannels()) {
                var part = dataModelMultiChannel.getPart(channel.channel_number);
                if (part == null || part.getNoteOnlyList() == null) continue;

                for (var note : part.getNoteOnlyList()) {
                    if (tickPosition >= note.onset() && tickPosition <= note.offset()) {
                        drawKeyHighlight(note.notenum());
                    }
                }
            }
        }
    }

    private void drawKeyHighlight(int currentNote) {
        if (!(keyboard instanceof jp.kthrlab.pianoroll.HorizontalKeyboard)) return;
        jp.kthrlab.pianoroll.HorizontalKeyboard hk = (jp.kthrlab.pianoroll.HorizontalKeyboard) keyboard;

        float x;
        boolean isWhite = jp.kthrlab.pianoroll.Keyboard.isWhiteKey(currentNote);

        if (isWhite) {
            int whiteIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (jp.kthrlab.pianoroll.Keyboard.isWhiteKey(nn)) whiteIndex++;
            }
            if (whiteIndex < hk.whiteKeyX.size()) {
                x = hk.whiteKeyX.get(whiteIndex);
            } else return;
        } else {
            int blackIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (!jp.kthrlab.pianoroll.Keyboard.isWhiteKey(nn)) blackIndex++;
            }
            if (blackIndex < hk.blackKeyX.size()) {
                x = hk.blackKeyX.get(blackIndex);
            } else return;
        }

        if (x >= getDynamicScoreLeftX() - 10 && x < getDynamicScoreRightX() - 10) return;

        float w = isWhite ? hk.getWhiteKeyWidth() : hk.getBlackKeyWidth();
        float h = isWhite ? hk.getWhiteKeyHeight() : hk.getBlackKeyHeight();
        float y = hk.getKeyboardY();

        pushStyle();
        noStroke();
        for (int i = 0; i < 5; i++) {
            fill(64, 156, 255, 45 - i * 9);
            rect(x - i, y - i, w + i * 2, h + i * 2, 3);
        }
        fill(64, 156, 255, 210);
        rect(x, y, w, h, 2);
        fill(160, 210, 255);
        rect(x + 1, y, w - 2, 4);
        popStyle();
    }

    void createMenu() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();

        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();

        // モード選択メニュー
        javax.swing.JMenu modeMenu = new javax.swing.JMenu("Mode");
        javax.swing.JMenuItem learningItem = new javax.swing.JMenuItem("Learning Mode");
        learningItem.addActionListener(e -> setAppMode(0));

        javax.swing.JMenuItem rhythmItem = new javax.swing.JMenuItem("Rhythm Mode");
        rhythmItem.addActionListener(e -> setAppMode(1));

        javax.swing.JMenuItem twoMeasureItem = new javax.swing.JMenuItem("2-Measure Repeat Mode");
        twoMeasureItem.addActionListener(e -> setAppMode(2));

        modeMenu.add(learningItem);
        modeMenu.add(rhythmItem);
        modeMenu.add(twoMeasureItem);
        menuBar.add(modeMenu);

        javax.swing.JMenu settingsMenu = new javax.swing.JMenu("Options");
        javax.swing.JMenuItem saveMidiItem = new javax.swing.JMenuItem("Save MIDI (S)");
        saveMidiItem.addActionListener(e -> {
            stopMusic();
            String filename = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\midi\\" +
                    "subject" + subjectId + "_take" + takeCount + ".mid";

            midiRecorder.save(filename);
            takeCount++;

            midiRecorder.reset();
        });
        settingsMenu.add(saveMidiItem);
        menuBar.add(settingsMenu);

        // 曲切り替え用メニューを追加
        javax.swing.JMenu songsMenu = new javax.swing.JMenu("Songs");
        for (SongConfig song : SONGS) {
            javax.swing.JMenuItem songItem = new javax.swing.JMenuItem(song.name);
            songItem.addActionListener(e -> {
                loadSong(song);
            });
            songsMenu.add(songItem);
        }
        menuBar.add(songsMenu);

        frame.setJMenuBar(menuBar);
        frame.revalidate();
    }

    public static class SongConfig {
        public final String name;
        public final String midiFile;
        public final String[] pdfFiles;

        public SongConfig(String name, String midiFile, String[] pdfFiles) {
            this.name = name;
            this.midiFile = midiFile;
            this.pdfFiles = pdfFiles;
        }
    }

    private static final List<SongConfig> SONGS = List.of(
        new SongConfig(
            "きらきら星 (Kirakira)",
            "kirakira2.mid",
            new String[]{
                "/kirakira_0to3.pdf",
                "/kirakira_4to6.pdf",
                "/kirakira_7to10.pdf",
                "/kirakira_11to13.pdf"
            }
        ),
        new SongConfig(
            "練習曲1 (Ex1)",
            "ex1.mid",
            new String[]{
                "/ex1_0to5.pdf", "/ex1_6to8.pdf", "/ex1_9to12.pdf", "/ex1_13to15.pdf",
                "/ex1_16to21.pdf", "/ex1_22to24.pdf", "/ex1_25to29.pdf", "/ex1_30to31.pdf",
                "/ex1_32to37.pdf", "/ex1_38to40.pdf", "/ex1_41to46.pdf", "/ex1_47to49.pdf",
                "/ex1_50to53.pdf", "/ex1_54to57.pdf", "/ex1_58to61.pdf", "/ex1_62to65.pdf",
                "/ex1_66.pdf", "/ex1_67.pdf", "/ex1_68to71.pdf", "/ex1_72to74.pdf"
            }
        ),
        new SongConfig(
            "練習曲2 (Ex2)",
            "ex2.mid",
            new String[]{
                "/ex2_0to4.pdf", "/ex2_5to6.pdf", "/ex2_7to10.pdf", "/ex2_11to14.pdf",
                "/ex2_15to18.pdf", "/ex2_19to22.pdf", "/ex2_23to26.pdf", "/ex2_27to30.pdf",
                "/ex2_31to34.pdf", "/ex2_35to38.pdf", "/ex2_39to44.pdf", "/ex2_45to47.pdf",
                "/ex2_48to51.pdf", "/ex2_52to53.pdf", "/ex2_54to57.pdf", "/ex2_58to60.pdf",
                "/ex2_61to64.pdf", "/ex2_65to68.pdf", "/ex2_69to73.pdf", "/ex2_74.pdf"
            }
        ),
        new SongConfig(
            "練習曲3 (Ex3)",
            "ex3.mid",
            new String[]{
                "/ex3_0to2.pdf", "/ex3_3to5.pdf", "/ex3_6to11.pdf", "/ex3_12.pdf",
                "/ex3_13to15.pdf", "/ex3_16to18.pdf", "/ex3_19to24.pdf", "/ex3_25to26.pdf",
                "/ex3_27to32.pdf", "/ex3_33to38.pdf", "/ex3_39to44.pdf", "/ex3_45.pdf",
                "/ex3_46to48.pdf", "/ex3_49to51.pdf", "/ex3_52to57.pdf", "/ex3_58.pdf"
            }
        ),
        new SongConfig(
            "練習曲4 (Ex4)",
            "ex4.mid",
            new String[]{
                "/ex4_0to5.pdf", "/ex4_6to8.pdf", "/ex4_9to13.pdf", "/ex4_14to15.pdf",
                "/ex4_16to21.pdf", "/ex4_22to24.pdf", "/ex4_25to29.pdf", "/ex4_30.pdf",
                "/ex4_31to36.pdf", "/ex4_37to39.pdf", "/ex4_40to45.pdf", "/ex4_46to48.pdf",
                "/ex4_49to51.pdf", "/ex4_52to57.pdf", "/ex4_58to62.pdf", "/ex4_63to64.pdf",
                "/ex4_65to70.pdf", "/ex4_71to73.pdf", "/ex4_74to78.pdf", "/ex4_79to80.pdf",
                "/ex4_81to86.pdf", "/ex4_87to89.pdf", "/ex4_90to94.pdf", "/ex4_95.pdf"
            }
        ),
        new SongConfig(
            "練習曲5 (Ex5)",
            "ex5.mid",
            new String[]{
                "/ex5_0to2.pdf", "/ex5_3to7.pdf", "/ex5_8to13.pdf", "/ex5_14to15.pdf",
                "/ex5_16to20.pdf", "/ex5_21to27.pdf", "/ex5_28to32.pdf", "/ex5_33to35.pdf",
                "/ex5_36to40.pdf", "/ex5_41to44.pdf", "/ex5_45to50.pdf", "/ex5_51to56.pdf",
                "/ex5_57to62.pdf", "/ex5_63to66.pdf", "/ex5_67to74.pdf", "/ex5_75to76.pdf"
            }
        ),
        new SongConfig(
            "練習曲6 (Ex6)",
            "ex6.mid",
            new String[]{
                "/ex6_0to3.pdf", "/ex6_4to6.pdf", "/ex6_7to9.pdf", "/ex6_10.pdf",
                "/ex6_11to14.pdf", "/ex6_15to17.pdf", "/ex6_18to21.pdf", "/ex6_22to23.pdf",
                "/ex6_24to28.pdf", "/ex6_29to31.pdf", "/ex6_32to36.pdf", "/ex6_37to39.pdf",
                "/ex6_40to44.pdf", "/ex6_45to50.pdf", "/ex6_51to54.pdf", "/ex6_55to57.pdf"
            }
        )
    );

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

            midiRecorder.reset();
        }
    }

    private void loadSong(SongConfig song) {
        stopMusic();

        musicData = new MusicData(
                song.midiFile,
                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12);

        List<Color> colors = new ArrayList<>(Arrays.asList(
                new Color(255, 50, 80),    // ネオンレッド
                new Color(255, 120, 0),    // ネオンオレンジ
                new Color(255, 255, 0),    // ネオンイエロー
                new Color(50, 255, 150)    // ネオングリーン
        ));
        List<Channel> channels = new ArrayList<Channel>();
        try {
            Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEachOrdered(part -> {
                Channel channel = new Channel(
                        part.channel(),
                        part.prognum(),
                        "",
                        colors.get(part.channel() % colors.size()));
                channels.add(channel);

                // mute (controlled dynamically per turn)
                // part.addControlChange(0, 7, 0);
                // part.addControlChange(1, 7, 0);
            });
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        cmx.smfread(musicData.getScc());

        try {
            performanceData = new PerformanceData(musicData.getScc().toDataSet());
            rhythmJudge = new RhythmJudge(musicData.getScc().toDataSet());
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        dataModel = new PianoRollDataModelMultiChannel(
                2,
                2 + 12,
                4,
                channels,
                musicData.getScc());

        // PDFのスライスを読み込む
        loadMultiplePdfSlices(song.pdfFiles);


        // モジュール側の参照を新曲用にアップデート
        if (pianoTeacherModule != null) {
            try {
                pianoTeacherModule.updateSongData(musicData.getScc().toDataSet(), performanceData, rhythmJudge);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (receiver != null) {
            try {
                receiver.updateSccDataSet(musicData.getScc().toDataSet());
                receiver.updatePerformanceData(performanceData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 表示位置と状態を初期化
        cmx.setTickPosition(0);
        lastTickPosition = 0;
        noteIdx = 0;
        loopCount = 0;
        currentHideRate = 0.0;
        highlightCache.clear();
        setHighlightIndexes(highlightCache);
        loopJustReset = false;
        isDemoTurn = true;
        currentBlockIndex = 0;
        if (twoMeasureRepeatMode) {
            setPartVolume(100);
        } else {
            setPartVolume(0);
        }
        
        if (rhythmJudge != null) {
            rhythmJudge.reset();
        }
    }
}
