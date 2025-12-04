//pianoroll/HorizontalPAppletCmxPianoRoll.javaにある
//PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
//long tickPosition = getCmx().getTickPosition();
////Object tickLock = tickPosition;
//Long relativeOnset = note.onset() - tickPosition;
//float h = (float) ((note.offset() - note.onset()) * dataModelMultiChannel.getPixelPerTick());
//float y = timeline.getSpan() - (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;

//pianoroll/HorizontalPAppletCmxPianoRoll.javaにあるdrawNote()内にカラーバーを描画するロジックがあるからそれを参考にpdfを描画したい
//ここのdrawSlice()で、はてなの縦幅を画像ごとに変更できるようにする
//MyAppのsetup()内にある
//dataModel = new PianoRollDataModelMultiChannel(
//                2,
//                2 + 12,
//                4,
//                channels,
//                musicData.getScc());

package jp.kthrlab.pianoroll.processing;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import jp.kthrlab.pianoroll.Keyboard;
import jp.kthrlab.pianoroll.PianoRoll;
import jp.kthrlab.pianoroll.Timeline;
import processing.core.PApplet;
import processing.core.PImage;

public class PAppletPianoRoll extends PApplet implements PianoRoll {
    protected Keyboard keyboard;
    protected Timeline timeline;

    /** 複数PDF × 各PDF縦10分割 */
    private PImage[][] pdfSlicesList;

    @Override
    public void settings() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        Frame frame = new Frame();
        frame.setUndecorated(false);
        frame.setSize(0, 0);
        frame.setVisible(true);

        size((int) usableBounds.width, (int) (usableBounds.height - frame.getInsets().top));

        frame.dispose();
    }

    @Override
    public void setup() {
        background(255);
        windowMove(0, 0);

        // ★ 複数PDFを読み込む
        String[] pdfs = {
                "/kirakira2_first2-midi.pdf",
                "/kirakira2_first4-midi.pdf",
                "/hatena.pdf"
        };

        loadMultiplePdfSlices(pdfs);
    }

    @Override
    public void draw() {
        background(255);

        drawTimeline();
        drawNote();

        //先にはてなを表示する
        drawSlice(2, 0, 580, 1.0f);
        
        // ★ PDF0 の 2番目のスライスを上に
        //drawSlice(0, 0, 100, 0.5f);

        // ★ PDF1 の 3番目のスライスを中段に
        drawSlice(1, 0, 580, 0.33f);

        drawPlayhead();
        drawKeyboard();
    }

    /**
     * 複数PDFを読み込み、それぞれ縦10分割する
     */
    private void loadMultiplePdfSlices(String[] pdfPaths) {
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

    /// **
    // * 指定したPDF・指定したスライスを任意の高さに描画
    // */
    // private void drawSlice(int pdfIndex, int sliceIndex, float offsetY) {
    // if (pdfSlicesList == null)
    // return;
    // if (pdfIndex >= pdfSlicesList.length)
    // return;
    // if (sliceIndex >= 10)
    // return;
    // if (pdfSlicesList[pdfIndex][sliceIndex] == null)
    // return;
    //
    // PImage img = pdfSlicesList[pdfIndex][sliceIndex];
    //
    // float w = img.width;
    // float h = img.height;
    //
    // float sx = (float) width / w;
    // float sy = (float) height / h;
    // float s = Math.min(sx, sy);
    //
    // pushMatrix();
    // translate((width - w * s) / 2f, offsetY);
    // scale(s);
    // // translate((width - (w * s * 0.5f)) / 2f, (height - h * s) / 2f + 350);
    // // scale(s * 0.5f, s); // ★ 横方向だけ 0.5倍にする
    // // image(img, 0, 0);
    // image(
    // img,
    // 0, 0, // 描画先の左上
    // (int)(w / 3), (int)h, // 描画先サイズ（半分の幅）
    // 0, 0, // ソース画像左上
    // (int)(w / 3), (int)h // ソース画像の右端（中央）
    // );
    // popMatrix();
    // }

    private void drawSlice(int pdfIndex, int sliceIndex, float offsetY, float cropXratio) {
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
        //translate((width - cropW * s) / 2f, offsetY);// 中央揃え
        translate(0, offsetY);//左寄せ
        scale(s);

        image(
                img,
                0, 0,
                (int) cropW, (int) h,
                0, 0,
                (int) cropW, (int) h);

        popMatrix();
    }

    @Override
    public void drawTimeline() {
    }

    @Override
    public void drawKeyboard() {
    }

    @Override
    public void drawNote() {
    }

    @Override
    public void drawPlayhead() {
    }
}
