package com.raota.domain.ramenShop.repository;

import com.raota.domain.ramenShop.model.RamenShopReport;
import com.raota.domain.ramenShop.model.RamenShopReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenShopReportRepository extends JpaRepository<RamenShopReport, Long> {

    @Query(value = """
            select report
            from RamenShopReport report
            join fetch report.ramenShop shop
            join fetch report.memberProfile member
            where (:keywordBlank = true
                   or lower(shop.name) like :keyword
                   or lower(coalesce(shop.branchName, '')) like :keyword
                   or lower(member.nickname) like :keyword
                   or lower(coalesce(member.email, '')) like :keyword
                   or lower(report.content) like :keyword
                   or (:keywordId is not null and (
                        report.id = :keywordId
                        or shop.id = :keywordId
                        or member.id = :keywordId
                   )))
              and (:reportType is null or report.reportType = :reportType)
            order by report.reportedAt desc, report.id desc
            """,
            countQuery = """
                    select count(report)
                    from RamenShopReport report
                    join report.ramenShop shop
                    join report.memberProfile member
                    where (:keywordBlank = true
                           or lower(shop.name) like :keyword
                           or lower(coalesce(shop.branchName, '')) like :keyword
                           or lower(member.nickname) like :keyword
                           or lower(coalesce(member.email, '')) like :keyword
                           or lower(report.content) like :keyword
                           or (:keywordId is not null and (
                                report.id = :keywordId
                                or shop.id = :keywordId
                                or member.id = :keywordId
                           )))
                      and (:reportType is null or report.reportType = :reportType)
                    """)
    Page<RamenShopReport> findAdminReports(
            @Param("keywordBlank") boolean keywordBlank,
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("reportType") RamenShopReportType reportType,
            Pageable pageable
    );
}
