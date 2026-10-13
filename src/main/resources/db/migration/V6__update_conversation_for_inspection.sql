-- Check My Unit의 Talk to Deaver로 만든 채팅방 (type = 'INSPECTION')이 가리키는 검사 결과
ALTER TABLE conversation ADD COLUMN inspection_id BIGINT REFERENCES inspection_result (id) ON DELETE CASCADE;

-- 검사 결과당 채팅방 1개
CREATE UNIQUE INDEX uk_conversation_inspection ON conversation (inspection_id) WHERE type = 'INSPECTION';

-- 사용자가 채팅방을 마지막으로 연 시각 (채팅 목록 안 읽음 표시)
ALTER TABLE conversation ADD COLUMN last_read_at TIMESTAMP;
