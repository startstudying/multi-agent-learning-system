package com.learningos.rag.storage;

import com.learningos.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "learning-os.storage.mode", havingValue = "minio")
public class MinioDocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;
    private final MinioClient minioClient;

    public MinioDocumentStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.minioClient = MinioClient.builder()
                .endpoint(storageProperties.endpoint())
                .credentials(storageProperties.accessKey(), storageProperties.secretKey())
                .build();
    }

    @Override
    public StoredObject store(String kbId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String key = kbId + "/" + UUID.randomUUID() + "/" + originalName;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception exception) {
            throw new IOException("Failed to store document in MinIO", exception);
        }
        return new StoredObject(storageProperties.bucket(), key, file.getSize(), file.getContentType());
    }

    @Override
    public byte[] read(String bucket, String key) throws IOException {
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build())) {
            return input.readAllBytes();
        } catch (Exception exception) {
            throw new IOException("Failed to read document from MinIO", exception);
        }
    }
}
