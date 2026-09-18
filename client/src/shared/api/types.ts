/**
 * 백엔드 공통 응답 봉투. 성공·실패 모두 이 모양으로 내려온다.
 *
 * 성공 응답은 client.ts 인터셉터가 봉투를 벗겨서 data 만 넘기므로,
 * 화면 코드에서는 이 타입을 직접 쓸 일이 거의 없다.
 */
export interface ApiResponse<T> {
  success: boolean
  /** 성공은 항상 "0000", 실패는 도메인별 에러 코드 */
  code: string
  message: string
  data: T
}

/**
 * 실패 응답을 담아 던지는 에러.
 * 화면에서는 code 로 분기하고 message 를 그대로 보여주면 된다.
 */
export class ApiError extends Error {
  /** 백엔드 에러 코드. errorCodes.ts 의 ERROR_CODE 와 비교해서 분기한다 */
  readonly code: string
  readonly status: number

  // tsconfig 의 erasableSyntaxOnly 때문에 생성자 파라미터 프로퍼티는 쓸 수 없다.
  constructor(code: string, message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}
