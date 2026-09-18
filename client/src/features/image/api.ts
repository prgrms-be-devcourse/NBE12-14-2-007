import { client } from '@/shared/api/client'

/** 백엔드 ImageController.ImageType 과 동일. 버킷 안의 폴더가 된다. */
export type ImageType = 'POST' | 'PROFILE' | 'INQUIRY' | 'FESTIVAL'

/** 백엔드 ImageController.UploadInfo 와 1:1 */
export interface UploadInfo {
  /** DB에 저장할 값. URL 이 아니라 이 key 를 보낸다 */
  key: string
  /** 화면에 바로 띄울 수 있는 공개 URL */
  url: string
}

/** 백엔드가 허용하는 형식과 크기. 서버에서도 검증하지만 미리 걸러 왕복을 아낀다. */
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024

/**
 * 이미지를 업로드하고 저장 key 를 받는다.
 * 로그인이 필요하다. 비로그인 상태로 호출하면 401 이 난다.
 */
export async function uploadImage(file: File, type: ImageType): Promise<UploadInfo> {
  const form = new FormData()
  form.append('file', file)

  // Content-Type(multipart 경계값)은 axios 가 FormData 를 보고 알아서 채운다.
  return client.post<unknown, UploadInfo>(`/api/v1/images?type=${type}`, form)
}

/** 업로드 전 검사. 문제가 있으면 메시지를, 없으면 null 을 돌려준다. */
export function validateImage(file: File): string | null {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return 'JPG, PNG, WEBP 형식만 올릴 수 있습니다.'
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return '이미지는 5MB 이하만 올릴 수 있습니다.'
  }
  return null
}
