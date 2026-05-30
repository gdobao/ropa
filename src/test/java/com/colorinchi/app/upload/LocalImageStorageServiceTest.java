package com.colorinchi.app.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.colorinchi.app.config.UploadProperties;

import net.coobird.thumbnailator.Thumbnails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class LocalImageStorageServiceTest {

    @TempDir
    Path tempDir;

    private UploadProperties uploadProperties;
    private LocalImageStorageService service;

    private byte[] validJpegBytes;

    @BeforeEach
    void setUp() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/jpeg"));
        service = new LocalImageStorageService(uploadProperties);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        validJpegBytes = baos.toByteArray();
    }

    @Test
    void storeWithNullFileThrowsIllegalArgument() {
        assertThatThrownBy(() -> service.store(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selecciona");
    }

    @Test
    void storeWithEmptyFileThrowsIllegalArgument() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selecciona");
    }

    @Test
    void storeWithOversizedFileThrowsIllegalArgument() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(DataSize.ofMegabytes(9).toBytes());
        when(file.getContentType()).thenReturn("image/jpeg");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamano maximo");
    }

    @Test
    void storeWithInvalidContentTypeThrowsIllegalArgument() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imagen");
    }

    @Test
    void storeWithNullContentTypeThrowsIllegalArgument() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imagen");
    }

    @Test
    void storeWithValidImageReturnsUploadUrl() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) validJpegBytes.length);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(validJpegBytes));
        when(file.getBytes()).thenReturn(validJpegBytes);

        String url = service.store(file);

        assertThat(url).startsWith("/uploads/");
        assertThat(url).endsWith(".jpg");

        Path savedFile = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(savedFile).exists();
    }

    @Test
    void storeWithNullBytesThrowsIllegalArgument() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(new byte[]{0, 0, 0});

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imagen válida");
    }

    @Test
    void storeWithShortHeaderThrowsIllegalArgument() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(new byte[]{0, 0});

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imagen válida");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void storeWithPngImageReturnsUploadUrl() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/png"));
        service = new LocalImageStorageService(uploadProperties);

        byte[] pngBytes = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) pngBytes.length);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(pngBytes);

        Thumbnails.Builder builder = mock(Thumbnails.Builder.class);
        when(builder.size(anyInt(), anyInt())).thenReturn(builder);
        when(builder.outputQuality(anyDouble())).thenReturn(builder);

        try (MockedStatic<Thumbnails> mocked = mockStatic(Thumbnails.class)) {
            mocked.when(() -> Thumbnails.of(any(InputStream.class))).thenReturn(builder);

            String url = service.store(file);
            assertThat(url).startsWith("/uploads/");
            assertThat(url).endsWith(".jpg");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void storeWithWebPImageReturnsUploadUrl() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/webp"));
        service = new LocalImageStorageService(uploadProperties);

        byte[] webpBytes = {
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x20
        };

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) webpBytes.length);
        when(file.getContentType()).thenReturn("image/webp");
        when(file.getBytes()).thenReturn(webpBytes);

        Thumbnails.Builder builder = mock(Thumbnails.Builder.class);
        when(builder.size(anyInt(), anyInt())).thenReturn(builder);
        when(builder.outputQuality(anyDouble())).thenReturn(builder);

        try (MockedStatic<Thumbnails> mocked = mockStatic(Thumbnails.class)) {
            mocked.when(() -> Thumbnails.of(any(InputStream.class))).thenReturn(builder);

            String url = service.store(file);
            assertThat(url).startsWith("/uploads/");
            assertThat(url).endsWith(".jpg");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void storeWithThumbnailatorIOExceptionThrowsIllegalState() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) validJpegBytes.length);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(validJpegBytes));
        when(file.getBytes()).thenReturn(validJpegBytes);

        Thumbnails.Builder builder = mock(Thumbnails.Builder.class);
        when(builder.size(anyInt(), anyInt())).thenReturn(builder);
        when(builder.outputQuality(anyDouble())).thenReturn(builder);
        doThrow(new IOException("Disk full")).when(builder).toFile(any(java.io.File.class));

        try (MockedStatic<Thumbnails> mocked = mockStatic(Thumbnails.class)) {
            mocked.when(() -> Thumbnails.of(any(InputStream.class))).thenReturn(builder);

            assertThatThrownBy(() -> service.store(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("guardar");
        }
    }
}
