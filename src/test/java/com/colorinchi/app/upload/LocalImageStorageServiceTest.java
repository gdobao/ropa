package com.colorinchi.app.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;

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
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/jpeg"), 6000, 6000, 24_000_000L);
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
    void storeWithImageOverMaxWidthThrowsIllegalArgument() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/png"), 6000, 6000, 24_000_000L);
        service = new LocalImageStorageService(uploadProperties);

        MultipartFile file = imageFile(pngHeaderWithDimensions(6001, 100), "image/png");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demasiado grande")
                .hasMessageContaining("6000x6000");
    }

    @Test
    void storeWithImageOverMaxHeightThrowsIllegalArgument() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/png"), 6000, 6000, 24_000_000L);
        service = new LocalImageStorageService(uploadProperties);

        MultipartFile file = imageFile(pngHeaderWithDimensions(100, 6001), "image/png");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demasiado grande")
                .hasMessageContaining("6000x6000");
    }

    @Test
    void storeWithImageOverMaxPixelsThrowsIllegalArgument() throws Exception {
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/png"), 6000, 6000, 24_000_000L);
        service = new LocalImageStorageService(uploadProperties);

        MultipartFile file = imageFile(pngHeaderWithDimensions(5000, 5000), "image/png");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demasiado grande");
    }

    @Test
    void storeWithUnreadableImageDimensionsThrowsIllegalArgument() throws Exception {
        byte[] invalidJpegWithValidMagic = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MultipartFile file = imageFile(invalidJpegWithValidMagic, "image/jpeg");

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensiones");
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
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/png"), 6000, 6000, 24_000_000L);
        service = new LocalImageStorageService(uploadProperties);

        byte[] pngBytes = imageBytes("png", 1, 1);

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
        uploadProperties = new UploadProperties(tempDir, DataSize.ofMegabytes(8), List.of("image/webp"), 6000, 6000, 24_000_000L);
        service = new LocalImageStorageService(uploadProperties);

        byte[] webpBytes = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

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

    private MultipartFile imageFile(byte[] bytes, String contentType) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) bytes.length);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(bytes);
        return file;
    }

    private byte[] imageBytes(String format, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    private byte[] pngHeaderWithDimensions(int width, int height) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] ihdrData = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0)
                .array();
        byte[] ihdrType = {0x49, 0x48, 0x44, 0x52};
        CRC32 crc = new CRC32();
        crc.update(ihdrType);
        crc.update(ihdrData);

        ByteBuffer buffer = ByteBuffer.allocate(33);
        buffer.put(signature);
        buffer.putInt(13);
        buffer.put(ihdrType);
        buffer.put(ihdrData);
        buffer.putInt((int) crc.getValue());
        return buffer.array();
    }
}
