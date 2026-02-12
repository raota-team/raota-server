package com.raota.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.proofPicture.controller.response.ProofPictureInfoResponse;
import com.raota.domain.proofPicture.controller.response.RamenShopProofPictureResponse;
import com.raota.domain.proofPicture.model.RamenProofPicture;
import com.raota.domain.proofPicture.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDataHelper.class)
public class RamenProofPictureRepositoryTest {

    @Autowired
    private RamenProofPictureRepository ramenProofPictureRepository;

    @Autowired
    private TestDataHelper testDataHelper;

    private MemberProfile memberProfile;
    private MemberProfile memberProfile2;
    private RamenShop ramenShop;

    @BeforeEach
    void setUp(){
        memberProfile = testDataHelper.createMember("테스트");
        memberProfile2 = testDataHelper.createMember("테스트2");
        ramenShop = testDataHelper.createRamenShop("테스트 라멘샵",new Address("서울시","영등포구","망원동","123"));
    }

    @DisplayName("이미지 url이 비어있으면 에러가 발생한다.")
    @Test
    void if_imageUrl_is_empty(){
        assertThatThrownBy(()->{
            RamenProofPicture picture = RamenProofPicture.builder()
                    .memberProfile(memberProfile)
                    .ramenShop(ramenShop)
                    .build();
            ramenProofPictureRepository.save(picture);
        });
    }

    @DisplayName("유저가 올린 사진 목록 페이지 객체를 불러온다.")
    @Test
    void user_uploaded_photos_page(){
        RamenProofPicture picture1 = testDataHelper.createProofPicture(ramenShop, memberProfile);
        RamenProofPicture picture2 = testDataHelper.createProofPicture(ramenShop, memberProfile);


        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<ProofPictureInfoResponse> result = ramenProofPictureRepository.findMemberUploadPhoto(memberProfile.getId(),pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        ProofPictureInfoResponse first = result.getContent().getFirst();

        assertThat(first.photo_id()).isEqualTo(picture2.getId());
        assertThat(first.image_url()).isEqualTo(picture2.getImageUrl());
        assertThat(first.uploader_nickname()).isEqualTo(memberProfile.getNickname());

        assertThat(result.getNumber()).isEqualTo(0);      // 현재 페이지
        assertThat(result.getSize()).isEqualTo(10);       // 요청한 size
        assertThat(result.isFirst()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("라멘가게의 인증샷들을 불러온다.")
    @Test
    void load_the_photos_from(){
        RamenProofPicture picture1 = testDataHelper.createProofPicture(ramenShop, memberProfile);
        RamenProofPicture picture2 = testDataHelper.createProofPicture(ramenShop, memberProfile2);

        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<RamenShopProofPictureResponse> result = ramenProofPictureRepository.searchPictures(ramenShop.getId(), pageRequest);

        RamenShopProofPictureResponse first = result.getContent().getFirst();
        RamenShopProofPictureResponse second = result.getContent().get(1);

        assertThat(result.getContent()).hasSize(2);

        assertThat(first.photo_id()).isEqualTo(picture2.getId());
        assertThat(second.photo_id()).isEqualTo(picture1.getId());

        assertThat(first.image_url()).isEqualTo(picture2.getImageUrl());
        assertThat(first.uploader_nickname()).isEqualTo(memberProfile2.getNickname());
        assertThat(first.uploaded_at()).isNotNull();

        assertThat(second.image_url()).isEqualTo(picture1.getImageUrl());
        assertThat(second.uploader_nickname()).isEqualTo(memberProfile.getNickname());
        assertThat(second.uploaded_at()).isNotNull();
    }
}
