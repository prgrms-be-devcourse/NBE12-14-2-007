/** 백엔드 AuthRequest.Login 과 1:1 */
export interface LoginRequest {
  email: string
  password: string
}

/** 백엔드 AuthRequest.Signup 과 1:1 */
export interface SignupRequest {
  email: string
  password: string
  nickname: string
  /** 선택 */
  phone?: string
}

/** 백엔드 TokenResponse 와 1:1 */
export interface TokenResponse {
  accessToken: string
  tokenType: string
}
