package com.raota.global.presentation.file;
import com.raota.global.file.FileUploader;
import com.raota.global.presentation.file.FIleController;
import com.raota.global.presentation.file.response.PresignedUrlResponse;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FIleControllerTest {

    private FileUploader fileUploader;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fileUploader = mock(FileUploader.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FIleController(fileUploader)).build();
    }

    @Test
    void getUploadTicketSupportsBackgroundImages() throws Exception {
        given(fileUploader.getPresignedUrl(eq("backgrounds"), eq("jpg"), eq("image/jpeg")))
                .willReturn(PresignedUrlResponse.of("https://upload.example.com/background", "https://images.example.com/background.jpg"));

        mockMvc.perform(get("/files/upload-ticket")
                        .param("type", "BACKGROUND")
                        .param("extension", "jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upload_url").value("https://upload.example.com/background"))
                .andExpect(jsonPath("$.img_url").value("https://images.example.com/background.jpg"));

        verify(fileUploader).getPresignedUrl("backgrounds", "jpg", "image/jpeg");
    }
}
