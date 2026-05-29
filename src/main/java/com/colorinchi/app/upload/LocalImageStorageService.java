package com.colorinchi.app.upload;

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
            Files.createDirectories(properties.directory());
            String filename = UUID.randomUUID() + ".jpg";
            Path target = properties.directory().resolve(filename).toAbsolutePath().normalize();

            try (InputStream input = file.getInputStream()) {
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
