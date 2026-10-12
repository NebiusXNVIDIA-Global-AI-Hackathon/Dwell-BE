-- 공간별 부위 값이 늘어나 길이를 늘림 (ELECTRICAL_OUTLET_FIXTURE = 25자)
ALTER TABLE cases ALTER COLUMN affected_area TYPE VARCHAR(30);

-- Entire Unit은 부위 선택 단계를 건너뛰므로 비워 둘 수 있음
ALTER TABLE cases ALTER COLUMN affected_area DROP NOT NULL;

-- 부위에서 Other를 고르면 직접 입력한 값
ALTER TABLE cases ADD COLUMN affected_area_other VARCHAR(50);

-- 심각도는 AI 분석 후에 정해지므로 생성 시점에는 비워 둠
ALTER TABLE cases ALTER COLUMN severity DROP NOT NULL;

-- DOCX MIME 타입(71자)이 들어가도록 길이를 늘림
ALTER TABLE file ALTER COLUMN mime_type TYPE VARCHAR(100);