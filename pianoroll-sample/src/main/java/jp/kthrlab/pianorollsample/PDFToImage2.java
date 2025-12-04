package jp.kthrlab.pianorollsample;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFToImage2 {
    public static BufferedImage loadFirstPage(String fileName) throws Exception {
        try (InputStream is = PDFToImage2.class.getResourceAsStream("/" + fileName);
             PDDocument document = PDDocument.load(is)) {
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(0, 150); // 150dpiの画像に変換
        }
    }
}
