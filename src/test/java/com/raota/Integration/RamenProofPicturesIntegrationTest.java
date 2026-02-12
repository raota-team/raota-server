package com.raota.Integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.proofPicture.model.RamenProofPicture;
import com.raota.domain.proofPicture.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.global.file.FileUploader;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class RamenProofPicturesIntegrationTest {

    @LocalServerPort
    int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockitoBean
    FileUploader fileUploader;

    @Autowired private RamenShopRepository ramenShopRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RamenProofPictureRepository proofPictureRepository;

    private Long targetShopId;
    private Long memberId;
    private final String mockImageUrl = "https://mock.cdn.com/uploaded/702.jpg";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        clearDatabase();

        RamenShop shop = ramenShopRepository.save(RamenShop.builder().name("인증샵").build());
        targetShopId = shop.getId();

        MemberProfile member = memberRepository.save(MemberProfile.builder().nickname("방금먹음").build());
        memberId = member.getId();

        when(fileUploader.upload(any(), any())).thenReturn(mockImageUrl);
    }

    private void clearDatabase() {
        proofPictureRepository.deleteAll();
        memberRepository.deleteAll();
        ramenShopRepository.deleteAll();
    }

    @Test
    @DisplayName("통합: 인증샷 업로드에 성공하면 Mock URL과 DB에 저장된 정보를 반환한다.")
    void upload_proof_picture_integration(){
        byte[] fileContent = "dummy image data".getBytes();

        RestAssured.given().log().all()
                .multiPart("file","test.jpg",fileContent,"image/jpeg")
                .header("X-User-Id", memberId)

                .when()
                .post("/photos/{shopId}", targetShopId)

                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("SUCCESS"))
                .body("data.image_url", equalTo(mockImageUrl))
                .body("data.uploader_nickname", equalTo("방금먹음"));

        assertThat(proofPictureRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("통합: 인증샷 목록을 페이지로 조회하면 DB의 콘텐츠를 반환한다")
    void get_proof_pictures_returns_page_integration() {
        RamenShop shop = ramenShopRepository.findById(targetShopId).get();
        MemberProfile me = memberRepository.findById(memberId).get();

        proofPictureRepository.save(RamenProofPicture.builder()
                .ramenShop(shop).memberProfile(me).imageUrl("http://photo_a.com").uploadAt(LocalDateTime.now()).build());
        proofPictureRepository.save(RamenProofPicture.builder()
                .ramenShop(shop).memberProfile(me).imageUrl("http://photo_b.com").uploadAt(LocalDateTime.now()).build());

        RestAssured.given().log().all()
                .param("page", "0")
                .param("size", "1")

                .when()
                .get("/photos/{shopId}", targetShopId)

                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("SUCCESS"))
                .body("data.content", hasSize(1))
                .body("data.content[0].uploader_nickname", equalTo("방금먹음"))

                .body("data.number", equalTo(0))
                .body("data.size", equalTo(1))
                .body("data.totalElements", equalTo(2))
                .body("data.totalPages", equalTo(2));
    }
}
