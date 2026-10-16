package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogCommentResult;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogComment;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogCommentRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 댓글 작성·삭제와 기록의 댓글 수가 함께 커밋되도록 묶고, 댓글 목록을 등록순으로 읽는다. */
@Service
public class MobileRamenLogCommentService {

    private static final int PREVIEW_SIZE = 20;

    private final MobileRamenLogCommentRepository comments;

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogVisibility visibility;

    private final MobileAccountRamenLogFacade accounts;

    private final Clock clock;

    private final TransactionTemplate createTransaction;

    public MobileRamenLogCommentService(MobileRamenLogCommentRepository comments, MobileRamenLogRepository logs,
            MobileRamenLogVisibility visibility, MobileAccountRamenLogFacade accounts, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.comments = comments;
        this.logs = logs;
        this.visibility = visibility;
        this.accounts = accounts;
        this.clock = clock;
        this.createTransaction = new TransactionTemplate(transactionManager);
        this.createTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogCommentResult> list(Long logId, Long viewerId, String rawCursor, int size) {
        visibility.visible(logId, viewerId);
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        if (cursor == null) {
            return page(logId, viewerId, null, null, size);
        }
        return page(logId, viewerId, parseInstant(cursor), cursor.id(), size);
    }

    /** 기록 상세에 붙이는 첫 댓글 묶음이다. 기록을 볼 수 있는지는 호출한 쪽이 확인한다. */
    CursorPage<MobileRamenLogCommentResult> preview(Long logId, Long viewerId) {
        return page(logId, viewerId, null, null, PREVIEW_SIZE);
    }

    public MobileRamenLogCommentResult create(Long logId, Long userId, String key, String content) {
        MobileRamenLogComment previous = comments.findByUserIdAndIdempotencyKey(userId, key).orElse(null);
        if (previous != null) {
            return replay(previous, logId, userId);
        }
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Long id;
        try {
            // UNIQUE 충돌은 이 트랜잭션만 되돌린 뒤 별도 읽기로 먼저 커밋된 댓글을 조회한다.
            id = createTransaction.execute(status -> {
                MobileRamenLog log = visibility.visibleForUpdate(logId, userId);
                MobileRamenLogComment comment = comments
                    .saveAndFlush(MobileRamenLogComment.create(logId, userId, content, key, now));
                log.addComment();
                return comment.getId();
            });
        }
        catch (DataIntegrityViolationException exception) {
            MobileRamenLogComment replayed = comments.findByUserIdAndIdempotencyKey(userId, key)
                .orElseThrow(() -> exception);
            return replay(replayed, logId, userId);
        }
        return view(comments.findById(id).orElseThrow(this::notFound), userId);
    }

    /** 댓글 행을 잠가 같은 댓글을 동시에 지워도 기록의 댓글 수는 한 번만 줄어든다. */
    @Transactional
    public void delete(Long commentId, Long userId) {
        MobileRamenLogComment comment = comments.findByIdForUpdate(commentId)
            .filter(found -> found.getDeletedAt() == null)
            .orElseThrow(this::notFound);
        if (!comment.getUserId().equals(userId)) {
            throw new MobileException(MobileErrorCode.FORBIDDEN, "작성자만 댓글을 삭제할 수 있습니다.");
        }
        comment.softDelete(Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        logs.findByIdForUpdate(comment.getRamenLogId()).ifPresent(MobileRamenLog::removeComment);
    }

    /** 같은 키의 재시도는 처음 만든 댓글을 돌려준다. 다른 기록에 쓰였거나 이미 지운 댓글의 키는 409다. */
    private MobileRamenLogCommentResult replay(MobileRamenLogComment previous, Long logId, Long userId) {
        if (!previous.getRamenLogId().equals(logId) || previous.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 사용한 요청 키입니다.");
        }
        return view(previous, userId);
    }

    /**
     * 로그인한 회원이 차단한 작성자와 신고한 댓글은 쿼리에서 빼고, 활성 회원이 아닌 작성자(탈퇴 유예·정지)의 댓글은 건너뛴 뒤 걸러낸 만큼 다음 묶음을
     * 더 읽어 페이지를 채운다.
     */
    private CursorPage<MobileRamenLogCommentResult> page(Long logId, Long viewerId, Instant afterAt, Long afterId,
            int size) {
        List<MobileRamenLogComment> visible = new ArrayList<>();
        Map<Long, MobileAuthor> authors = new HashMap<>();
        while (true) {
            List<MobileRamenLogComment> batch = comments.findPage(logId, viewerId, afterAt, afterId,
                    PageRequest.of(0, size + 1));
            Set<Long> unknown = batch.stream()
                .map(MobileRamenLogComment::getUserId)
                .filter(id -> !authors.containsKey(id))
                .collect(Collectors.toSet());
            authors.putAll(accounts.authors(unknown));
            for (MobileRamenLogComment comment : batch) {
                MobileAuthor author = authors.get(comment.getUserId());
                if (author != null && author.active()) {
                    visible.add(comment);
                }
            }
            if (visible.size() > size || batch.size() <= size) {
                break;
            }
            afterAt = batch.getLast().getCreatedAt();
            afterId = batch.getLast().getId();
        }
        CursorPage<MobileRamenLogComment> page = CursorPage.of(visible, size,
                comment -> Cursor.of(comment.getCreatedAt(), comment.getId()));
        List<MobileRamenLogCommentResult> items = page.items()
            .stream()
            .map(comment -> MobileRamenLogViews.comment(comment, authors.get(comment.getUserId()), viewerId))
            .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    private MobileRamenLogCommentResult view(MobileRamenLogComment comment, Long viewerId) {
        MobileAuthor author = accounts.authors(List.of(comment.getUserId())).get(comment.getUserId());
        if (author == null) {
            throw notFound();
        }
        return MobileRamenLogViews.comment(comment, author, viewerId);
    }

    private Instant parseInstant(Cursor cursor) {
        try {
            return Instant.parse(cursor.sortValue());
        }
        catch (DateTimeParseException exception) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
        }
    }

    private MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다.");
    }

}
