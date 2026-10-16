package jp.kthrlab.pianorollsample;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import jp.kthrlab.pianoroll.Keyboard;
import jp.kthrlab.pianorollsample.ImageNotePianoRoll.PdfDisplay;
import processing.core.PImage;

public class ImageNotePianoRoll extends HorizontalPAppletCmxPianoRoll {
    List<MutableNote> imageNotes = new java.util.ArrayList<>();
    protected List<PdfRange> pdfRanges = new ArrayList<>();
    protected PerformanceData performanceData;

    private final List<PdfDisplay> pdfToDrawFront = new ArrayList<>();
    private final List<Float> pdfYFront = new ArrayList<>();

    public float getPdfLeftX() {
        return 0f;
    }

    public float getPdfRightX() {
        return 480f;
    }

    public enum HandMode {
        RIGHT,
        LEFT
    }

    protected HandMode handMode = HandMode.RIGHT;

    public HandMode getHandMode() {
        return handMode;
    }

    public void setHandMode(HandMode handMode) {
        this.handMode = handMode;
    }

    public boolean isLeftHand() {
        return handMode == HandMode.LEFT;
    }

    /** 楽曲全体の最低音・最高音の音高(MIDI notenum)を取得する */
    public int[] getMidiPitchRange() {
        if (dataModel == null) return new int[]{60, 72};
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        int minNn = 127;
        int maxNn = 0;
        try {
            for (var part : dataModelMultiChannel.getScc().getPartList()) {
                if (part == null || part.getNoteOnlyList() == null) continue;
                for (var note : part.getNoteOnlyList()) {
                    if (note.notenum() < minNn) minNn = note.notenum();
                    if (note.notenum() > maxNn) maxNn = note.notenum();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (minNn > maxNn) {
            minNn = 60;
            maxNn = 72;
        }
        return new int[]{minNn, maxNn};
    }

    /** 楽曲の音域を検出し、カラーバーと重ならない最適な位置に楽譜を表示するための左端X座標を返す */
    public float getDynamicScoreLeftX() {
        if (dataModel == null || keyboard == null || timeline == null) return 10f;
        int[] range = getMidiPitchRange();
        int minNn = range[0];
        int maxNn = range[1];

        float semitoneW = timeline.getSemitoneWidth();
        float offset = keyboard.getOffset();
        float minNnX = (minNn - keyboard.getFirstNn()) * semitoneW + offset;
        float maxNnX = (maxNn - keyboard.getFirstNn() + 1) * semitoneW + offset;

        if (isLeftHand()) {
            // 左手モード：理想の配置として画面左側（X=10px）に楽譜を配置
            // もし超低音の曲で左側の空き幅が240px未満になる場合は、カラーバーの右側に自動配置
            if (minNnX >= 250f) {
                return 10f;
            } else {
                return Math.min(width - 490f, maxNnX + 15f);
            }
        } else {
            // 右手モード：カラーバーは右側に表示されるため、楽譜はその左横（最低音の左側）に配置する
            float rightX = minNnX - 15f;
            float leftX = rightX - 480f;
            if (leftX >= 10f) {
                return leftX;
            } else if (maxNnX + 490f <= width) {
                return maxNnX + 15f; // 左側が狭い場合は右側に退避
            } else {
                return Math.max(0f, leftX);
            }
        }
    }

    public float getDynamicScoreRightX() {
        if (dataModel == null || keyboard == null || timeline == null) return 490f;
        int[] range = getMidiPitchRange();
        int minNn = range[0];
        int maxNn = range[1];

        float semitoneW = timeline.getSemitoneWidth();
        float offset = keyboard.getOffset();
        float minNnX = (minNn - keyboard.getFirstNn()) * semitoneW + offset;

        float leftX = getDynamicScoreLeftX();
        if (isLeftHand()) {
            if (minNnX >= 250f) {
                // 左側に配置：最低音のカラーバーに重ならないギリギリまで楽譜の横幅を最大確保
                float maxRight = minNnX - 12f;
                return Math.max(leftX + 240f, Math.min(leftX + 480f, maxRight));
            } else {
                // 右側に配置した場合
                return Math.min(width - 10f, leftX + 480f);
            }
        } else {
            if (leftX >= minNnX) {
                // 右側に退避した場合
                return Math.min(width - 10f, leftX + 480f);
            } else {
                return Math.min(minNnX - 12f, leftX + 480f);
            }
        }
    }

    @Override
    public void draw() {
        background(255);

        // 背景のタイムラインとカラーバーの描画
        drawTimeline();
        drawMidiNotesOnly();
        
        // プレイヘッドと鍵盤の描画
        drawPlayhead();
        drawKeyboard();
        drawCurrentNote();

        // 五線譜・音符を描画
        drawMidiScoreAndMaskOnly();
    }

    @Override
    public void drawTimeline() {
        pushStyle();
        noStroke();
        int numKeys = keyboard.getNumKeys();
        float semitoneW = timeline.getSemitoneWidth();
        float offset = keyboard.getOffset();
        float h = height - keyboard.getWhiteKeyHeight();

        for (int i = 0; i < numKeys; i++) {
            float x = i * semitoneW + offset;
            int notenum = keyboard.getFirstNn() + i;

            if (Keyboard.isWhiteKey(notenum)) {
                fill(253, 253, 254); // 白鍵
            } else {
                fill(240, 242, 246); // 黒鍵
            }
            rect(x, 0, semitoneW, h);

            // レーン同士の境界線
            stroke(225, 227, 232, 120);
            strokeWeight(0.5f);
            line(x, 0, x, h);
            noStroke();
        }
        popStyle();
    }

    /** 複数PDF × 各PDF縦10分割 */
    private PImage[][] pdfSlicesList;

    /**
     * 複数PDFを読み込み、それぞれ縦10分割する
     */
    public void loadMultiplePdfSlices(String[] pdfPaths) {
        pdfSlicesList = new PImage[pdfPaths.length][25];

        for (int i = 0; i < pdfPaths.length; i++) {
            pdfSlicesList[i] = loadPdfSlicesVertical(pdfPaths[i]);
        }
    }

    /**
     * PDF を読み込み、縦方向 10 分割した PImage[] を返す
     */
    private PImage[] loadPdfSlicesVertical(String pdfResource) {
        PImage[] slices = new PImage[25];

        try (var in = getClass().getResourceAsStream(pdfResource)) {
            if (in == null) {
                return slices;
            }

            try (PDDocument doc = PDDocument.load(in)) {
                PDFRenderer renderer = new PDFRenderer(doc);

                float dpi = 150f;
                BufferedImage bi = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);

                int w = bi.getWidth();
                int h = bi.getHeight();

                int sliceHeight = h / 10; //分割数

                for (int i = 0; i < 10; i++) {
                    int y = i * sliceHeight;
                    int actualHeight = (i == 9) ? (h - y) : sliceHeight;

                    BufferedImage sub = bi.getSubimage(0, y, w, actualHeight);

                    PImage pimg = createImage(w, actualHeight, RGB);
                    pimg.loadPixels();
                    int[] buf = sub.getRGB(0, 0, w, actualHeight, null, 0, w);
                    System.arraycopy(buf, 0, pimg.pixels, 0, buf.length);
                    pimg.updatePixels();

                    slices[i] = pimg;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return slices;
    }

    // MIDI音高と臨時記号（シャープ・フラット）および五線上のステップ情報
    public static class NoteAccidental {
        public final int step;
        public final String symbol; // "SHARP", "FLAT", or ""

        public NoteAccidental(int step, String symbol) {
            this.step = step;
            this.symbol = symbol;
        }
    }

    /**
     * MIDI音高から五線上の位置（ダイアトニックステップ）と臨時記号（♯ / ♭）を求める。
     * C4(60) = 35基準。
     * 西洋音楽理論に基づき、F#, C#はシャープ(♯)、Bb, Eb, Abはフラット(♭)として記譜。
     */
    public NoteAccidental getNoteAccidental(int notenum) {
        int octave = notenum / 12;
        int noteInOctave = notenum % 12;
        int stepInOctave;
        String symbol = "";

        switch (noteInOctave) {
            case 0: stepInOctave = 0; break;                    // C
            case 1: stepInOctave = 0; symbol = "SHARP"; break;  // C♯
            case 2: stepInOctave = 1; break;                    // D
            case 3: stepInOctave = 2; symbol = "FLAT"; break;   // E♭
            case 4: stepInOctave = 2; break;                    // E
            case 5: stepInOctave = 3; break;                    // F
            case 6: stepInOctave = 3; symbol = "SHARP"; break;  // F♯
            case 7: stepInOctave = 4; break;                    // G
            case 8: stepInOctave = 5; symbol = "FLAT"; break;   // A♭
            case 9: stepInOctave = 5; break;                    // A
            case 10: stepInOctave = 6; symbol = "FLAT"; break;  // B♭
            case 11: stepInOctave = 6; break;                   // B
            default: stepInOctave = 0; break;
        }
        return new NoteAccidental(octave * 7 + stepInOctave, symbol);
    }

    // 後方互換用
    private int getDiatonicStep(int notenum) {
        return getNoteAccidental(notenum).step;
    }

    /**
     * 楽譜用のシャープ記号（♯）をベクターで正確に描画する
     * （フォントや文字コードに依存しないため、どの環境でも文字化けせず美しく描画される）
     */
    private void drawSharp(float x, float y) {
        pushStyle();
        stroke(35);
        strokeCap(processing.core.PConstants.SQUARE);

        // 縦線2本（少し斜め）
        strokeWeight(1.2f);
        line(x - 2.8f, y - 10, x - 2.8f, y + 8);
        line(x + 2.8f, y - 8, x + 2.8f, y + 10);

        // 横線2本（右上がりに傾斜、太めの線）
        strokeWeight(2.4f);
        line(x - 6.0f, y - 1.5f, x + 6.0f, y - 4.5f);
        line(x - 6.0f, y + 4.5f, x + 6.0f, y + 1.5f);

        popStyle();
    }

    /**
     * 楽譜用のフラット記号（♭）をベクターで正確に描画する
     * （フォントや文字コードに依存しないため、どの環境でも文字化けせず美しく描画される）
     */
    private void drawFlat(float x, float y) {
        pushStyle();
        stroke(35);

        // 上に伸びる縦棒（ステム）
        strokeWeight(1.4f);
        strokeCap(processing.core.PConstants.ROUND);
        line(x - 2.5f, y - 12, x - 2.5f, y + 4);

        // 右側のふくらみ（ループ）
        strokeWeight(1.8f);
        noFill();
        bezier(x - 2.5f, y - 2,
               x + 5.5f, y - 2,
               x + 5.5f, y + 4,
               x - 2.5f, y + 4);

        // ループの内側を少し塗りつぶして自然な太さを出す
        fill(35);
        noStroke();
        triangle(x - 2.5f, y, x + 2.5f, y + 2, x - 2.5f, y + 4);

        popStyle();
    }

    /**
     *  指定されたノートから、同じパート内で次に onset が異なるノートまでのtickを求める。
     */
    private long getStepTicks(MutableNote note, MutableNote[] allNotes) {
        long currentOnset = note.onset();
        long nextOnset = -1;
        for (MutableNote other : allNotes) {
            if (other.onset() > currentOnset) {
                if (nextOnset == -1 || other.onset() < nextOnset) {
                    nextOnset = other.onset();
                }
            }
        }
        if (nextOnset != -1) {
            return nextOnset - currentOnset;
        } else {
            // 次の音符がない場合は、offset - onsetを使用するが、最低でも480
            return Math.max(note.offset() - note.onset(), 480);
        }
    }

    /**
     * MIDIデータから指定した小節の楽譜をリアルタイムに自動描画する。
     */
    private void drawMidiMeasureScore(int measureIdx, float absoluteY) {
        if (dataModel == null) return;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        float startX = getDynamicScoreLeftX() + 15;
        float endX = getDynamicScoreRightX() - 15;
        float scoreW = endX - startX;
        float scoreH = 100;
        float startY = absoluteY - scoreH;

        float trebleCenterY = startY + 25; // ト音記号（右手）の中心（第3線: B4=MIDI 71）
        float bassCenterY = startY + 75;   // ヘ音記号（左手）の中心（第3線: D3=MIDI 50）

        pushStyle();
        // 五線譜の描画
        stroke(100, 100, 110, 140);
        strokeWeight(0.8f);
        for (int i = -2; i <= 2; i++) {
            // ト音譜表 (Treble Clef)
            line(startX, trebleCenterY + i * 5, endX, trebleCenterY + i * 5);
            // ヘ音譜表 (Bass Clef)
            line(startX, bassCenterY + i * 5, endX, bassCenterY + i * 5);
        }

        // 小節線 (左端と右端)
        stroke(60, 60, 70, 200);
        strokeWeight(1.5f);
        line(startX, trebleCenterY - 10, startX, bassCenterY + 10);
        line(endX, trebleCenterY - 10, endX, bassCenterY + 10);



        // 小節に含まれるノートの収集と描画
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;

        // 小節の開始ティックを算出
        int startNoteIdx = 0;
        if (pdfRanges != null && measureIdx < pdfRanges.size()) {
            startNoteIdx = pdfRanges.get(measureIdx).startNoteIdx;
        }

        long tempOnset = 0;
        int currentIdx = 0;
        boolean found = false;
        for (int chIdx = 0; chIdx < dataModelMultiChannel.getChannels().size(); chIdx++) {
            var part = dataModelMultiChannel.getPart(chIdx);
            if (part != null && part.getNoteOnlyList() != null) {
                for (var note : part.getNoteOnlyList()) {
                    if (currentIdx == startNoteIdx) {
                        tempOnset = note.onset();
                        found = true;
                        break;
                    }
                    currentIdx++;
                }
            }
            if (found) break;
        }

        long T_start = (tempOnset / measureTicks) * measureTicks;

        // この小節内のノートを描画
        for (int chIdx = 0; chIdx < dataModelMultiChannel.getChannels().size(); chIdx++) {
            var part = dataModelMultiChannel.getPart(chIdx);
            if (part == null || part.getNoteOnlyList() == null) continue;

            for (var note : part.getNoteOnlyList()) {
                if (note.onset() >= T_start && note.onset() < T_start + measureTicks) {
                    double relOnset = note.onset() - T_start;
                    float noteX = startX + 35 + (float) (relOnset / measureTicks) * (scoreW - 55);

                    int notenum = note.notenum();
                    boolean isTreble = notenum >= 60; // 中央C以上はト音

                    // Y座標算出
                    int step = getDiatonicStep(notenum);
                    float noteY;
                    if (isTreble) {
                        int diff = step - 41; // B4(step 41)基準
                        noteY = trebleCenterY - diff * 2.5f;
                    } else {
                        int diff = step - 29; // D3(step 29)基準
                        noteY = bassCenterY - diff * 2.5f;
                    }

                    // 臨時記号(シャープ)
                    boolean isSharp = !Keyboard.isWhiteKey(notenum);
                    if (isSharp) {
                        fill(30);
                        textSize(11);
                        textAlign(RIGHT, CENTER);
                        text("#", noteX - 6, noteY - 1);
                    }

                    // 線の描画
                    stroke(80);
                    strokeWeight(1.0f);
                    if (isTreble) {
                        if (step <= 35) { // 中央C(35)以下
                            for (int s = 35; s >= step; s -= 2) {
                                float ly = trebleCenterY - (s - 41) * 2.5f;
                                line(noteX - 6, ly, noteX + 6, ly);
                            }
                        } else if (step >= 47) { // A5(47)以上
                            for (int s = 47; s <= step; s += 2) {
                                float ly = trebleCenterY - (s - 41) * 2.5f;
                                line(noteX - 6, ly, noteX + 6, ly);
                            }
                        }
                    } else {
                        if (step <= 23) { // E2(23)以下
                            for (int s = 23; s >= step; s -= 2) {
                                float ly = bassCenterY - (s - 29) * 2.5f;
                                line(noteX - 6, ly, noteX + 6, ly);
                            }
                        } else if (step >= 35) { // 中央C(35)以上
                            for (int s = 35; s <= step; s += 2) {
                                float ly = bassCenterY - (s - 29) * 2.5f;
                                line(noteX - 6, ly, noteX + 6, ly);
                            }
                        }
                    }

                    // 音符の長さ（音価）の判定
                    long stepTicks = getStepTicks(note, part.getNoteOnlyList());
                    double duration = (double) stepTicks / division;

                    stroke(30);
                    fill(30);

                    if (duration >= 3.0) {
                        // 全音符: 白抜き楕円、棒なし
                        strokeWeight(1.5f);
                        fill(255);
                        ellipse(noteX, noteY, 8, 6);
                    } else if (duration >= 1.5) {
                        // 2分音符: 白抜き楕円 + 棒
                        strokeWeight(1.5f);
                        fill(255);
                        ellipse(noteX, noteY, 8, 6);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                    } else if (duration >= 0.75) {
                        // 4分音符: 黒塗り楕円 + 棒
                        noStroke();
                        ellipse(noteX, noteY, 8, 6);
                        stroke(30);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                    } else {
                        // 8分音符以下: 黒塗り楕円 + 棒 + 旗
                        noStroke();
                        ellipse(noteX, noteY, 8, 6);
                        stroke(30);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                        line(noteX + 4, noteY - 15, noteX + 8, noteY - 10);
                    }
                }
            }
        }
        popStyle();
    }

    public void drawSliceAtY(int pdfIndex, int sliceIndex, float absoluteY, float cropXratio) {
        // PDF画像の表示を、自作のMIDI自動楽譜描画に差し替え
        drawMidiMeasureScore(pdfIndex, absoluteY);
    }

    public static class PdfDisplay {
        public int pdfIndex;
        public int sliceIndex;

        public PdfDisplay(int pdfIndex, int sliceIndex) {
            this.pdfIndex = pdfIndex;
            this.sliceIndex = sliceIndex;
        }
    }

    public java.util.function.Function<Integer, PdfDisplay> pdfRule = null;

    public void setPdfDisplayRule(java.util.function.Function<Integer, PdfDisplay> rule) {
        this.pdfRule = rule;
    }

    private List<Integer> highlightIndexes = new java.util.ArrayList<>();

    public void setHighlightIndexes(List<Integer> indexes) {
        this.highlightIndexes = indexes;
    }

    public void addImageNote(MutableNote note) {
        this.imageNotes.add(note);
    }

    public void setImageNotes(List<MutableNote> imageNotes) {
        this.imageNotes = imageNotes;
    }

    protected void drawPdfForNoteIndex(int noteIndex) {
        if (pdfRule == null)
            return;

        PdfDisplay disp = pdfRule.apply(noteIndex);
        if (disp == null)
            return;

        int p = disp.pdfIndex;
        int s = disp.sliceIndex;

        if (p < 0 || p >= pdfSlicesList.length)
            return;
        if (s < 0 || s >= pdfSlicesList[p].length)
            return;

        PImage img = pdfSlicesList[p][s];

        float maxW = width * 0.33f;
        float maxH = height * 0.45f;

        float scale = Math.min(maxW / img.width, maxH / img.height);
        float drawW = img.width * scale;
        float drawH = img.height * scale;

        image(img, 0, 0, drawW, drawH);
    }

    private void drawPdfFront() {
        // 何もしない
    }

    void drawPdfGuideLines() {
        // 何もしない
    }

    @Override
    public void drawNote() {
        // 何もしない
    }

    protected boolean isLearningMode() {
        return true;
    }

    protected int getTotalMeasures() {
        if (dataModel == null) return 0;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;
        
        final long[] maxTick = {0};
        dataModelMultiChannel.getChannels().forEach(channel -> {
            var part = dataModelMultiChannel.getPart(channel.channel_number);
            if (part != null && part.getNoteOnlyList() != null) {
                for (MutableNote note : part.getNoteOnlyList()) {
                    if (note.offset() > maxTick[0]) {
                        maxTick[0] = note.offset();
                    }
                }
            }
        });
        return (int) Math.ceil((double) maxTick[0] / measureTicks);
    }

    /**
     * 指定された小節が非表示の対象かを判定する。
     * その小節のノート範囲にhighlightIndexesの対象が含まれていればマスクする。
     */
    private boolean isMeasureHighlighted(int measureIdx) {
        if (!isLearningMode()) return false; // ラーニングモード以外は目隠ししない
        if (dataModel == null) return false;

        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;

        if (dataModelMultiChannel.getChannels().isEmpty()) return false;
        int mainChannelNum = dataModelMultiChannel.getChannels().get(0).channel_number;
        var part = dataModelMultiChannel.getPart(mainChannelNum); // 主旋律パート
        if (part != null && part.getNoteOnlyList() != null) {
            var notes = part.getNoteOnlyList();
            for (int i = 0; i < notes.length; i++) {
                int noteMeasure = (int) (notes[i].onset() / measureTicks);
                if (noteMeasure == measureIdx && highlightIndexes.contains(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 音部記号を描画する（左手ならヘ音記号、右手ならト音記号）
     */
    private void drawClef(float x, float staffCenterY, boolean isLeft) {
        pushStyle();
        if (isLeft) {
            // ヘ音記号 (Bass Clef)
            // 第4線 (staffCenterY - 8) に主点
            fill(45);
            noStroke();
            ellipse(x + 2, staffCenterY - 8, 5.5f, 5.5f);

            noFill();
            stroke(45);
            strokeWeight(1.8f);
            // 上の半円から右へのアーチ、そして第2線へ降りるフック
            bezier(x + 2, staffCenterY - 8,
                   x + 2, staffCenterY - 17,
                   x + 12, staffCenterY - 17,
                   x + 12, staffCenterY - 7);
            bezier(x + 12, staffCenterY - 7,
                   x + 12, staffCenterY + 2,
                   x + 7, staffCenterY + 7,
                   x + 3, staffCenterY + 9);

            // 第4線を挟む2つのドット (第4間: staffCenterY - 12, 第3間: staffCenterY - 4)
            fill(45);
            noStroke();
            ellipse(x + 16, staffCenterY - 12, 2.8f, 2.8f);
            ellipse(x + 16, staffCenterY - 4, 2.8f, 2.8f);
        } else {
            // ト音記号 (Treble Clef)
            noFill();
            stroke(45);
            strokeWeight(1.8f);
            // 縦の芯
            line(x + 6, staffCenterY - 21, x + 6, staffCenterY + 16);
            fill(45);
            ellipse(x + 4, staffCenterY + 16, 3.5f, 3.5f);

            noFill();
            // G線（第2線: staffCenterY + 8）周りのスパイラル
            bezier(x + 6, staffCenterY + 8,
                   x + 15, staffCenterY + 8,
                   x + 15, staffCenterY,
                   x + 6, staffCenterY);
            bezier(x + 6, staffCenterY,
                   x - 2, staffCenterY,
                   x - 2, staffCenterY + 12,
                   x + 6, staffCenterY + 14);
            // 上部ループ
            bezier(x + 6, staffCenterY,
                   x - 2, staffCenterY - 9,
                   x + 12, staffCenterY - 21,
                   x + 6, staffCenterY - 21);
        }
        popStyle();
    }

    /**
     * 指定された小節の五線譜および音符をY位置に動的描画
     */
    private void drawMidiMeasureScoreAtY(int measureIdx, long T_start, float yEnd, float measureHeight) {
        if (dataModel == null) return;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        float startX = getDynamicScoreLeftX() + 20;
        float endX = getDynamicScoreRightX() - 20;
        float scoreW = endX - startX;
        float startY = yEnd - measureHeight;

        // 五線の中心（第3線）を小節の中央に配置
        float staffCenterY = startY + measureHeight / 2f; 
        long tickPosition = getCmx().getTickPosition();

        pushStyle();
        // 楽譜エリアの背景を完全に真っ白にしてストライプ背景を隠す
        noStroke();
        fill(255);
        rect(getDynamicScoreLeftX(), startY, getDynamicScoreRightX() - getDynamicScoreLeftX(), measureHeight);

        // 五線譜の描画 - 線の間隔を8pxに拡大
        stroke(110, 110, 120, 160);
        strokeWeight(1.2f);
        for (int i = -2; i <= 2; i++) {
            line(startX, staffCenterY + i * 8, endX, staffCenterY + i * 8);
        }

        // 音部記号の描画（右手：ト音記号、左手：ヘ音記号）
        drawClef(startX + 4, staffCenterY, isLeftHand());

        // 音符とタイムラインの描画パラメータ
        int beatNum = dataModelMultiChannel.getBeatNum();
        int division = dataModelMultiChannel.getScc().getDivision();
        long measureTicks = (long) beatNum * division;
        if (measureTicks <= 0) measureTicks = 1920;

        // 次の音を待機しているとき（停止中や待機中）は、打鍵待ち対象音符のOnsetにピタッと完全スナップ
        if (performanceData != null && (!getCmx().isNowPlaying() || isLearningMode())) {
            Long nextTick = performanceData.getNextWaitingTick();
            if (nextTick != null && nextTick >= T_start && nextTick < T_start + measureTicks) {
                if (!getCmx().isNowPlaying() || Math.abs(tickPosition - nextTick) < 120) {
                    tickPosition = nextTick;
                }
            }
        }

        float marginL = 35f; // 小節左端からの余白（音部記号の右側）
        float marginR = 25f; // 小節右端への余白
        float playableW = scoreW - marginL - marginR;

        // === 赤い演奏タイムライン（スライドする縦線プレイヘッド）を音符の【後ろ】に描画 ===
        if (tickPosition >= T_start && tickPosition < T_start + measureTicks) {
            float progress = (float) (tickPosition - T_start) / (float) measureTicks;
            progress = Math.max(0f, Math.min(1.0f, progress));
            float cursorX = startX + marginL + progress * playableW;

            pushStyle();
            float lineTopY = staffCenterY - 24;
            float lineBottomY = staffCenterY + 24;

            // 背景の薄い赤グロー
            stroke(255, 69, 58, 70);
            strokeWeight(4.0f);
            line(cursorX, lineTopY - 2, cursorX, lineBottomY + 2);

            // メインの赤い縦線（音符の中心と完全に一致）
            stroke(255, 45, 45, 230); // 鮮やかな赤
            strokeWeight(2.0f);
            line(cursorX, lineTopY, cursorX, lineBottomY);

            // 上下の小さな赤い三角インジケーター
            noStroke();
            fill(255, 45, 45);
            triangle(cursorX - 4, lineTopY - 4, cursorX + 4, lineTopY - 4, cursorX, lineTopY + 1);
            triangle(cursorX - 4, lineBottomY + 4, cursorX + 4, lineBottomY + 4, cursorX, lineBottomY - 1);
            popStyle();
        }

        // 音符の描画（赤い縦線の手前・最前面に描画）
        boolean isLeft = isLeftHand();
        int centerStep = isLeft ? 29 : 41; // ヘ音譜表の中心はD3(step 29)、ト音譜表の中心はB4(step 41)

        for (jp.kthrlab.pianoroll.Channel channel : dataModelMultiChannel.getChannels()) {
            var part = dataModelMultiChannel.getPart(channel.channel_number);
            if (part == null || part.getNoteOnlyList() == null) continue;

            for (var note : part.getNoteOnlyList()) {
                if (note.onset() >= T_start && note.onset() < T_start + measureTicks) {
                    double relOnset = note.onset() - T_start;
                    float noteX = startX + marginL + (float) (relOnset / measureTicks) * playableW;

                    int notenum = note.notenum();

                    // Y座標算出: centerStep基準、1ステップあたり4.0px
                    NoteAccidental acc = getNoteAccidental(notenum);
                    int step = acc.step;
                    int diff = step - centerStep; 
                    float noteY = staffCenterY - diff * 4.0f;

                    // 臨時記号（シャープ・フラット）のベクター描画（文字化け完全防止）
                    if ("SHARP".equals(acc.symbol)) {
                        drawSharp(noteX - 12f, noteY);
                    } else if ("FLAT".equals(acc.symbol)) {
                        drawFlat(noteX - 11f, noteY);
                    }

                    // 加線の描画
                    pushStyle();
                    stroke(80);
                    strokeWeight(1.2f);
                    if (isLeft) {
                        // ヘ音記号: E2(step 23)以下、および 中央C(C4, step 35)以上
                        if (step <= 23) {
                            for (int s = 23; s >= step; s -= 2) {
                                float ly = staffCenterY - (s - 29) * 4.0f;
                                line(noteX - 8, ly, noteX + 8, ly);
                            }
                        } else if (step >= 35) {
                            for (int s = 35; s <= step; s += 2) {
                                float ly = staffCenterY - (s - 29) * 4.0f;
                                line(noteX - 8, ly, noteX + 8, ly);
                            }
                        }
                    } else {
                        // ト音記号: 中央C(C4, step 35)以下、および A5(step 47)以上
                        if (step <= 35) {
                            for (int s = 35; s >= step; s -= 2) {
                                float ly = staffCenterY - (s - 41) * 4.0f;
                                line(noteX - 8, ly, noteX + 8, ly);
                            }
                        } else if (step >= 47) {
                            for (int s = 47; s <= step; s += 2) {
                                float ly = staffCenterY - (s - 41) * 4.0f;
                                line(noteX - 8, ly, noteX + 8, ly);
                            }
                        }
                    }
                    popStyle();

                    // 音符の長さ（音価）の判定
                    long stepTicks = getStepTicks(note, part.getNoteOnlyList());
                    double duration = (double) stepTicks / division;

                    // 五線譜の第3線（中央の線）以上の音は棒を左側に下向き、第3線未満の音は棒を右側に上向き
                    boolean isUpward = (step < centerStep);

                    if (duration >= 3.0) {
                        // 全音符: 白抜き楕円、棒なし (11x8.5px)
                        stroke(35);
                        strokeWeight(1.8f);
                        fill(255);
                        ellipse(noteX, noteY, 11, 8.5f);
                    } else if (duration >= 1.5) {
                        // 2分音符: 白抜き楕円 + 棒
                        stroke(35);
                        strokeWeight(1.8f);
                        fill(255);
                        ellipse(noteX, noteY, 11, 8.5f);
                        strokeWeight(1.5f);
                        if (isUpward) {
                            line(noteX + 5.5f, noteY, noteX + 5.5f, noteY - 22);
                        } else {
                            line(noteX - 5.5f, noteY, noteX - 5.5f, noteY + 22);
                        }
                    } else if (duration >= 0.75) {
                        // 4分音符: 黒塗り楕円 + 棒
                        noStroke();
                        fill(35);
                        ellipse(noteX, noteY, 11, 8.5f);
                        stroke(35);
                        strokeWeight(1.5f);
                        if (isUpward) {
                            line(noteX + 5.5f, noteY, noteX + 5.5f, noteY - 22);
                        } else {
                            line(noteX - 5.5f, noteY, noteX - 5.5f, noteY + 22);
                        }
                    } else {
                        // 8分音符以下: 黒塗り楕円 + 棒 + 旗
                        noStroke();
                        fill(35);
                        ellipse(noteX, noteY, 11, 8.5f);
                        stroke(35);
                        strokeWeight(1.5f);
                        if (isUpward) {
                            line(noteX + 5.5f, noteY, noteX + 5.5f, noteY - 22);
                            line(noteX + 5.5f, noteY - 22, noteX + 11.5f, noteY - 14);
                        } else {
                            line(noteX - 5.5f, noteY, noteX - 5.5f, noteY + 22);
                            line(noteX - 5.5f, noteY + 22, noteX, noteY + 14);
                        }
                    }
                }
            }
        }
        popStyle();
    }

    /**
     * MIDIデータから楽譜、ノート、非表示機能をタイムライン同期で一括描画する
     */
    /**
     * MIDIデータからノートのみを描画する
     */
    private void drawMidiNotesOnly() {
        if (dataModel == null) return;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        long tickPosition = getCmx().getTickPosition();
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;
        float pixelPerTick = (float)(double) dataModelMultiChannel.getPixelPerTick();
        float measureHeight = measureTicks * pixelPerTick;
        float timelineHeight = timeline.getSpan(); // height - keyboardHeight

        pushStyle();
        blendMode(BLEND);

        dataModelMultiChannel.getChannels().forEach(channel -> {
            var part = dataModelMultiChannel.getPart(channel.channel_number);
            if (part != null && part.getNoteOnlyList() != null) {
                for (MutableNote note : part.getNoteOnlyList()) {
                    long relativeOnset = note.onset() - tickPosition;
                    float h = (float) ((note.offset() - note.onset()) * pixelPerTick);
                    float y = timelineHeight - (float) (relativeOnset * pixelPerTick) - h;

                    if (y + h < 0 || y > timelineHeight) continue;

                    // このノートが属する小節が非表示されている場合は隠す
                    int noteMeasure = (int) (note.onset() / measureTicks);
                    if (isLearningMode() && isMeasureHighlighted(noteMeasure)) {
                        continue;
                    }

                    float x = horizontalKeyboard.semitoneXMap.get(note.notenum());
                    if (x >= getDynamicScoreLeftX() && x < getDynamicScoreRightX()) {
                        continue; // 楽譜エリアの裏に隠れるカラーバーは描画をスキップ
                    }
                    float w = timeline.getSemitoneWidth();

                    pushStyle();
                    float marginX = 1.5f;
                    float marginY = 1.0f;
                    
                    stroke(40, 45, 55, 200);
                    strokeWeight(1f);
                    if (!Keyboard.isWhiteKey(note.notenum())) {
                        // 黒鍵: 同系色で少し濃い色（明度約70%で深みのある色に）
                        int darkR = (int) (channel.color.getRed() * 0.70f);
                        int darkG = (int) (channel.color.getGreen() * 0.70f);
                        int darkB = (int) (channel.color.getBlue() * 0.70f);
                        fill(darkR, darkG, darkB);
                    } else {
                        fill(channel.color.getRGB());
                    }
                    rect(x + marginX, y + marginY, w - marginX * 2, h - marginY * 2, 3);

                    noStroke();
                    fill(255, 255, 255, 75);
                    rect(x + marginX + 1, y + marginY + 1, w - marginX * 2 - 2, Math.min(2.5f, h - marginY * 2 - 2), 1.5f);

                    popStyle();
                }
            }
        });
        popStyle();
    }

    /**
     * 楽譜エリア（白塗り）、五線譜、音符、非表示のみを最前面に描画
     */
    private void drawMidiScoreAndMaskOnly() {
        if (dataModel == null) return;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        long tickPosition = getCmx().getTickPosition();
        int division = dataModelMultiChannel.getScc().getDivision();
        int beatNum = dataModelMultiChannel.getBeatNum();
        int measureTicks = beatNum * division;
        float pixelPerTick = (float)(double) dataModelMultiChannel.getPixelPerTick();
        float measureHeight = measureTicks * pixelPerTick;
        float timelineHeight = timeline.getSpan();

        // 楽譜エリア全体の白塗り背景
        pushStyle();
        noStroke();
        fill(255);
        rect(getDynamicScoreLeftX(), 0, getDynamicScoreRightX() - getDynamicScoreLeftX(), height);
        popStyle();

        // 画面内に見える小節インデックスの範囲を計算
        int minMeasure = (int) (tickPosition / measureTicks);
        int maxMeasure = (int) ((tickPosition + timelineHeight / pixelPerTick) / measureTicks) + 1;

        // 各小節の五線譜と非表示機能の描画
        for (int m = minMeasure; m <= maxMeasure; m++) {
            long T_start = (long) m * measureTicks;
            float yEnd = timelineHeight - (float) (T_start - tickPosition) * pixelPerTick;
            float yStart = yEnd - measureHeight;

            if (yEnd < 0 || yStart > height) continue;

            // 五線譜を左側に描画
            drawMidiMeasureScoreAtY(m, T_start, yEnd, measureHeight);

            // 小節区切り線（楽譜・カラーバー共通の1小節の区切り横線。鍵盤表示領域には重ねない）
            pushStyle();
            stroke(200, 202, 210, 180);
            strokeWeight(1.0f);
            if (yEnd <= timelineHeight) {
                line(0, yEnd, width, yEnd);
            } else {
                line(getDynamicScoreLeftX(), yEnd, getDynamicScoreRightX(), yEnd);
            }
            popStyle();

            // この小節が非表示対象かどうかを判定して楽譜を非表示
            if (isLearningMode() && isMeasureHighlighted(m)) {
                pushStyle();
                noStroke();
                fill(255, 140, 0, 12);// オレンジ
                rect(getDynamicScoreLeftX(), yStart, getDynamicScoreRightX() - getDynamicScoreLeftX(), measureHeight);

                // 楽譜の右端のグレーのカラーバー
                for (jp.kthrlab.pianoroll.Channel channel : dataModelMultiChannel.getChannels()) {
                    var part = dataModelMultiChannel.getPart(channel.channel_number);
                    if (part == null || part.getNoteOnlyList() == null) continue;

                    for (var note : part.getNoteOnlyList()) {
                        if (note.onset() >= T_start && note.onset() < T_start + measureTicks) {
                            double relOnset = note.onset() - T_start;
                            float noteH = (float) ((note.offset() - note.onset()) * pixelPerTick);
                            float noteY = yEnd - (float) (relOnset / measureTicks) * measureHeight - noteH;

                            float ry = noteY + 1.0f;
                            float rh = noteH - 2.0f;
                            if (ry + rh > height) {
                                rh = height - ry;
                            }

                            if (rh > 0.5f) {
                                pushStyle();
                                stroke(100, 102, 110, 200); // 濃いめのグレー枠
                                strokeWeight(0.8f);
                                fill(160, 162, 170, 220); // 半透明グレー
                                rect(getDynamicScoreRightX() - 15, ry, 6, rh, 1.5f);
                                popStyle();
                            }
                        }
                    }
                }
                popStyle();
            }
        }

        // 楽譜エリアとピアノロールエリアの縦の境界線
        pushStyle();
        stroke(200, 202, 210, 200);
        strokeWeight(1f);
        line(getDynamicScoreLeftX(), 0, getDynamicScoreLeftX(), height);
        line(getDynamicScoreRightX(), 0, getDynamicScoreRightX(), height);
        popStyle();
    }

    @Override
    public void drawKeyboard() {
        // 白鍵を描画
        horizontalKeyboard.whiteKeyX.forEach(this::drawWhiteKey);
        // 黒鍵を描画
        horizontalKeyboard.blackKeyX.forEach(this::drawBlackKey);

        // 鍵盤の上端境界線
        stroke(200);
        strokeWeight(1);
        float ky = horizontalKeyboard.getKeyboardY();
        if (getDynamicScoreLeftX() > 0) {
            line(0, ky, getDynamicScoreLeftX(), ky);
        }
        line(getDynamicScoreRightX(), ky, width, ky);
    }

    private void drawWhiteKey(float x) {
        if (x >= getDynamicScoreLeftX() - 10 && x < getDynamicScoreRightX() - 10) return; // 楽譜エリアの上には鍵盤を描かない
        pushStyle();
        // 白鍵
        fill(255);
        stroke(215); 
        strokeWeight(1);
        float y = horizontalKeyboard.getKeyboardY();
        float w = keyboard.getWhiteKeyWidth();
        float h = keyboard.getWhiteKeyHeight();
        rect(x, y, w, h, 0, 0, 3, 3); 

        noStroke();
        fill(240, 240, 243);
        rect(x + 1, y + h - 5, w - 2, 4, 0, 0, 2, 2);
        popStyle();
    }

    private void drawBlackKey(float x) {
        if (x >= getDynamicScoreLeftX() - 10 && x < getDynamicScoreRightX() - 10) return; // 楽譜エリアの上には鍵盤を描かない
        pushStyle();
        // 黒鍵
        fill(25, 27, 33);
        stroke(40, 43, 50);
        strokeWeight(1);
        float y = horizontalKeyboard.getKeyboardY();
        float w = keyboard.getBlackKeyWidth();
        float h = keyboard.getBlackKeyHeight();
        rect(x, y, w, h, 0, 0, 2, 2);

        noStroke();
        fill(60, 65, 75);
        rect(x + 1, y + 1, w - 2, 3);
        popStyle();
    }

    @Override
    public void drawCurrentNote() {
        if (!(keyboard instanceof jp.kthrlab.pianoroll.HorizontalKeyboard))
            return;

        int currentNote = jp.kthrlab.pianoroll.CurrentInputState.getCurrentNote();
        if (currentNote < 0)
            return;

        jp.kthrlab.pianoroll.HorizontalKeyboard hk = (jp.kthrlab.pianoroll.HorizontalKeyboard) keyboard;

        float x;
        boolean isWhite = jp.kthrlab.pianoroll.Keyboard.isWhiteKey(currentNote);

        if (isWhite) {
            int whiteIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (jp.kthrlab.pianoroll.Keyboard.isWhiteKey(nn))
                    whiteIndex++;
            }
            x = hk.whiteKeyX.get(whiteIndex);
        } else {
            int blackIndex = 0;
            for (int nn = hk.getFirstNn(); nn < currentNote; nn++) {
                if (!jp.kthrlab.pianoroll.Keyboard.isWhiteKey(nn))
                    blackIndex++;
            }
            x = hk.blackKeyX.get(blackIndex);
        }

        if (x >= getDynamicScoreLeftX() - 10 && x < getDynamicScoreRightX() - 10) return; // 楽譜エリアの上には表示しない

        float w = isWhite ? hk.getWhiteKeyWidth() : hk.getBlackKeyWidth();
        float h = isWhite ? hk.getWhiteKeyHeight() : hk.getBlackKeyHeight();
        float y = hk.getKeyboardY();

        pushStyle();
        // 鍵盤のブルー
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

    public static class PdfRange {
        public final int startNoteIdx;
        public final int endNoteIdx;
        public final int pdfIndex;

        public PdfRange(int startNoteIdx, int endNoteIdx, int pdfIndex) {
            this.startNoteIdx = startNoteIdx;
            this.endNoteIdx = endNoteIdx;
            this.pdfIndex = pdfIndex;
        }
    }

    public static class PdfRangeBuilder {
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
}
