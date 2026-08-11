package com.raota.ramenshop.presentation.admin.response;

import com.raota.account.domain.member.model.MemberProfile;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.model.RamenShopReport;
import com.raota.ramenshop.domain.model.RamenShopReportType;
import java.time.LocalDateTime;

public record RamenShopReportAdminResponse(
        Long id,
        Long shopId,
        String shopName,
        String branchName,
        Long memberId,
        String memberNickname,
        String memberEmail,
        RamenShopReportType reportType,
        String reportTypeDescription,
        String content,
        LocalDateTime reportedAt
) {

    public static RamenShopReportAdminResponse from(RamenShopReport report) {
        RamenShop shop = report.getRamenShop();
        MemberProfile member = report.getMemberProfile();
        RamenShopReportType reportType = report.getReportType();
        return new RamenShopReportAdminResponse(
                report.getId(),
                shop.getId(),
                shop.getName(),
                shop.getBranchName(),
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                reportType,
                reportType.getDescription(),
                report.getContent(),
                report.getReportedAt()
        );
    }
}
