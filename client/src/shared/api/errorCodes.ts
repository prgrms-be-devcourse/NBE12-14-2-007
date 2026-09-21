/**
 * 백엔드 에러 코드. 문자열을 화면에 하드코딩하지 말고 여기서 가져다 쓴다.
 * 백엔드의 AuthExceptionCode / MemberExceptionCode 와 1:1로 맞춘다.
 */
export const ERROR_CODE = {
  /** 이메일 또는 비밀번호 불일치 */
  INVALID_CREDENTIALS: 'AUTH001',
  /** 권한 부족 */
  ACCESS_DENIED: 'AUTH100',
  /** Authorization 헤더 없음 */
  TOKEN_MISSING: 'AUTH200',
  /** 서명 불일치·형식 오류 */
  TOKEN_INVALID: 'AUTH201',
  /** 토큰 만료 → refresh 대상 */
  TOKEN_EXPIRED: 'AUTH202',

  /** 회원 없음 또는 탈퇴 */
  MEMBER_NOT_FOUND: 'MEMBER000',
  EMAIL_DUPLICATED: 'MEMBER001',
  NICKNAME_DUPLICATED: 'MEMBER002',
  /** 제재 등급이라 수정 불가 */
  MEMBER_RESTRICTED: 'MEMBER003',

  /** 요청 값 검증 실패 */
  INVALID_INPUT: 'COMMON_INVALID_INPUT',
} as const

export type ErrorCode = (typeof ERROR_CODE)[keyof typeof ERROR_CODE]
