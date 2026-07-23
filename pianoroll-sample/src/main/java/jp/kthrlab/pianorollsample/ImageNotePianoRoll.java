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

    private final List<PdfDisplay> pdfToDrawFront = new ArrayList<>();
    private final List<Float> pdfYFront = new ArrayList<>();

    public float getPdfLeftX() {
        return 0f;
    }

    public float getPdfRightX() {
        return 400f;
    }

    /** 各楽曲の最低音を検出し、そのカラーバーのすぐ左横に楽譜を表示するための左端X座標を返す */
    public float getDynamicScoreLeftX() {
        if (dataModel == null) return 0f;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        int minNn = 127;
        boolean found = false;
        try {
            for (var part : dataModelMultiChannel.getScc().getPartList()) {
                if (part == null || part.getNoteOnlyList() == null) continue;
                for (var note : part.getNoteOnlyList()) {
                    if (note.notenum() < minNn) {
                        minNn = note.notenum();
                        found = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!found) return 0f;

        float semitoneW = timeline.getSemitoneWidth();
        float offset = keyboard.getOffset();
        float minNnX = (minNn - keyboard.getFirstNn()) * semitoneW + offset;

        // 楽譜の左端を計算
        // 画面外にならないようにする
        float leftX = minNnX - 15f - 400f;
        return Math.max(0f, leftX);
    }

    public float getDynamicScoreRightX() {
        return getDynamicScoreLeftX() + 400f;
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

    //MIDI音高から五線上の位置を求める。C4(60) = 35 
    private int getDiatonicStep(int notenum) {
        int octave = notenum / 12;
        int noteInOctave = notenum % 12;
        int stepInOctave = 0;
        switch (noteInOctave) {
            case 0: stepInOctave = 0; break; // C
            case 1: stepInOctave = 0; break; // C#
            case 2: stepInOctave = 1; break; // D
            case 3: stepInOctave = 1; break; // D#
            case 4: stepInOctave = 2; break; // E
            case 5: stepInOctave = 3; break; // F
            case 6: stepInOctave = 3; break; // F#
            case 7: stepInOctave = 4; break; // G
            case 8: stepInOctave = 4; break; // G#
            case 9: stepInOctave = 5; break; // A
            case 10: stepInOctave = 5; break; // A#
            case 11: stepInOctave = 6; break; // B
        }
        return octave * 7 + stepInOctave;
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
     * 指定された小節の五線譜および音符をY位置に動的描画
     */
    private void drawMidiMeasureScoreAtY(int measureIdx, long T_start, float yEnd, float measureHeight) {
        if (dataModel == null) return;
        PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

        float startX = getDynamicScoreLeftX() + 15;
        float endX = getDynamicScoreRightX() - 15;
        float scoreW = endX - startX;
        float startY = yEnd - measureHeight;

        // 右手（ト音五線）のみを小節の中央に配置
        float trebleCenterY = startY + measureHeight / 2f; 
        long tickPosition = getCmx().getTickPosition();

        pushStyle();
        // 楽譜エリアの背景を完全に真っ白にしてストライプ背景を隠す
        noStroke();
        fill(255);
        rect(getDynamicScoreLeftX(), startY, getDynamicScoreRightX() - getDynamicScoreLeftX(), measureHeight);

        // 五線譜の描画 (右手のみ)
        stroke(120, 120, 130, 140);
        strokeWeight(1.0f);
        for (int i = -2; i <= 2; i++) {
            line(startX, trebleCenterY + i * 6, endX, trebleCenterY + i * 6);
        }



        // 音符の描画
        int division = dataModelMultiChannel.getScc().getDivision();
        float pixelPerTick = (float)(double) dataModelMultiChannel.getPixelPerTick();
        long measureTicks = (long)(measureHeight / pixelPerTick);

        for (jp.kthrlab.pianoroll.Channel channel : dataModelMultiChannel.getChannels()) {
            var part = dataModelMultiChannel.getPart(channel.channel_number);
            if (part == null || part.getNoteOnlyList() == null) continue;

            for (var note : part.getNoteOnlyList()) {
                if (note.onset() >= T_start && note.onset() < T_start + measureTicks) {
                    double relOnset = note.onset() - T_start;
                    float noteX = startX + 35 + (float) (relOnset / measureTicks) * (scoreW - 55);

                    int notenum = note.notenum();

                    // Y座標算出: B4(step 41)基準
                    int step = getDiatonicStep(notenum);
                    int diff = step - 41; 
                    float noteY = trebleCenterY - diff * 3.0f; // 1ステップあたり3px

                    // 今まさに弾いている音符か判定
                    boolean isCurrent = (tickPosition >= note.onset() && tickPosition <= note.offset());

                    // ハイライト色 or 通常色
                    if (isCurrent) {
                        stroke(64, 156, 255);
                        fill(64, 156, 255);
                        
                        pushStyle();
                        noFill();
                        stroke(64, 156, 255, 140);
                        strokeWeight(1.5f);
                        ellipse(noteX, noteY, 14, 11);
                        popStyle();
                    } else {
                        stroke(35);
                        fill(35);
                    }

                    // 臨時記号(シャープ)
                    boolean isSharp = !Keyboard.isWhiteKey(notenum);
                    if (isSharp) {
                        pushStyle();
                        if (isCurrent) fill(64, 156, 255); else fill(35);
                        textSize(11);
                        textAlign(RIGHT, CENTER);
                        text("#", noteX - 5, noteY - 1);
                        popStyle();
                    }

                    // 線の描画
                    pushStyle();
                    if (isCurrent) stroke(64, 156, 255); else stroke(90);
                    strokeWeight(1.0f);
                    if (step <= 35) { // 中央C(35)以下
                        for (int s = 35; s >= step; s -= 2) {
                            float ly = trebleCenterY - (s - 41) * 3.0f;
                            line(noteX - 6, ly, noteX + 6, ly);
                        }
                    } else if (step >= 47) { // A5(47)以上
                        for (int s = 47; s <= step; s += 2) {
                            float ly = trebleCenterY - (s - 41) * 3.0f;
                            line(noteX - 6, ly, noteX + 6, ly);
                        }
                    }
                    popStyle();

                    // 音符の長さ（音価）の判定
                    long stepTicks = getStepTicks(note, part.getNoteOnlyList());
                    double duration = (double) stepTicks / division;

                    if (duration >= 3.0) {
                        // 全音符: 白抜き楕円、棒なし
                        strokeWeight(1.5f);
                        if (isCurrent) fill(230, 242, 255); else fill(255);
                        ellipse(noteX, noteY, 8, 6);
                    } else if (duration >= 1.5) {
                        // 2分音符: 白抜き楕円 + 棒
                        strokeWeight(1.5f);
                        if (isCurrent) fill(230, 242, 255); else fill(255);
                        ellipse(noteX, noteY, 8, 6);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                    } else if (duration >= 0.75) {
                        // 4分音符: 黒塗り楕円 + 棒
                        noStroke();
                        ellipse(noteX, noteY, 8, 6);
                        if (isCurrent) stroke(64, 156, 255); else stroke(35);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                    } else {
                        // 8分音符以下: 黒塗り楕円 + 棒 + 旗
                        noStroke();
                        ellipse(noteX, noteY, 8, 6);
                        if (isCurrent) stroke(64, 156, 255); else stroke(35);
                        strokeWeight(1.2f);
                        line(noteX + 4, noteY, noteX + 4, noteY - 15);
                        line(noteX + 4, noteY - 15, noteX + 8, noteY - 10);
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
                    fill(channel.color.getRGB());
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
