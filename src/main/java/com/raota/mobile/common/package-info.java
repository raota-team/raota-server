/**
 * 모바일 v2 API가 공통으로 쓰는 표현 계층 뼈대(v2 응답 형식, 오류 코드, 응답 meta 채우기)를 둔다.
 *
 * <p>
 * 모든 v2 도메인 모듈이 사용하므로 OPEN 모듈로 등록한다. 도메인 로직, 엔티티, 저장소는 두지 않으며 다른 {@code mobile} 모듈을 참조하지
 * 않는다. web은 이 형식을 쓰지 않으므로 {@code global}에 두지 않는다.
 * </p>
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.raota.mobile.common;

import org.springframework.modulith.ApplicationModule;
