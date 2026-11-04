package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.notification.application.facade.MobilePushFacade;
import com.raota.mobile.ramenlog.application.result.MobileCurrentTasteReportResult;
import com.raota.mobile.ramenlog.application.result.MobileCurrentTasteReportResult.Upcoming;
import com.raota.mobile.ramenlog.application.result.MobileDrawnTasteReportResult;
import com.raota.mobile.ramenlog.application.result.MobileTasteReportResult;
import com.raota.mobile.ramenlog.application.result.MobileTasteReportSeenResult;
import com.raota.mobile.ramenlog.domain.model.MobileTasteReport;
import com.raota.mobile.ramenlog.domain.model.MobileTasteReportFigures;
import com.raota.mobile.ramenlog.domain.model.TasteReportKind;
import com.raota.mobile.ramenlog.domain.model.TasteTopShop;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileTasteReportRepository;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 앱 {@code reportDraw.ts}와 같은 규칙이고, 날짜는 모두 서울 기준이다.
 *
 * <ul>
 *   <li>첫 리포트: 리포트가 없고 기록이 {@value #TARGET}개 이상이면 오늘을 기준일로 뽑는다. 기록 작성 직후와 조회 때 확인한다.
 *   <li>매달 리포트: 매달 1일, 최근 리포트 기준일 다음 날부터 지난달 말일까지 방문한 기록이 {@value #TARGET}개 이상이면 지난달 말일을 기준일로 뽑는다.
 *       배치와 조회 때 확인한다.
 *   <li>기록을 지우거나 방문일을 바꿔 최근 기준일까지의 기록이 하나도 없으면 그 회원의 리포트를 모두 지워 첫 리포트부터 다시 센다.
 * </ul>
 *
 * <p>리포트는 기준일까지의 기록을 모두 센다. 기준일 이전 날짜로 나중에 남긴 기록은 이미 뽑은 리포트를 바꾸지 않고, 다음 리포트를 뽑을 기록 수에도 들어가지 않는다.
 * {@code (user_id, cutoff_date)} UNIQUE라 배치가 두 번 돌거나 여러 경로가 겹쳐도 리포트는 하나다.
 */
@Slf4j
@Service
public class MobileTasteReportService {

    static final int TARGET = 3;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MobileTasteReportRepository reports;

    private final MobileRamenLogRepository logs;

    private final MobileShopRamenLogFacade shops;

    private final MobileAccountRamenLogFacade accounts;

    private final MobilePushFacade push;

    private final Clock clock;

    private final TransactionTemplate newTransaction;

    public MobileTasteReportService(
            MobileTasteReportRepository reports,
            MobileRamenLogRepository logs,
            MobileShopRamenLogFacade shops,
            MobileAccountRamenLogFacade accounts,
            MobilePushFacade push,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.reports = reports;
        this.logs = logs;
        this.shops = shops;
        this.accounts = accounts;
        this.push = push;
        this.clock = clock;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 기록 작성이 커밋된 뒤 부른다. 뽑지 않았으면 null이다. */
    public MobileDrawnTasteReportResult drawFirstAfterRecord(Long userId) {
        return drawFirst(userId, today())
                .map(report -> new MobileDrawnTasteReportResult(
                        String.valueOf(report.getId()), report.getKind(), report.getCutoffDate()))
                .orElse(null);
    }

    /** 빠진 뽑기(배치 실패, 배포 중 누락)를 먼저 만든 뒤 가장 최근 리포트를 돌려준다. */
    public MobileCurrentTasteReportResult current(Long userId) {
        LocalDate today = today();
        drawFirst(userId, today);
        drawMonthly(userId, lastDayOfPreviousMonth(today));
        MobileTasteReport latest =
                reports.findFirstByUserIdOrderByCutoffDateDesc(userId).orElse(null);
        if (latest == null) {
            long needed = TARGET - logs.countByUserIdAndDeletedAtIsNull(userId);
            return new MobileCurrentTasteReportResult(null, new Upcoming(null, atLeastZero(needed)));
        }
        long fresh = logs.countByUserIdAndDeletedAtIsNullAndVisitedAtAfter(userId, latest.getCutoffDate());
        LocalDate nextDraw = today.withDayOfMonth(1).plusMonths(1);
        return new MobileCurrentTasteReportResult(
                toResult(latest), new Upcoming(nextDraw, atLeastZero(TARGET - fresh)));
    }

    @Transactional
    public MobileTasteReportSeenResult markSeen(Long userId, Long reportId) {
        reports.markSeen(reportId, userId, now());
        MobileTasteReport report = reports.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "취향 리포트를 찾을 수 없습니다."));
        return new MobileTasteReportSeenResult(report.getSeenAt());
    }

    /** 기록을 지우거나 방문일을 바꾼 트랜잭션 안에서 부른다. */
    @Transactional
    public void forgetIfNoBowlsLeft(Long userId) {
        reports.findFirstByUserIdOrderByCutoffDateDesc(userId)
                .filter(latest ->
                        !logs.existsByUserIdAndDeletedAtIsNullAndVisitedAtLessThanEqual(userId, latest.getCutoffDate()))
                .ifPresent(latest -> reports.deleteForUser(userId));
    }

    public int drawMonthlyForAll(LocalDate today) {
        LocalDate monthEnd = lastDayOfPreviousMonth(today);
        int drawn = 0;
        for (Long userId : reports.findMonthlyDrawCandidates(monthEnd, TARGET)) {
            try {
                if (drawMonthly(userId, monthEnd).isPresent()) {
                    drawn++;
                }
            } catch (RuntimeException exception) {
                log.warn("매달 취향 리포트: 회원 {}의 리포트를 뽑지 못해 건너뜀", userId, exception);
            }
        }
        return drawn;
    }

    /** 지난달 말일 기준으로 새로 뽑았고 아직 안 연 리포트의 활성 회원에게 한 번만 보낸다. 빠진 뽑기를 먼저 채운다. */
    public int notifyMonthly(LocalDate today) {
        drawMonthlyForAll(today);
        LocalDate monthEnd = lastDayOfPreviousMonth(today);
        List<MobileTasteReport> pending =
                reports.findByKindAndCutoffDateAndNotifiedAtIsNullAndSeenAtIsNull(TasteReportKind.MONTHLY, monthEnd);
        if (pending.isEmpty()) {
            return 0;
        }
        // 보내기 전에 표시해, 발송 중에 실패하거나 다시 실행돼도 같은 알림을 두 번 보내지 않는다.
        newTransaction.executeWithoutResult(status -> reports.markNotified(
                pending.stream().map(MobileTasteReport::getId).toList(), now()));
        Map<Long, MobileAuthor> members = accounts.authors(
                pending.stream().map(MobileTasteReport::getUserId).toList());
        List<Long> recipients = new ArrayList<>();
        for (MobileAuthor member : members.values()) {
            if (member.active()) {
                recipients.add(member.id());
            }
        }
        YearMonth month = YearMonth.from(monthEnd);
        return push.send(recipients, locale -> MobileTasteReportPushMessages.monthly(locale, month));
    }

    private Optional<MobileTasteReport> drawFirst(Long userId, LocalDate today) {
        return insert(() -> {
            if (reports.existsByUserId(userId) || logs.countByUserIdAndDeletedAtIsNull(userId) < TARGET) {
                return null;
            }
            return draw(userId, TasteReportKind.FIRST, today);
        });
    }

    private Optional<MobileTasteReport> drawMonthly(Long userId, LocalDate monthEnd) {
        return insert(() -> {
            MobileTasteReport latest =
                    reports.findFirstByUserIdOrderByCutoffDateDesc(userId).orElse(null);
            if (latest == null || !latest.getCutoffDate().isBefore(monthEnd)) {
                return null;
            }
            long fresh = logs.countByUserIdAndDeletedAtIsNullAndVisitedAtAfterAndVisitedAtLessThanEqual(
                    userId, latest.getCutoffDate(), monthEnd);
            if (fresh < TARGET) {
                return null;
            }
            return draw(userId, TasteReportKind.MONTHLY, monthEnd);
        });
    }

    private MobileTasteReport draw(Long userId, TasteReportKind kind, LocalDate cutoff) {
        MobileTasteReportFigures figures = MobileTasteReportFigures.of(logs.findLiveVisitedUpTo(userId, cutoff));
        return reports.saveAndFlush(MobileTasteReport.draw(userId, kind, cutoff, figures, now()));
    }

    /** 별도 트랜잭션에서 뽑는다. 다른 요청이나 배치가 같은 기준일을 먼저 뽑았으면(UNIQUE 충돌) 이번에는 뽑지 않은 것으로 본다. */
    private Optional<MobileTasteReport> insert(Supplier<MobileTasteReport> drawing) {
        try {
            return Optional.ofNullable(newTransaction.execute(status -> drawing.get()));
        } catch (DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }

    private MobileTasteReportResult toResult(MobileTasteReport report) {
        List<Long> shopIds =
                report.getTopShops().stream().map(TasteTopShop::shopId).toList();
        Map<Long, MobileShopRef> refs = shops.findShopRefs(shopIds);
        Map<Long, String> photos = shops.findFirstImageUrls(shopIds);
        List<MobileTasteReportResult.TopShop> topShops = new ArrayList<>();
        for (TasteTopShop topShop : report.getTopShops()) {
            MobileShopRef ref = refs.get(topShop.shopId());
            if (ref == null) {
                continue;
            }
            topShops.add(new MobileTasteReportResult.TopShop(
                    new MobileTasteReportResult.Shop(String.valueOf(ref.id()), ref.name(), ref.branchName()),
                    topShop.visitCount(),
                    topShop.topMenu(),
                    topShop.topRamenType(),
                    topShop.lastVisitedAt(),
                    photos.get(topShop.shopId())));
        }
        return new MobileTasteReportResult(
                String.valueOf(report.getId()),
                report.getKind(),
                report.getCutoffDate(),
                report.getCreatedAt(),
                report.getSeenAt(),
                report.getBowlCount(),
                report.getShopCount(),
                report.getTypeCounts(),
                report.getAxisAverages(),
                topShops,
                report.getRecentLogIds().stream().map(String::valueOf).toList());
    }

    private static LocalDate lastDayOfPreviousMonth(LocalDate today) {
        return today.withDayOfMonth(1).minusDays(1);
    }

    private static int atLeastZero(long needed) {
        return (int) Math.max(0, needed);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SEOUL));
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
