package com.raota.mobile.common.copy;

/** v1 → v2 복사에서 원본 행을 옮기지 않은 이유다. 대조표의 {@code excluded} 키로 나간다. */
public enum LegacyCopyExclusion {

    /** 탈퇴한 v1 회원이거나 그 회원의 소셜 계정이다. */
    WITHDRAWN_MEMBER,

    /** v1 회원이 없는 소셜 계정이다. */
    ORPHAN,

    /** 작성자·소유자가 v2 회원으로 복사되지 않았다. */
    MEMBER_NOT_COPIED,

    /** 매장이 v2에 없다. */
    SHOP_NOT_COPIED,

    /** 기록이 v2에 없다. */
    LOG_NOT_COPIED,

    /** 같은 ID의 v2 행이 이 원본에서 복사한 행이 아니다. */
    ID_CONFLICT,

    /** 같은 v2 회원에 같은 제공자 계정이 이미 있다. */
    DUPLICATE_PROVIDER,

    /** 같은 제공자 계정이 다른 v2 회원에 이미 연결돼 있다. */
    DUPLICATE_SUBJECT,

    /** v2가 지원하지 않는 소셜 제공자다. */
    UNKNOWN_PROVIDER,

    /** 삭제된 v1 기록이다. */
    DELETED_LOG,

    /** v2 재방문 의사로 옮길 수 없는 값이다. */
    UNKNOWN_REVISIT,

    /** v2 컬럼 길이를 넘는다. */
    TOO_LONG,

    /** 사진이 없는 기록이다. */
    NO_IMAGE,

    /** 취소된 v1 북마크다. */
    CANCELED,

    /** 같은 회원·매장의 앞선 활성 북마크가 있다. */
    DUPLICATE_BOOKMARK

}
