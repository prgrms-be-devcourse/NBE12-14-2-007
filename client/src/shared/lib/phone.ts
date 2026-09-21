/**
 * 휴대폰 번호 표시·저장 규칙.
 *
 * 저장(서버 전송)은 숫자만: 01023232323
 * 표시(입력창)는 하이픈 포함: 010-2323-2323
 *
 * 사용자는 하이픈을 칠 필요가 없다. 숫자를 치면 화면에서 자동으로 붙는다.
 */

/** 서버로 보낼 값. 숫자만 남기고 11자리로 자른다. */
export function toDigits(value: string): string {
  return value.replace(/\D/g, '').slice(0, 11)
}

/**
 * 입력창에 보여줄 값. 입력 중간 단계에서도 자연스럽게 끊어준다.
 *
 * 10자리(011-123-4567 같은 옛 번호)는 3-3-4,
 * 11자리(010-1234-5678)는 3-4-4 로 끊는다.
 */
export function formatPhone(value: string): string {
  const digits = toDigits(value)

  if (digits.length <= 3) {
    return digits
  }
  if (digits.length <= 7) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`
  }
  if (digits.length === 10) {
    return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

/** 백엔드 @ValidPhone 과 동일한 규칙. 숫자만 들어온다고 가정한다. */
const PHONE_PATTERN = /^01[016789]\d{7,8}$/

/** 비어 있으면 통과(선택 입력). 적었으면 형식을 지켜야 한다. */
export function isValidPhone(digits: string): boolean {
  return digits === '' || PHONE_PATTERN.test(digits)
}
