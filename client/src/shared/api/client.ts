import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ERROR_CODE } from './errorCodes'
import { tokenStore } from './tokenStore'
import { ApiError, type ApiResponse } from './types'

/** 재시도 여부를 표시하기 위한 확장. 같은 요청을 무한히 재시도하지 않도록 쓴다. */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // Refresh Token 이 HttpOnly 쿠키로 오간다. 이게 없으면 쿠키가 전송되지 않아
  // 로그인은 되는데 /auth/refresh 가 계속 401 이 난다.
  withCredentials: true,
})

// ── 요청: Access Token 을 헤더에 붙인다 ─────────────────────────
client.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 진행 중인 refresh 요청. 동시에 여러 요청이 401 을 받아도
 * refresh 는 한 번만 나가고 나머지는 이 Promise 에 붙는다.
 */
let refreshing: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  refreshing ??= axios
    .post<ApiResponse<{ accessToken: string }>>(
      `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth/refresh`,
      null,
      { withCredentials: true },
    )
    .then((res) => {
      const token = res.data.data.accessToken
      tokenStore.set(token)
      return token
    })
    .finally(() => {
      refreshing = null
    })

  return refreshing
}

// ── 응답: 봉투를 벗기고, 401 이면 refresh 후 재시도 ──────────────
client.interceptors.response.use(
  // 성공: { success, code, message, data } 에서 data 만 넘긴다.
  // 덕분에 화면 코드에서 res.data.data 를 쓸 일이 없다.
  //
  // axios 타입상 인터셉터는 AxiosResponse 를 돌려줘야 하지만 우리는 일부러 벗겨서 넘긴다.
  // 그래서 캐스팅이 필요하고, 호출부는 client.get<unknown, T>() 처럼
  // 두 번째 제네릭(인터셉터 통과 후 타입)에 실제 타입을 적는다.
  (response: AxiosResponse<ApiResponse<unknown>>) =>
    response.data?.data as unknown as AxiosResponse,

  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RetriableConfig | undefined
    const body = error.response?.data
    const status = error.response?.status ?? 0

    // 토큰 만료면 재발급 후 원래 요청을 한 번만 다시 보낸다.
    const isExpired = body?.code === ERROR_CODE.TOKEN_EXPIRED
    if (isExpired && config && !config._retried) {
      config._retried = true
      try {
        const token = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${token}`
        return client.request(config)
      } catch {
        // refresh 도 실패 = 세션이 완전히 끊긴 상태
        tokenStore.clear()
      }
    }

    // 백엔드가 준 code/message 를 그대로 살려서 던진다.
    // 네트워크 오류처럼 응답 자체가 없으면 빈 코드로 채운다.
    throw new ApiError(body?.code ?? '', body?.message ?? error.message, status)
  },
)
