package com.colorinchi.app.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.colorinchi.app.config.UploadProperties;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private final UploadProperties properties;

    public LocalImageStorageService(UploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);
        try {
            byte[] bytes = file.getBytes();
            if (!isValidImage(bytes)) {
                throw new IllegalArgumentException("El archivo no es una imagen válida (JPEG, PNG o WebP)");
            }

            Files.createDirectories(properties.directory());
            String filename = UUID.randomUUID() + ".jpg";
            Path target = properties.directory().resolve(filename).toAbsolutePath().normalize();

            try (InputStream input = new ByteArrayInputStream(bytes)) {
                Thumbnails.of(input)
                        .size(900, 900)
                        .outputQuality(0.88)
                        .toFile(target.toFile());
            }
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la imagen", ex);
        }
    }

    private boolean isValidImage(byte[] header) {
        if (header == null || header.length < 4) {
            return false;
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && (header[7] & 0xFF) == 0x0A) {
            return true;
        }
        // WebP: RIFF at 0-3, WEBP at 8-11
        if (header.length >= 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return true;
        }
        return false;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen de la prenda.");
        }
        if (file.getSize() > properties.maxSize().toBytes()) {
            throw new IllegalArgumentException("La imagen supera el tamano maximo permitido.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException("El archivo debe ser una imagen JPG, PNG o WebP.");
        }
    }
}
