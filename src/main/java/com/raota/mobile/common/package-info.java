/**
 * 모바일 v2 API의 공통 진입 계층이다.
 *
 * <p>v2 예외 처리기와 `/api/v2` 보안 설정을 둔다. 응답·오류·커서 형식은 global에 있다.
 * global이 mobile 패키지를 알 필요가 없도록, mobile 범위를 지정해야 하는 코드만 여기에 둔다.</p>
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.raota.mobile.common;
