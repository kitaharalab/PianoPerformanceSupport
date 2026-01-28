package jp.kthrlab.pianorollsample;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFToImage {
    /**
     * PDFの1ページ目を横方向に10等分した画像配列を返す
     */
    public static BufferedImage[] loadFirstPageSplitHorizontally(String fileName) throws Exception {
        try (InputStream is = PDFToImage.class.getResourceAsStream("/" + fileName);
             PDDocument document = PDDocument.load(is)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage fullImage = renderer.renderImageWithDPI(0, 1000); // 150dpiの画像に変換

            int width = fullImage.getWidth();
            int height = fullImage.getHeight();
            int partHeight = height / 19;
            BufferedImage[] parts = new BufferedImage[10];

            for (int i = 0; i < 10; i++) {
                int y = i * partHeight;
                int h = (i == 9) ? height - y : partHeight;
                parts[i] = fullImage.getSubimage(0, y, width, h);
            }
            return parts;
        }
    }
}