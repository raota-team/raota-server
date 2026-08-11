package com.raota.global.file;

import com.raota.application.ramenShop.port.FileUrlPort;
import org.springframework.stereotype.Component;

@Component
public class FileUrlAdapter implements FileUrlPort {

    private final FileUploader fileUploader;

    public FileUrlAdapter(FileUploader fileUploader) {
        this.fileUploader = fileUploader;
    }

    @Override
    public String getAccessibleUrl(String filePath) {
        return fileUploader.getAccessibleUrl(filePath);
    }
}
