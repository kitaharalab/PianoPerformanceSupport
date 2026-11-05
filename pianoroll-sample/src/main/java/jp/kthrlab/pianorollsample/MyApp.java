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
import jp.crestmuse.cmx.filewrappers.SCC;
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
    int currentMode = 0; // 0: Learning Mode, 1: Rhythm Mode
    boolean rhythmMode = false;
    boolean twoMeasureRepeatMode = true; // 2小節交代モード（全モード共通）
    boolean isDemoTurn = true;           // true: お手本ターン, false: 演奏ターン
    int currentBlockIndex = 0;          // 現在の2小節ブロック番号
    String lastRhythmResult = "NONE";

    private HandMode currentHand = HandMode.RIGHT;
    private SongConfig currentSongConfig = null;

    // 左手・右手モードタブの配置座標情報 (クリック判定用)
    private float[] handTabX = new float[2];
    private float[] handTabW = new float[2];
    private float handTabY = 22;
    private float handTabH = 26;

    // モードタブの配置座標情報 (クリック判定用: Learning, Rhythm)
    private float[] tabX = new float[2];
    private float[] tabW = new float[2];
    private float tabY = 22;
    private float tabH = 26;

    private Transmitter midiTransmitter;

    CMXController cmx = CMXController.getInstance();

    List<Integer> highlightCache = new ArrayList<>();

    PImage[] pdfImage;
    PImage[] pdfImage2;
    int currentImageIndex = 0;
    long lastSwitchTime = 0;
    int switchIntervalMillis = 2000; // 2秒

    IMusicData musicData;
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

        // 最初の曲（右手デフォルト）を初期読み込み
        loadSong(RIGHT_SONGS.get(0));

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
        return true;
    }

    @Override
    protected boolean isMaskEnabled() {
        return true;
    }

    @Override
    public void draw() {
        super.draw();

        long tickPosition = cmx.getTickPosition();

        // Learning Mode & Rhythm Mode 共通: 2小節交代（お手本→演奏）＆周回ごとの目隠し暗記学習
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
                prepareDemoTurn(0, twoMeasureTicks);
                performanceData.setNotesToPlayForRange(0, twoMeasureTicks);
                if (rhythmJudge != null) {
                    rhythmJudge.reset();
                }
                cmx.setTickPosition(0);
                lastTickPosition = 0;
                tickPosition = 0;

                // 周回更新：小節の目隠し（暗記用マスク）を増加（Learning & Rhythm Mode）
                if (isMaskEnabled()) {
                    loopCount++;
                    currentHideRate = Math.min(1.0, loopCount * hideStep);
                    updateHighlightByRate(currentHideRate);
                }
            }

            if (currentBlockIndex >= totalBlocks) {
                currentBlockIndex = 0;
            }

            long blockStartTick = (long) currentBlockIndex * twoMeasureTicks;
            long blockEndTick = blockStartTick + twoMeasureTicks;

            if (isDemoTurn) {
                //  お手本ターン (2小節)  有音でお手本を自動再生
                if (!cmx.isNowPlaying()) {
                    cmx.playMusic();
                }

                // 次のブロックの1音目(blockEndTick)のonsetに届く前に、15 Tick手前の安全タイミングで先回り停止
                long safeStopTick = Math.max(blockStartTick, blockEndTick - 15);

                if (tickPosition >= safeStopTick) {
                    stopMusic();
                    silenceAllChannels();
                    performanceData.setNotesToPlayForRange(blockStartTick, blockEndTick);
                    if (rhythmJudge != null) {
                        rhythmJudge.resetForTick(blockStartTick);
                    }
                    cmx.setTickPosition(blockStartTick);
                    lastTickPosition = blockStartTick;
                    isDemoTurn = false; // 演奏ターンへ移行
                }
            } else {
                //  演奏ターン (2小節)  CMXは消音、未演奏ノートがあれば停止して打鍵待ち
                LongStream.rangeClosed(lastTickPosition, tickPosition).forEach(tick -> {
                    if (performanceData.hasNotesToPlay(tick)) {
                        stopMusic(); // 未演奏ノートがあれば停止して打鍵待ち
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
                        // 1周（全ブロック）完了！周回ごとの目隠し（暗記用マスク）を更新（Learning & Rhythm Mode）
                        if (isMaskEnabled()) {
                            loopCount++;
                            currentHideRate = Math.min(1.0, loopCount * hideStep);
                            updateHighlightByRate(currentHideRate);
                        }
                    }

                    long nextBlockStartTick = (long) currentBlockIndex * twoMeasureTicks;
                    long nextBlockEndTick = nextBlockStartTick + twoMeasureTicks;

                    // 次のお手本ターンへ移行：この2小節のみ有音化＆末尾で音量0カットしたSCCを準備
                    prepareDemoTurn(nextBlockStartTick, nextBlockEndTick);
                    performanceData.setNotesToPlayForRange(nextBlockStartTick, nextBlockEndTick);
                    // 1音目の発音抜けを完全防止するため、シーク位置を音符onsetの5 Tick手前にセット
                    long demoSeekTick = Math.max(0, nextBlockStartTick - (nextBlockStartTick > 0 ? 5 : 0));
                    cmx.setTickPosition(demoSeekTick);
                    lastTickPosition = nextBlockStartTick;
                    isDemoTurn = true; // 次のブロックのお手本ターンへ移行
                }
            }
        }
        lastTickPosition = tickPosition;

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
                        b = 255; // 青（正常・タイミング一致）
                        break;

                    case "EARLY":
                        r = 0;
                        g = 255;
                        b = 120; // 緑（速い）
                        break;

                    case "LATE":
                        r = 255;
                        g = 170;
                        b = 0; // 鮮やかなオレンジ・イエロー（遅い）
                        break;

                    case "WRONG_NOTE":
                        r = 255;
                        g = 20;
                        b = 30; // 鮮烈な赤（誤音階・ミス）
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

            if (!rhythmMode) {
                loopCount++;
                currentHideRate = Math.min(1.0, loopCount * hideStep);
                updateHighlightByRate(currentHideRate);
            }
        }

        // インタラクティブなモード切替タブバーの描画
        drawModeTabs();
    }

    private void drawModeTabs() {
        pushStyle();
        textSize(11.5f);
        textAlign(CENTER, CENTER);

        String[] handLabels = new String[]{"Right Hand", "Left Hand"};
        String[] tabLabels = new String[2];
        if (currentMode == 0) {
            tabLabels[0] = isDemoTurn ? "Learning (DEMO)" : "Learning (PLAY)";
        } else {
            tabLabels[0] = "Learning Mode";
        }
        if (currentMode == 1) {
            tabLabels[1] = isDemoTurn ? "Rhythm (DEMO)" : "Rhythm (PLAY)";
        } else {
            tabLabels[1] = "Rhythm Mode";
        }

        float paddingX = 10;
        float spacing = 8;
        float groupGap = 18;

        for (int i = 0; i < 2; i++) {
            handTabW[i] = textWidth(handLabels[i]) + paddingX * 2;
        }
        for (int i = 0; i < 2; i++) {
            tabW[i] = textWidth(tabLabels[i]) + paddingX * 2;
        }

        // 画面右上から左に向かって配置: [Right Hand] [Left Hand] | [Learning] [Rhythm]
        float rightMargin = 20;
        float currentX = width - rightMargin;

        // Mode タブ (右側グループ: 1(Rhythm), 0(Learning))
        for (int i = 1; i >= 0; i--) {
            currentX -= tabW[i];
            tabX[i] = currentX;
            currentX -= spacing;
        }

        // グループ間の間隔
        currentX -= (groupGap - spacing);

        // Hand タブ (左側グループ: 1(Left), 0(Right))
        for (int i = 1; i >= 0; i--) {
            currentX -= handTabW[i];
            handTabX[i] = currentX;
            currentX -= spacing;
        }

        // HandグループとModeグループの間の縦セパレーター線
        float dividerX = handTabX[1] + handTabW[1] + groupGap / 2f;
        stroke(60, 75, 100, 150);
        strokeWeight(1.2f);
        line(dividerX, tabY + 4, dividerX, tabY + tabH - 4);

        // === Hand タブの描画 ===
        for (int i = 0; i < 2; i++) {
            boolean isActive = (i == 0 && currentHand == HandMode.RIGHT) || (i == 1 && currentHand == HandMode.LEFT);
            int strokeR, strokeG, strokeB;
            int textR, textG, textB;

            if (i == 0) {
                // 右手: ネオンレッド/コーラル
                strokeR = 255; strokeG = 75; strokeB = 95;
                textR = 255; textG = 120; textB = 140;
            } else {
                // 左手: ネオンシアン/ブルー
                strokeR = 0; strokeG = 210; strokeB = 255;
                textR = 80; textG = 225; textB = 255;
            }

            boolean isHover = mouseX >= handTabX[i] && mouseX <= handTabX[i] + handTabW[i] &&
                              mouseY >= handTabY && mouseY <= handTabY + handTabH;

            fill(14, 18, 26, isActive ? 245 : (isHover ? 200 : 160));
            stroke(strokeR, strokeG, strokeB, isActive ? 255 : (isHover ? 180 : 120));
            strokeWeight(isActive ? 1.8f : 1.0f);
            rect(handTabX[i], handTabY, handTabW[i], handTabH, 4);

            if (isActive) {
                noStroke();
                fill(strokeR, strokeG, strokeB, 240);
                rect(handTabX[i] + 3, handTabY + handTabH - 2.5f, handTabW[i] - 6, 2.5f, 1);
            }

            noStroke();
            fill(isActive ? textR : (isHover ? 240 : 180),
                 isActive ? textG : (isHover ? 245 : 190),
                 isActive ? textB : (isHover ? 255 : 205));
            text(handLabels[i], handTabX[i] + handTabW[i] / 2f, handTabY + handTabH / 2f - (isActive ? 1.5f : 1f));
        }

        // === Mode タブの描画 ===
        for (int i = 0; i < 2; i++) {
            boolean isActive = (i == currentMode);
            int strokeR = 75, strokeG = 90, strokeB = 115;
            int textR = 170, textG = 180, textB = 200;

            if (i == 0 && isActive) {
                if (isDemoTurn) {
                    strokeR = 255; strokeG = 180; strokeB = 50; // ネオンオレンジ（お手本）
                    textR = 255; textG = 190; textB = 70;
                } else {
                    strokeR = 50; strokeG = 220; strokeB = 120; // ネオングリーン（演奏）
                    textR = 80; textG = 235; textB = 140;
                }
            } else if (i == 1 && isActive) {
                if (isDemoTurn) {
                    strokeR = 255; strokeG = 180; strokeB = 50; // ネオンオレンジ（お手本）
                    textR = 255; textG = 190; textB = 70;
                } else {
                    strokeR = 180; strokeG = 80; strokeB = 255; // ネオンパープル/バイオレット（Rhythm演奏）
                    textR = 210; textG = 130; textB = 255;
                }
            }

            boolean isHover = mouseX >= tabX[i] && mouseX <= tabX[i] + tabW[i] &&
                              mouseY >= tabY && mouseY <= tabY + tabH;

            // 各タブの独立した境界線と背景
            fill(14, 18, 26, isActive ? 245 : (isHover ? 200 : 160));
            stroke(strokeR, strokeG, strokeB, isActive ? 255 : (isHover ? 200 : 140));
            strokeWeight(isActive ? 1.8f : 1.0f);
            rect(tabX[i], tabY, tabW[i], tabH, 4);

            // アクティブタブの下部発光インジケーターバー
            if (isActive) {
                noStroke();
                fill(strokeR, strokeG, strokeB, 240);
                rect(tabX[i] + 3, tabY + tabH - 2.5f, tabW[i] - 6, 2.5f, 1);
            }

            // テキスト描画
            noStroke();
            fill(isActive ? textR : (isHover ? 240 : textR),
                 isActive ? textG : (isHover ? 245 : textG),
                 isActive ? textB : (isHover ? 255 : textB));
            text(tabLabels[i], tabX[i] + tabW[i] / 2f, tabY + tabH / 2f - (isActive ? 1.5f : 1f));
        }
        popStyle();
    }

    @Override
    public void mousePressed() {
        super.mousePressed();
        // 手切り替えタブのクリック判定
        for (int i = 0; i < 2; i++) {
            if (mouseX >= handTabX[i] && mouseX <= handTabX[i] + handTabW[i] &&
                mouseY >= handTabY && mouseY <= handTabY + handTabH) {
                setHand(i == 0 ? HandMode.RIGHT : HandMode.LEFT);
                return;
            }
        }

        // モードタブのクリック判定 (0: Learning, 1: Rhythm)
        for (int i = 0; i < 2; i++) {
            if (mouseX >= tabX[i] && mouseX <= tabX[i] + tabW[i] &&
                mouseY >= tabY && mouseY <= tabY + tabH) {
                setAppMode(i);
                break;
            }
        }
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
        return false;
    }

    private void silenceAllChannels() {
        if (musicData != null && musicData.getScc() != null) {
            cmx.smfread(musicData.getScc());
        }
    }

    private void prepareDemoTurn(long blockStartTick, long blockEndTick) {
        if (musicData != null) {
            SCC demoScc = musicData.getSccForDemoBlock(blockStartTick, blockEndTick);
            if (demoScc != null) {
                cmx.smfread(demoScc);
            }
        }
    }

    void setAppMode(int modeIndex) {
        stopMusic();
        silenceAllChannels();
        loopCount = 0;
        currentHideRate = 0.0;
        highlightCache.clear();
        setHighlightIndexes(highlightCache);

        currentMode = modeIndex;
        rhythmMode = (modeIndex == 1);
        twoMeasureRepeatMode = true;
        isDemoTurn = true;
        currentBlockIndex = 0;

        if (dataModel != null) {
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
            int division = dataModelMultiChannel.getScc().getDivision();
            int beatNum = dataModelMultiChannel.getBeatNum();
            int twoMeasureTicks = beatNum * division * 2;
            prepareDemoTurn(0, twoMeasureTicks);
            if (performanceData != null) {
                performanceData.setNotesToPlayForRange(0, twoMeasureTicks);
            }
        }

        cmx.setTickPosition(0);
        lastTickPosition = 0;
        if (rhythmJudge != null) {
            rhythmJudge.reset();
        }
    }

    @Override
    public void drawCurrentNote() {
        super.drawCurrentNote();

        // お手本ターン中は、今鳴っているお手本の鍵盤位置を鮮やかなネオンブルーで光らせる
        if (isDemoTurn && dataModel != null) {
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

        int hr = (currentHand == HandMode.LEFT) ? 0 : 255;
        int hg = (currentHand == HandMode.LEFT) ? 210 : 150;
        int hb = (currentHand == HandMode.LEFT) ? 255 : 50;

        pushStyle();
        noStroke();
        for (int i = 0; i < 5; i++) {
            fill(hr, hg, hb, 45 - i * 9);
            rect(x - i, y - i, w + i * 2, h + i * 2, 3);
        }
        fill(hr, hg, hb, 210);
        rect(x, y, w, h, 2);
        fill(Math.min(255, hr + 80), Math.min(255, hg + 60), Math.min(255, hb + 60));
        rect(x + 1, y, w - 2, 4);
        popStyle();
    }

    void createMenu() {
        JFrame frame = (JFrame) ((processing.awt.PSurfaceAWT.SmoothCanvas) getSurface().getNative()).getFrame();

        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();

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

        // 曲切り替え用メニューを追加（現在の手に応じたリストを表示）
        String songsMenuLabel = (currentHand == HandMode.RIGHT) ? "Songs (Right Hand)" : "Songs (Left Hand)";
        javax.swing.JMenu songsMenu = new javax.swing.JMenu(songsMenuLabel);
        List<SongConfig> currentSongs = (currentHand == HandMode.RIGHT) ? RIGHT_SONGS : LEFT_SONGS;
        for (SongConfig song : currentSongs) {
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

    public void setHand(HandMode hand) {
        if (this.currentHand == hand) return;
        stopMusic();
        silenceAllChannels();
        this.currentHand = hand;
        setHandMode(hand);

        if (hand == HandMode.LEFT) {
            loadSong(LEFT_SONGS.get(0));
        } else {
            loadSong(RIGHT_SONGS.get(0));
        }
        createMenu();
    }

    public static class SongConfig {
        public final String name;
        public final String midiFile;
        public final String[] pdfFiles;
        public final HandMode hand;
        public final int beatNum;
        public final int beatDenom;

        public SongConfig(String name, String midiFile, String[] pdfFiles, HandMode hand, int beatNum, int beatDenom) {
            this.name = name;
            this.midiFile = midiFile;
            this.pdfFiles = pdfFiles;
            this.hand = hand;
            this.beatNum = beatNum;
            this.beatDenom = beatDenom;
        }

        public SongConfig(String name, String midiFile, String[] pdfFiles, HandMode hand) {
            int[] ts = detectTimeSignature(midiFile);
            this.name = name;
            this.midiFile = midiFile;
            this.pdfFiles = pdfFiles;
            this.hand = hand;
            this.beatNum = ts[0];
            this.beatDenom = ts[1];
        }

        public SongConfig(String name, String midiFile, String[] pdfFiles) {
            this(name, midiFile, pdfFiles, HandMode.RIGHT);
        }
    }

    /**
     * MIDIファイルから拍子（分子・分母）を自動検出する
     */
    public static int[] detectTimeSignature(String midiResource) {
        try (var in = MyApp.class.getClassLoader().getResourceAsStream(midiResource)) {
            if (in != null) {
                var seq = javax.sound.midi.MidiSystem.getSequence(in);
                for (var track : seq.getTracks()) {
                    for (int i = 0; i < track.size(); i++) {
                        var ev = track.get(i);
                        if (ev.getMessage() instanceof javax.sound.midi.MetaMessage mm) {
                            if (mm.getType() == 0x58) {
                                byte[] d = mm.getData();
                                int num = d[0] & 0xFF;
                                int den = 1 << (d[1] & 0xFF);
                                if (num > 1 && den > 0) {
                                    return new int[]{num, den};
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return new int[]{4, 4};
    }

    private static final List<SongConfig> RIGHT_SONGS = List.of(
        new SongConfig(
            "きらきら星 (Kirakira)",
            "kirakira2.mid",
            new String[]{
                "/kirakira_0to3.pdf",
                "/kirakira_4to6.pdf",
                "/kirakira_7to10.pdf",
                "/kirakira_11to13.pdf"
            },
            HandMode.RIGHT,
            4, 4
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
            },
            HandMode.RIGHT,
            4, 4
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
            },
            HandMode.RIGHT,
            4, 4
        ),
        new SongConfig(
            "練習曲3 (Ex3)",
            "ex3.mid",
            new String[]{
                "/ex3_0to2.pdf", "/ex3_3to5.pdf", "/ex3_6to11.pdf", "/ex3_12.pdf",
                "/ex3_13to15.pdf", "/ex3_16to18.pdf", "/ex3_19to24.pdf", "/ex3_25to26.pdf",
                "/ex3_27to32.pdf", "/ex3_33to38.pdf", "/ex3_39to44.pdf", "/ex3_45.pdf",
                "/ex3_46to48.pdf", "/ex3_49to51.pdf", "/ex3_52to57.pdf", "/ex3_58.pdf"
            },
            HandMode.RIGHT,
            3, 4
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
            },
            HandMode.RIGHT,
            3, 4
        ),
        new SongConfig(
            "練習曲5 (Ex5)",
            "ex5.mid",
            new String[]{
                "/ex5_0to2.pdf", "/ex5_3to7.pdf", "/ex5_8to13.pdf", "/ex5_14to15.pdf",
                "/ex5_16to20.pdf", "/ex5_21to27.pdf", "/ex5_28to32.pdf", "/ex5_33to35.pdf",
                "/ex5_36to40.pdf", "/ex5_41to44.pdf", "/ex5_45to50.pdf", "/ex5_51to56.pdf",
                "/ex5_57to62.pdf", "/ex5_63to66.pdf", "/ex5_67to74.pdf", "/ex5_75to76.pdf"
            },
            HandMode.RIGHT,
            4, 4
        ),
        new SongConfig(
            "練習曲6 (Ex6)",
            "ex6.mid",
            new String[]{
                "/ex6_0to3.pdf", "/ex6_4to6.pdf", "/ex6_7to9.pdf", "/ex6_10.pdf",
                "/ex6_11to14.pdf", "/ex6_15to17.pdf", "/ex6_18to21.pdf", "/ex6_22to23.pdf",
                "/ex6_24to28.pdf", "/ex6_29to31.pdf", "/ex6_32to36.pdf", "/ex6_37to39.pdf",
                "/ex6_40to44.pdf", "/ex6_45to50.pdf", "/ex6_51to54.pdf", "/ex6_55to57.pdf"
            },
            HandMode.RIGHT,
            3, 4
        )
    );

    private static final List<SongConfig> LEFT_SONGS = List.of(
        new SongConfig(
            "きらきら星 (Kirakira)",
            "kirakira_L.mid",
            new String[0],
            HandMode.LEFT,
            4, 4
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

        if (key == 's' || key == 'S') {
            stopMusic();
            String filename = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\midi\\" +
                    "subject" + subjectId + "_take" + takeCount + ".mid";

            midiRecorder.save(filename);
            takeCount++;

            midiRecorder.reset();
        }

        if (key == 'h' || key == 'H') {
            setHand(currentHand == HandMode.RIGHT ? HandMode.LEFT : HandMode.RIGHT);
        }
    }

    private void loadSong(SongConfig song) {
        stopMusic();
        this.currentSongConfig = song;
        this.currentHand = song.hand;
        setHandMode(song.hand);
        setBeatDenom(song.beatDenom);

        musicData = new MusicData(
                song.midiFile,
                timeline.getSpan().intValue(),
                0,
                song.beatNum,
                48,
                1,
                12);

        // カラーバーの色（左手と右手で色を変更する）
        List<Color> colors;
        if (song.hand == HandMode.LEFT) {
            // 左手用カラーパレット (ネオンシアン, ネオンブルー, ネオンパープル, ネオンマゼンタ)
            colors = new ArrayList<>(Arrays.asList(
                    new Color(0, 210, 255),    // ネオンシアン
                    new Color(64, 140, 255),   // ネオンブルー
                    new Color(180, 80, 255),   // ネオンパープル
                    new Color(255, 60, 200)    // ネオンマゼンタ
            ));
        } else {
            // 右手用カラーパレット (ネオンレッド, ネオンオレンジ, ネオンイエロー, ネオングリーン)
            colors = new ArrayList<>(Arrays.asList(
                    new Color(255, 50, 80),    // ネオンレッド
                    new Color(255, 120, 0),    // ネオンオレンジ
                    new Color(255, 255, 0),    // ネオンイエロー
                    new Color(50, 255, 150)    // ネオングリーン
            ));
        }
        List<Channel> channels = new ArrayList<Channel>();
        try {
            Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEachOrdered(part -> {
                Channel channel = new Channel(
                        part.channel(),
                        part.prognum(),
                        "",
                        colors.get(part.channel() % colors.size()));
                channels.add(channel);

                // Mute CMX SMF player by default so timeline advancement never leaks sound in any mode
                part.addControlChange(0, 7, 0);
                part.addControlChange(1, 7, 0);
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
                song.beatNum,
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

        twoMeasureRepeatMode = true;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        if (dataModelMultiChannel != null) {
            int division = dataModelMultiChannel.getScc().getDivision();
            int beatNum = dataModelMultiChannel.getBeatNum();
            int twoMeasureTicks = beatNum * division * 2;
            prepareDemoTurn(0, twoMeasureTicks);
            if (performanceData != null) {
                performanceData.setNotesToPlayForRange(0, twoMeasureTicks);
            }
        }
        
        if (rhythmJudge != null) {
            rhythmJudge.reset();
        }
    }
}
