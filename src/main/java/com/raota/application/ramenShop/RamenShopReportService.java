package com.raota.application.ramenShop;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.presentation.api.ramenShop.dto.RamenShopReportRequest;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.model.RamenShopReport;
import com.raota.domain.ramenShop.repository.RamenShopReportRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RamenShopReportService {

    private final RamenShopReportRepository reportRepository;
    private final RamenShopRepository ramenShopRepository;
    private final MemberRepository memberRepository;

    public void reportShop(Long shopId, Long memberId, RamenShopReportRequest request) {
        RamenShop shop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘 가게를 찾을 수 없습니다."));
        MemberProfile member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RamenShopReport report = RamenShopReport.create(
                shop,
                member,
                request.getReportType(),
                request.getContent()
        );

        reportRepository.save(report);
    }
}
