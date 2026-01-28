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
import jp.kthrlab.pianorollsample.ImageNotePianoRoll.PdfDisplay;
import processing.core.PImage;

public class ImageNotePianoRoll extends HorizontalPAppletCmxPianoRoll {
    List<MutableNote> imageNotes = new java.util.ArrayList<>();

    private final List<PdfDisplay> pdfToDrawFront = new ArrayList<>();
    private final List<Float> pdfYFront = new ArrayList<>();

    @Override
    public void draw() {
        background(255);

        drawTimeline();
        drawNote();
        drawPlayhead();
        drawPdfGuideLines();
        drawKeyboard();
        drawCurrentNote();
        drawPdfFront();
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

    public void drawSliceAtY(int pdfIndex, int sliceIndex, float absoluteY, float cropXratio) {
        if (pdfSlicesList == null)
            return;
        if (pdfIndex >= pdfSlicesList.length)
            return;
        if (sliceIndex >= 10)
            return;
        if (pdfSlicesList[pdfIndex][sliceIndex] == null)
            return;

        PImage img = pdfSlicesList[pdfIndex][sliceIndex];

        float w = img.width;
        float h = img.height;

        float sx = (float) width / w;
        float sy = (float) height / h;
        float s = Math.min(sx, sy);

        float cropW = w * cropXratio;

        pushMatrix();
        translate(-170, absoluteY - (h * s));
        scale(s);

        image(img,
                0, 0,
                (int) cropW, (int) h,
                0, 0,
                (int) cropW, (int) h);

        popMatrix();
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
        for (int i = 0; i < pdfToDrawFront.size(); i++) {
            PdfDisplay pd = pdfToDrawFront.get(i);
            float y = pdfYFront.get(i);
            drawSliceAtY(pd.pdfIndex, pd.sliceIndex, y, 0.51f);
            stroke(80);
            strokeWeight(1);
            line(0, y, width / 3 + 86, y);
        }
    }

    void drawPdfGuideLines() {
        pushStyle();
        stroke(80);
        strokeWeight(1);

        for (int i = 0; i < pdfYFront.size(); i++) {
            float y = pdfYFront.get(i);
            line(0, y, width, y);
        }

        popStyle();
    }

    @Override
    public void drawNote() {
        pdfToDrawFront.clear();
        pdfYFront.clear();
        if (isNoteVisible && (dataModel != null)) {
            super.drawNote();
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

            long tickPosition = getCmx().getTickPosition();
            Object tickLock = tickPosition;
            synchronized (tickLock) {
                dataModelMultiChannel.getChannels().forEach(channel -> {
                    strokeWeight(1.0f);
                    stroke(channel.color.getRed(), channel.color.getGreen(), channel.color.getBlue(),
                            channel.color.getAlpha());
                    SCCDataSet.Part part = dataModelMultiChannel.getPart(channel.channel_number);

                    if (part != null && part.getNoteOnlyList() != null) {
                        int noteIndex = 0;
                        List<PdfDisplay> pdfToDraw = new ArrayList<>();
                        List<Float> pdfY = new ArrayList<>();
                        List<Float> pdfH = new ArrayList<>();
                        for (MutableNote note : part.getNoteOnlyList()) {

                            Long relativeOnset = note.onset() - tickPosition;
                            float h = (float) ((note.offset() - note.onset())
                                    * dataModelMultiChannel.getPixelPerTick());
                            float y = timeline.getSpan()
                                    - (float) (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;

                            if (y < 0) {
                                break;
                            }
                            float x = horizontalKeyboard.semitoneXMap.get(note.notenum());
                            float w = timeline.getSemitoneWidth();

                            int measure = (int) (note.onset() / dataModelMultiChannel.getScc().getDivision()
                                    / dataModelMultiChannel.getBeatNum());
                            double beat = note.onset() / (double) dataModelMultiChannel.getScc().getDivision()
                                    - (measure * dataModelMultiChannel.getBeatNum());
                            double duration = (note.offset() - note.onset())
                                    / (double) dataModelMultiChannel.getScc().getDivision();

                            boolean isImageNote = imageNotes.stream().anyMatch(
                                    imageNote -> imageNote.onset() == note.onset() &&
                                            imageNote.offset() == note.offset() &&
                                            imageNote.notenum() == note.notenum());

                            if (isImageNote) {
                                String path = getClass().getClassLoader().getResource(note.notenum() + ".png")
                                        .getPath();
                                PImage img = loadImage(path);
                                fill(Color.WHITE.getRGB());
                                noStroke();
                                image(img, x, y, w, h);
                            } else {
                                // 全てのノートを黄色で塗る
                                fill(channel.color.getRGB());
                                stroke(Color.LIGHT_GRAY.getRGB());
                                this.rect(x, y, w, h);
                            }
                            if (highlightIndexes.contains(noteIndex)) {
                                // 横全域にバーを描画
                                noStroke();
                                fill(200, 255);
                                this.rect(0, y, width, h);

                                fill(128);
                                textSize(32);
                                textAlign(CENTER, CENTER);
                                text("?", width / 2f, y + h / 2f);
                            }

                            // ノート描画ループ内で
                            if (pdfRule != null) {
                                PdfDisplay pd = pdfRule.apply(noteIndex);
                                if (pd != null && pd.pdfIndex >= 0 && pd.sliceIndex >= 0) {
                                    pdfToDrawFront.add(pd);
                                    pdfYFront.add(y + h);
                                }
                            }
                            noteIndex++;
                        }
                    }
                    blendMode(MULTIPLY);
                });

            }

        }
        blendMode(1);
    }
}
