-- 매니저 신청(manager) 도메인 제거에 따른 테이블 삭제.
-- 매니저 권한은 member.role(ROLE_MANAGER)로만 관리한다.
DROP TABLE IF EXISTS manager;
