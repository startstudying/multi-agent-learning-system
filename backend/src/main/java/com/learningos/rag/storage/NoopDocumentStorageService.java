package com.learningos.rag.storage;

import com.learningos.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "learning-os.storage.mode", havingValue = "noop", matchIfMissing = true)
public class NoopDocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    public NoopDocumentStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredObject store(String kbId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String key = kbId + "/" + UUID.randomUUID() + "/" + originalName;
        objects.put(objectKey(storageProperties.bucket(), key), file.getBytes());
        return new StoredObject(storageProperties.bucket(), key, file.getSize(), file.getContentType());
    }

    @Override
    public byte[] read(String bucket, String key) {
        return objects.get(objectKey(bucket, key));
    }

    private String objectKey(String bucket, String key) {
        return bucket + "/" + key;
    }
}
