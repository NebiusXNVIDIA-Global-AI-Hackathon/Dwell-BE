-- 이름 컬럼 삭제
ALTER TABLE users DROP COLUMN first_name;
ALTER TABLE users DROP COLUMN last_name;

-- 언어 선택 화면이 없으므로 기본값 'en'
ALTER TABLE users ALTER COLUMN preferred_language SET DEFAULT 'en';

-- 주소 화면에서 Apt/unit은 선택 입력
ALTER TABLE user_place ALTER COLUMN unit DROP NOT NULL;