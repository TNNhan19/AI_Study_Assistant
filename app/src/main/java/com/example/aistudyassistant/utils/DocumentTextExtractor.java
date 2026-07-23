package com.example.aistudyassistant.utils;

import android.util.Xml;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Đọc văn bản từ các định dạng tài liệu mà ứng dụng hỗ trợ.
 */
public final class DocumentTextExtractor {

    private DocumentTextExtractor() {
    }

    public static String extract(byte[] fileBytes, String fileType) throws Exception {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Tài liệu không có dữ liệu");
        }

        String normalizedType = fileType == null
                ? ""
                : fileType.toLowerCase(Locale.US).replace(".", "").trim();
        switch (normalizedType) {
            case "txt":
                return extractText(fileBytes);
            case "pdf":
                return extractPdf(fileBytes);
            case "docx":
                return extractDocx(fileBytes);
            case "doc":
                throw new IllegalArgumentException("Chưa hỗ trợ đọc file DOC cũ; hãy đổi sang DOCX hoặc PDF");
            default:
                throw new IllegalArgumentException("Định dạng tài liệu không được hỗ trợ: " + normalizedType);
        }
    }

    private static String extractText(byte[] fileBytes) {
        String text = new String(fileBytes, StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private static String extractPdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractDocx(byte[] fileBytes) throws Exception {
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return readDocxXml(zipInput);
                }
            }
        }
        throw new IllegalArgumentException("File DOCX không chứa nội dung hợp lệ");
    }

    private static String readDocxXml(ZipInputStream inputStream) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(inputStream, StandardCharsets.UTF_8.name());

        StringBuilder text = new StringBuilder();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG && "t".equals(name)) {
                text.append(parser.nextText());
            } else if (event == XmlPullParser.START_TAG && "tab".equals(name)) {
                text.append('\t');
            } else if (event == XmlPullParser.START_TAG && "br".equals(name)) {
                text.append('\n');
            } else if (event == XmlPullParser.END_TAG && "p".equals(name)) {
                text.append('\n');
            }
            event = parser.next();
        }
        return text.toString();
    }
}
