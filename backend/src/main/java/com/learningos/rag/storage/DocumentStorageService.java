package com.learningos.rag.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentStorageService {

    StoredObject store(String kbId, MultipartFile file) throws IOException;

    byte[] read(String bucket, String key) throws IOException;
}
