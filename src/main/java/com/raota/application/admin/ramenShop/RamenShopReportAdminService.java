package com.raota.application.admin.ramenShop;

import com.raota.domain.ramenShop.model.RamenShopReportType;
import com.raota.domain.ramenShop.repository.RamenShopReportRepository;
import com.raota.presentation.admin.ramenShop.response.RamenShopReportAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RamenShopReportAdminService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final RamenShopReportRepository ramenShopReportRepository;

    @Transactional(readOnly = true)
    public Page<RamenShopReportAdminResponse> getReports(String keyword, RamenShopReportType reportType, int page, int size) {
        String normalizedKeyword = normalize(keyword);
        Long keywordId = parseId(normalizedKeyword);

        return ramenShopReportRepository.findAdminReports(
                normalizedKeyword == null,
                normalizedKeyword == null ? "%" : "%" + normalizedKeyword.toLowerCase() + "%",
                keywordId,
                reportType,
                PageRequest.of(Math.max(page, 0), normalizeSize(size))
        ).map(RamenShopReportAdminResponse::from);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Long parseId(String keyword) {
        if (keyword == null || !keyword.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
