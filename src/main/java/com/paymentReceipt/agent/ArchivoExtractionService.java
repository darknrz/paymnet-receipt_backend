package com.paymentReceipt.agent;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ArchivoExtractionService {

    /**
     * Extrae texto de un archivo subido (PDF o texto plano).
     * En una implementación completa, se podría agregar OCR para imágenes.
     */
    public String extraerTexto(MultipartFile archivo) throws IOException {
        String contentType = archivo.getContentType();
        String nombreArchivo = archivo.getOriginalFilename();

        log.info("Extrayendo texto de archivo: {} (tipo: {})", nombreArchivo, contentType);

        if (contentType != null && contentType.equals("application/pdf")) {
            return extraerTextoPDF(archivo);
        } else if (contentType != null && contentType.startsWith("text/")) {
            return new String(archivo.getBytes(), StandardCharsets.UTF_8);
        } else if (nombreArchivo != null && nombreArchivo.toLowerCase().endsWith(".pdf")) {
            return extraerTextoPDF(archivo);
        } else {
            // Para imágenes o formatos no soportados directamente,
            // retornamos metadata para que el agent intente con lo que tenga
            log.warn("Tipo de archivo no soportado para extracción directa: {}", contentType);
            return "Archivo: " + nombreArchivo + " (tipo: " + contentType + ") - No se pudo extraer texto directamente. " +
                   "Por favor, proporcione el contenido del comprobante como texto.";
        }
    }

    private String extraerTextoPDF(MultipartFile archivo) throws IOException {
        try (PDDocument document = PDDocument.load(archivo.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);
            log.debug("Texto extraído del PDF ({} caracteres)", texto.length());
            return texto;
        } catch (IOException e) {
            log.error("Error al extraer texto del PDF: {}", e.getMessage());
            throw new IOException("No se pudo leer el archivo PDF: " + e.getMessage(), e);
        }
    }
}
