package com.raota.domain.ramenShop.model;

import com.raota.domain.member.model.MemberProfile;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tb_ramen_shop_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RamenShopReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_shop_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private RamenShop ramenShop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private MemberProfile memberProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RamenShopReportType reportType;

    @Column(nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    private LocalDateTime reportedAt;

    public static RamenShopReport create(RamenShop shop, MemberProfile member, RamenShopReportType type, String content) {
        return RamenShopReport.builder()
                .ramenShop(shop)
                .memberProfile(member)
                .reportType(type)
                .content(content)
                .build();
    }
}
