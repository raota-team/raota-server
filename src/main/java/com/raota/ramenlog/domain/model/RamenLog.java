package com.raota.ramenlog.domain.model;

import com.raota.account.domain.member.model.MemberProfile;
import com.raota.ramenshop.domain.model.RamenShop;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "tb_ramen_log")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class RamenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private MemberProfile author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_shop_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private RamenShop ramenShop;

    @Column(name = "menu_name", nullable = false, length = 1000)
    private String menuName;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "ramen_type", nullable = false, length = 50)
    @Builder.Default
    private String ramenType = "기타";

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "broth_notes", columnDefinition = "json")
    @Builder.Default
    private List<String> brothNotes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "noodle_notes", columnDefinition = "json")
    @Builder.Default
    private List<String> noodleNotes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seasoning_notes", columnDefinition = "json")
    @Builder.Default
    private List<String> seasoningNotes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topping_notes", columnDefinition = "json")
    @Builder.Default
    private List<String> toppingNotes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "revisit", nullable = false, length = 30)
    @Builder.Default
    private RevisitIntention revisit = RevisitIntention.SOMETIMES;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private long likeCount = 0;

    @Column(name = "visited_at", nullable = false)
    @Builder.Default
    private LocalDate visitedAt = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public void update(
            RamenShop ramenShop,
            String menuName,
            String ramenType,
            String imageUrl,
            String note,
            List<String> brothNotes,
            List<String> noodleNotes,
            List<String> seasoningNotes,
            List<String> toppingNotes,
            RevisitIntention revisit,
            LocalDate visitedAt,
            boolean isPublic
    ) {
        this.ramenShop = ramenShop;
        this.menuName = menuName;
        this.ramenType = ramenType;
        this.imageUrl = imageUrl;
        this.note = note;
        this.brothNotes = copyOf(brothNotes);
        this.noodleNotes = copyOf(noodleNotes);
        this.seasoningNotes = copyOf(seasoningNotes);
        this.toppingNotes = copyOf(toppingNotes);
        this.revisit = revisit;
        this.visitedAt = visitedAt;
        this.isPublic = isPublic;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void increaseLikeCount() {
        likeCount++;
    }

    public void decreaseLikeCount() {
        likeCount = Math.max(0, likeCount - 1);
    }

    private static List<String> copyOf(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
