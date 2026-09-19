package com.raota.web.ramenshop.infrastructure.file;

import com.raota.web.ramenshop.application.port.FileUrlPort;
import com.raota.global.file.FileUploader;
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
