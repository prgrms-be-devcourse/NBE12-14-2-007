/** 백엔드 MemberRole 과 동일. 선언 순서가 곧 권한 계층이다. */
export type MemberRole =
  'ROLE_WARNING' | 'ROLE_UNVERIFIED' | 'ROLE_NORMAL' | 'ROLE_TRUSTED' | 'ROLE_ADMIN'

/**
 * 백엔드 MemberRequest.UpdateMyPage 와 1:1.
 * PATCH 라서 보낸 필드만 반영된다. 생략하면 그대로 두고, 빈 문자열이면 지운다.
 */
export interface UpdateMyPageRequest {
  nickname?: string
  phone?: string
  /** 업로드 API 가 돌려준 key. 공개 URL 이 아니다 */
  profileImg?: string
}

/** 백엔드 MemberResponse.MyPageInfo 와 1:1 */
export interface MyPageInfo {
  id: string
  email: string
  nickname: string
  /** 공개 URL. 이미지가 없으면 null */
  profileImg: string | null
  phone: string | null
  role: MemberRole
  createdAt: string
  updatedAt: string
}
