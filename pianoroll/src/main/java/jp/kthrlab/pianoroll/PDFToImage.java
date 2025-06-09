package jp.kthrlab.pianoroll;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFToImage {
    public static BufferedImage loadFirstPage(String fileName) throws Exception {
        try (InputStream is = PDFToImage.class.getResourceAsStream("/" + fileName);
             PDDocument document = PDDocument.load(is)) {
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(0, 150); // 150dpiの画像に変換
        }
    }
}
