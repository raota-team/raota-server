package com.raota.agent.application.evaluation;

/**
 * 다른 요청이나 다른 서버가 이미 RAG 평가를 예약·실행 중이라 새 평가를 시작할 수 없음을 나타낸다.
 */
public class RagEvaluationAlreadyRunningException extends RuntimeException {

    public static final String CODE = "EVALUATION_ALREADY_RUNNING";

    public RagEvaluationAlreadyRunningException() {
        super("이미 실행 중인 RAG 평가가 있습니다.");
    }

}
