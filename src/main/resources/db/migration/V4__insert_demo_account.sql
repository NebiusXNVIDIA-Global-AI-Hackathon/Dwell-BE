-- 데모 계정 (비밀번호 없음: demo API로만 로그인 가능)
INSERT INTO users (email, password_hash, preferred_language, role, nickname, auth_provider, created_at, modified_at)
VALUES ('demo01@example.com', NULL, 'en', 'USER', 'demo1', 'LOCAL', NOW(), NOW());

-- 데모 계정의 건물 (이미 있으면 건너뜀)
INSERT INTO building (bbl, street, city, state, zip, created_at)
VALUES ('1018800031', '254 W 107th St', 'New York', 'NY', '10025', NOW())
    ON CONFLICT (bbl) DO NOTHING;

-- 데모 계정의 집 정보
INSERT INTO user_place (user_id, building_id, unit, floor, unit_line, lease_in_own_name, created_at, modified_at)
SELECT u.id, b.id, 'Apt 4B', 4, 'B', TRUE, NOW(), NOW()
FROM users u, building b
WHERE u.email = 'demo01@example.com'
  AND b.bbl = '1018800031';