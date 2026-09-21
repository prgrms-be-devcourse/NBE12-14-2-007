export const regions: Record<string, string> = {
  SEOUL: "서울특별시",
  BUSAN: "부산광역시",
  DAEGU: "대구광역시",
  INCHEON: "인천광역시",
  GWANGJU: "광주광역시",
  DAEJEON: "대전광역시",
  ULSAN: "울산광역시",
  SEJONG: "세종특별자치시",
  GANGWON: "강원특별자치도",
  CHUNGBUK: "충청북도",
  CHUNGNAM: "충청남도",
  JEONBUK: "전북특별자치도",
  JEONNAM: "전라남도",
  GYEONGBUK: "경상북도",
  GYEONGNAM: "경상남도",
  JEJU: "제주특별자치도",
  GYEONGGI: "경기도",
  ...Object.fromEntries(
    [
      ["SUWON", "수원시"],
      ["GOYANG", "고양시"],
      ["YONGIN", "용인시"],
      ["SEONGNAM", "성남시"],
      ["BUCHEON", "부천시"],
      ["HWASEONG", "화성시"],
      ["ANSAN", "안산시"],
      ["NAMYANGJU", "남양주시"],
      ["ANYANG", "안양시"],
      ["PYEONGTAEK", "평택시"],
      ["SIHEUNG", "시흥시"],
      ["PAJU", "파주시"],
      ["UIJEONGBU", "의정부시"],
      ["GIMPO", "김포시"],
      ["GWANGJU", "광주시"],
      ["GWANGMYEONG", "광명시"],
      ["GUNPO", "군포시"],
      ["HANAM", "하남시"],
      ["OSAN", "오산시"],
      ["YANGJU", "양주시"],
      ["ICHEON", "이천시"],
      ["GURI", "구리시"],
      ["ANSEONG", "안성시"],
      ["POCHEON", "포천시"],
      ["UIWANG", "의왕시"],
      ["YANGPYEONG", "양평군"],
      ["YEOJU", "여주시"],
      ["DONGDUCHEON", "동두천시"],
      ["GWACHEON", "과천시"],
      ["GAPYEONG", "가평군"],
      ["YEONCHEON", "연천군"],
    ].map(([key, name]) => [`GYEONGGI_${key}`, `경기도 ${name}`]),
  ),
};
export const categories = ["전체", "축제", "공연", "전시", "체험", "플리마켓"];
export const roleNames: Record<string, string> = {
  ROLE_WARNING: "활동 제한",
  ROLE_UNVERIFIED: "새로운 이웃",
  ROLE_NORMAL: "일반 회원",
  ROLE_TRUSTED: "신뢰 회원",
  ROLE_ADMIN: "관리자",
};
export function dateText(value?: string | null) {
  return value ? value.slice(0, 10).replaceAll("-", ".") : "일정 미정";
}
export function period(start?: string, end?: string) {
  return start?.slice(0, 10) === end?.slice(0, 10)
    ? dateText(start)
    : `${dateText(start)} — ${dateText(end)}`;
}
export function eventState(start: string, end: string) {
  const now = Date.now();
  return new Date(end).getTime() < now
    ? "종료"
    : new Date(start).getTime() > now
      ? "개최 예정"
      : "진행 중";
}
export function safeUrl(value?: string | null): string | undefined {
  if (!value) return undefined;
  try {
    const u = new URL(value);
    return ["https:", "http:"].includes(u.protocol) ? u.href : undefined;
  } catch {
    return undefined;
  }
}
export function imageUrl(value?: string | null) {
  if (!value) return undefined;
  if (value.startsWith("/images/") || value.startsWith("blob:")) return value;
  return (
    safeUrl(value) ||
    (import.meta.env.VITE_IMAGE_BASE_URL
      ? safeUrl(
          `${import.meta.env.VITE_IMAGE_BASE_URL.replace(/\/$/, "")}/${value}`,
        )
      : undefined)
  );
}
export function errorText(error: unknown) {
  return error instanceof Error
    ? error.message
    : "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.";
}
