package jp.kthrlab.pianorollsample;

import java.awt.Color;
import java.awt.image.BufferedImage;
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

    /** 複数PDF × 各PDF縦10分割 */
    private PImage[][] pdfSlicesList;

    /**
     * 複数PDFを読み込み、それぞれ縦10分割する
     */
    public void loadMultiplePdfSlices(String[] pdfPaths) {
        pdfSlicesList = new PImage[pdfPaths.length][10];

        for (int i = 0; i < pdfPaths.length; i++) {
            pdfSlicesList[i] = loadPdfSlicesVertical(pdfPaths[i]);
        }
    }

    /**
     * PDF を読み込み、縦方向 10 分割した PImage[] を返す
     */
    private PImage[] loadPdfSlicesVertical(String pdfResource) {
        PImage[] slices = new PImage[10];

        try (var in = getClass().getResourceAsStream(pdfResource)) {
            if (in == null) {
                System.out.println("PDF not found: " + pdfResource);
                return slices;
            }

            try (PDDocument doc = PDDocument.load(in)) {
                PDFRenderer renderer = new PDFRenderer(doc);

                float dpi = 150f;
                BufferedImage bi = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);

                int w = bi.getWidth();
                int h = bi.getHeight();

                int sliceHeight = h / 10;

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
        translate(-180, absoluteY);
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

    private java.util.function.Function<Integer, PdfDisplay> pdfRule = null;

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

    @Override
    public void drawNote() {
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
                        for (MutableNote note : part.getNoteOnlyList()) {

                            Long relativeOnset = note.onset() - tickPosition;
                            float h = (float) ((note.offset() - note.onset())
                                    * dataModelMultiChannel.getPixelPerTick());
                            float y = timeline.getSpan()
                                    - (float) (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;
                            // Break if it is outside the drawing range
                            if (y < 0) {
                                break;
                            }
                            float x = horizontalKeyboard.semitoneXMap.get(note.notenum());
                            float w = timeline.getSemitoneWidth();
                            // println(String.format("tickPosition=%s, note.onset()=%s, relativeOnset=%s,
                            // x=%s, y=%s, w=%s, h=%s",tickPosition, note.onset(), relativeOnset, x, y, w,
                            // h));

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
                                //// 全てのノートを黄色で塗る
                                // fill(channel.color.getRGB());
                                // stroke(Color.LIGHT_GRAY.getRGB());
                                // this.rect(x, y, w, h);

                                // ノートによって色を変更
                                int noteInOctave = note.notenum() % 12;
                                int fillColor;

                                switch (noteInOctave) {
                                    case 0: // C
                                        fillColor = Color.RED.getRGB();
                                        break;
                                    case 2: // D
                                        fillColor = Color.ORANGE.getRGB();
                                        break;
                                    case 4: // E
                                        fillColor = Color.YELLOW.getRGB();
                                        break;
                                    case 5: // F
                                        fillColor = Color.GREEN.getRGB();
                                        break;
                                    case 7: // G
                                        fillColor = new Color(135, 206, 235).getRGB();// 水色
                                        break;
                                    case 9: // A
                                        fillColor = Color.BLUE.getRGB();
                                        break;
                                    case 11: // B
                                        fillColor = new Color(128, 0, 128).getRGB(); // 紫
                                        break;
                                    default:
                                        fillColor = channel.color.getRGB(); // 黒鍵などは元の色
                                        break;
                                }

                                fill(fillColor);
                                stroke(Color.LIGHT_GRAY.getRGB());
                                this.rect(x, y, w, h);

                                //// ノートによって色を変更（オクターブによって彩度を変える）
                                // int noteInOctave = note.notenum() % 12;
                                // int baseColor;
                                //
                                // switch (noteInOctave) {
                                // case 0: // C
                                // baseColor = Color.RED.getRGB();
                                // break;
                                // case 2: // D
                                // baseColor = Color.ORANGE.getRGB();
                                // break;
                                // case 4: // E
                                // baseColor = Color.YELLOW.getRGB();
                                // break;
                                // case 5: // F
                                // baseColor = Color.GREEN.getRGB();
                                // break;
                                // case 7: // G
                                // baseColor = new Color(135, 206, 235).getRGB();// 水色
                                // break;
                                // case 9: // A
                                // baseColor = Color.BLUE.getRGB();
                                // break;
                                // case 11: // B
                                // baseColor = new Color(128, 0, 128).getRGB(); // 紫
                                // break;
                                // default:
                                // baseColor = channel.color.getRGB(); // 黒鍵などは元の色
                                // break;
                                // }
                                //
                                //// オクターブ番号を取得
                                // int octave = note.notenum() / 12;
                                //
                                //// RGB → HSB変換
                                // float[] hsb = Color.RGBtoHSB(
                                // (baseColor >> 16) & 0xFF,
                                // (baseColor >> 8) & 0xFF,
                                // (baseColor) & 0xFF,
                                // null);
                                //
                                //// 彩度をオクターブごとに変化させる
                                //// 例: オクターブが高いほど彩度を下げる（0.2〜1.0の範囲）
                                // float saturation = Math.max(0.2f, 1.0f - (octave * 0.1f));
                                // int adjustedColor = Color.HSBtoRGB(hsb[0], saturation, hsb[2]);
                                //
                                // fill(adjustedColor);
                                // stroke(Color.LIGHT_GRAY.getRGB());
                                // this.rect(x, y, w, h);

                            }
                            if (highlightIndexes.contains(noteIndex)) {
                                // 横全域にバーを描画
                                noStroke();
                                fill(128, 255);
                                this.rect(0, y, width, h);

                                fill(255); // 白
                                textSize(32); // 好きなサイズに変更可能
                                textAlign(CENTER, CENTER);
                                text("?", width / 2f, y + h / 2f);
                            }
                            if (pdfRule != null) {
                                //pdf描画
                                PdfDisplay pd = pdfRule.apply(noteIndex);

                                if (pd != null && pd.pdfIndex >= 0 && pd.sliceIndex >= 0) {

                                    this.drawSliceAtY(
                                            pd.pdfIndex,
                                            pd.sliceIndex,
                                            y,
                                            0.5f);
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
