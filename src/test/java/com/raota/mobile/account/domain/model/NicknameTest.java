package com.raota.mobile.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import org.junit.jupiter.api.Test;

class NicknameTest {

    @Test
    void 전각_영문과_앞뒤_공백은_표시용_닉네임에서_정리된다() {
        Nickname nickname = Nickname.of("  ＲａＭｅＮ_９  ");

        assertThat(nickname.display()).isEqualTo("RaMeN_9");
        assertThat(nickname.normalized()).isEqualTo("ramen_9");
    }

    @Test
    void 대문자와_소문자는_중복_판별에_같은_닉네임이다() {
        assertThat(Nickname.of("Ramen").normalized()).isEqualTo(Nickname.of("ramen").normalized());
    }

    @Test
    void 길이는_자바_문자수가_아니라_유니코드_코드포인트로_판정한다() {
        assertThat(Nickname.of("가".repeat(12)).display()).isEqualTo("가".repeat(12));
        assertInvalid("가");
        assertInvalid("가".repeat(13));
    }

    @Test
    void 기호와_이모지와_빈_입력은_허용하지_않는다() {
        assertInvalid("라멘!");
        assertInvalid("라멘🍜");
        assertInvalid(null);
    }

    private void assertInvalid(String raw) {
        assertThatThrownBy(() -> Nickname.of(raw)).isInstanceOfSatisfying(MobileException.class, exception -> {
            assertThat(exception.code()).isEqualTo(MobileErrorCode.VALIDATION_ERROR);
            assertThat(exception.getMessage()).isEqualTo("닉네임은 2~12자의 한글·영문·숫자·밑줄만 쓸 수 있습니다.");
        });
    }

}
