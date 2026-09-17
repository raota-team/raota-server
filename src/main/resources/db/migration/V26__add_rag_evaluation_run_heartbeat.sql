-- 여러 앱 서버에서 RAG 평가 실행을 하나로 제한하고, 멈춘 실행을 복구하기 위한 컬럼
-- active_slot: QUEUED/RUNNING 실행만 'ACTIVE' 값을 가진다. UNIQUE이므로 활성 실행은 최대 1건이다.
-- heartbeat_at: 실행 worker가 주기적으로 갱신한다. 오래 갱신되지 않은 활성 실행은 FAILED로 복구한다.
ALTER TABLE tb_rag_evaluation_run
    ADD COLUMN heartbeat_at DATETIME(6) NULL,
    ADD COLUMN active_slot VARCHAR(16) NULL,
    ADD UNIQUE KEY uk_rag_evaluation_run_active_slot (active_slot),
    ADD KEY idx_rag_evaluation_run_status_heartbeat (status, heartbeat_at);

-- 기존 활성 실행이 여러 건일 수 있으므로 active_slot은 채우지 않는다.
-- heartbeat만 채워 두면 stale 복구 작업이 임계 시간 뒤 FAILED로 정리한다.
UPDATE tb_rag_evaluation_run
SET heartbeat_at = COALESCE(started_at, created_at)
WHERE status IN ('QUEUED', 'RUNNING');
