export type Mode = "preview" | "api";
export type Role =
  | "ROLE_WARNING"
  | "ROLE_UNVERIFIED"
  | "ROLE_RECOGNIZED"
  | "ROLE_TRUSTED"
  | "ROLE_ADMIN";
export interface Member {
  id: string;
  email: string;
  nickname: string;
  profileImg: string | null;
  phone: string | null;
  role: Role;
  createdAt: string;
  updatedAt: string;
}
export interface Festival {
  festivalId: number;
  title: string;
  category: string;
  instNm: string | null;
  manager: string | null;
  festivalContent: string | null;
  referenceUrl: string | null;
  region: string;
  regionDetail: string;
  imgUrl: string | null;
  beginDe: string;
  endDe: string;
  eventTmInfo: string | null;
  partcptExpnInfo: string | null;
  telnoInfo: string | null;
  hostInstNm: string | null;
  writngDe: string | null;
  status: "OPEN" | "CLOSED";
}
export interface EventView extends Festival {
  source: "PUBLIC" | "MEMBER";
  accuracyVote?: FestivalAccuracyVote;
  likeCount?: number;
  likedByMe?: boolean;
  submitter?: FestivalMember | null;
  preview?: boolean;
  submissionId?: string;
}
export interface FestivalMember {
  id: string;
  nickname: string;
  profileImg: string | null;
  role: Role;
}
export interface FestivalSearchInput {
  keyword?: string;
  region?: string;
  providerType?: "PUBLIC" | "MEMBER";
  category?: string;
  date?: string;
  excludeClosed?: boolean;
  page?: number;
  sort?: "soon" | "name";
}
export interface FestivalSearchItem {
  festivalId: number;
  providerType: "PUBLIC" | "MEMBER";
  title: string;
  category: string;
  instNm: string | null;
  imgUrl: string | null;
  beginDe: string;
  endDe: string;
  region: string;
  status: "OPEN" | "CLOSED";
}
// GET /api/v1/festivals/{festivalId} 응답 그대로의 모양
export interface FestivalDetailItem {
  festivalId: number;
  providerType: "PUBLIC" | "MEMBER";
  title: string;
  category: string;
  festivalContent: string | null;
  instNm: string | null;
  hostInstNm: string | null;
  imgUrl: string | null;
  url: string | null;
  hmpgUrl: string | null;
  referenceUrl: string | null;
  beginDe: string;
  endDe: string;
  eventTmInfo: string | null;
  partcptExpnInfo: string | null;
  telnoInfo: string | null;
  region: string;
  regionDetail: string | null;
  status: "OPEN" | "CLOSED";
  accurateCount: number;
  inaccurateCount: number;
  myVote: "ACCURATE" | "INACCURATE" | null;
  likeCount: number;
  likedByMe: boolean;
  member: FestivalMember | null;
}
export interface FestivalAccuracyVote {
  accurateCount: number;
  inaccurateCount: number;
  myVote: "ACCURATE" | "INACCURATE" | null;
}
export type WeatherCondition =
  | "SUNNY"
  | "CLOUDY"
  | "RAIN"
  | "RAIN_SNOW"
  | "SNOW"
  | "UNKNOWN";
export interface WeatherView {
  condition: WeatherCondition;
  precipitationProbability: number | null;
  date: string;
}
export interface SubmissionInput {
  title: string;
  category: string;
  festivalContent?: string;
  region: string;
  regionDetail?: string;
  beginDe: string;
  endDe: string;
  eventTmInfo?: string;
  instNm?: string;
  referenceUrl: string;
  imgUrl?: string;
  partcptExpnInfo?: string;
  telnoInfo?: string;
  hostInstNm?: string;
}
export interface SubmissionSummary {
  festivalSubmissionId: string;
  festivalId: number;
  title: string;
  region: string;
  regionDetail: string;
  beginDe: string;
  endDe: string;
  createdAt: string;
  status: "OPEN" | "CLOSED";
}
export interface SubmissionDetail {
  submission: {
    festivalSubmissionId: string;
    createdAt: string;
    updatedAt: string;
    festival: Omit<Festival, "manager">;
  };
}
export interface PostInput {
  title: string;
  content: string;
  thumbnail?: string;
}
export type PostSearchType = "TITLE" | "MEMBER_NICKNAME" | "FESTIVAL_TITLE";
export interface PostSearch {
  type?: PostSearchType;
  keyword?: string;
}
export interface AdminPostQuery extends PostSearch {
  page?: number;
  size?: number;
  sort?: "createdAt,desc" | "createdAt,asc";
}
export interface PostSummary {
  id: string;
  member: { id: string; nickname: string; profileImg: string | null };
  festivalId: number;
  festivalTitle: string;
  title: string;
  thumbnail: string | null;
  date: string;
  // Preview enrichment; the current backend list DTO does not include this.
  likeCount?: number;
}
export interface PostDetail extends PostSummary {
  content: string;
}
export interface AdminPostSummary extends PostSummary {
  deletedAt: string | null;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
  first: boolean;
}
export interface Comment {
  id: number;
  postId: string;
  memberId: string;
  nickname: string;
  profile_img: string | null;
  content: string;
  date: string;
}
export interface InquiryInput {
  category: "QUESTION" | "REPORT";
  title: string;
  content: string;
  img?: string;
}
export interface Inquiry extends Omit<InquiryInput, "img"> {
  id: string;
  img: string | null;
  status: "PENDING" | "ANSWERED";
  answer: string | null;
  createdAt: string;
}
/** 관리자 화면에 보이는 작성자 정보. 서버 MemberResponse.AdminInfo와 같은 모양. */
export interface AdminMemberInfo {
  id: string;
  email: string;
  nickname: string;
  profileImg: string | null;
  phone: string | null;
  role: Role;
  createdAt: string;
  updatedAt: string;
  /** 탈퇴 시각. 탈퇴하지 않았으면 null */
  deletedAt: string | null;
}
/**
 * 관리자 문의 목록의 한 줄. 서버 AdminInquiryResponse.ListItem과 같은 모양.
 * 목록에는 본문(content)과 답변(answer)이 없다. 상세 조회 API가 따로 필요하다.
 */
export interface AdminInquiryListItem {
  id: string;
  category: "QUESTION" | "REPORT";
  title: string;
  /** 작성자 회원이 남아있지 않으면 null */
  writer: AdminMemberInfo | null;
  status: "PENDING" | "ANSWERED";
  createdAt: string;
  /** 삭제 시각. 삭제되지 않았으면 null */
  deletedAt: string | null;
}
/**
 * 관리자 문의 상세. 서버 AdminInquiryResponse.Detail과 같은 모양.
 * 목록에 없는 본문·첨부·답변이 여기에 있다.
 */
export interface AdminInquiryDetail extends AdminInquiryListItem {
  content: string;
  /** 첨부 이미지 공개 URL. 없으면 null */
  img: string | null;
  /** 아직 답변 전이면 null */
  answer: string | null;
  updatedAt: string;
}
/** 공공 행사 수동 동기화 결과. 서버 FestivalResponse.SyncResponse와 같은 모양. */
export interface SyncResult {
  /** 종료 처리(CLOSED)된 행사 */
  closedFestivals: { festivalId: number; title: string }[];
  /** 새로 저장된 행사 */
  savedFestivals: { festivalId: number; title: string }[];
}
/**
 * 운영 대시보드 집계.
 *
 * 전용 API가 없어서 각 목록 API에 size=1을 던지고 totalElements만 쓴다.
 * 서버는 COUNT만 돌리므로 비용은 전용 API와 같고, HTTP 왕복만 더 든다.
 * 행사·후기에 노출 상태가 생기면 GET /admin/overview 하나로 합칠 것.
 */
export interface AdminStats {
  memberTotal: number;
  memberTrusted: number;
  memberWarning: number;
  inquiryPending: number;
  inquiryReport: number;
  festivalTotal: number;
  festivalOpen: number;
  postTotal: number;
}
/**
 * 관리자 행사 상세. 서버 FestivalResponse.DetailResponse와 같은 모양.
 *
 * 공개 화면이 쓰는 FestivalDetailItem과 따로 두는 이유:
 * 그쪽 변환기(toDetailEvent)가 providerType과 festivalContent를 버리고
 * 값을 고정해 버려서 관리자 화면에 필요한 정보가 남지 않는다.
 */
export interface AdminFestivalDetail {
  festivalId: number;
  providerType: "PUBLIC" | "MEMBER";
  title: string;
  category: string;
  /** 행사 소개 본문 */
  festivalContent: string | null;
  instNm: string | null;
  hostInstNm: string | null;
  imgUrl: string | null;
  url: string | null;
  hmpgUrl: string | null;
  referenceUrl: string | null;
  beginDe: string;
  endDe: string;
  eventTmInfo: string | null;
  partcptExpnInfo: string | null;
  telnoInfo: string | null;
  region: string;
  regionDetail: string | null;
  status: "OPEN" | "CLOSED";
}
/**
 * 관리자 행사 목록의 한 줄. 서버 AdminFestivalResponse.ListItem과 같은 모양.
 * 공개 목록과 달리 deletedAt이 있어 삭제된 행사를 구분할 수 있다.
 */
export interface AdminFestivalListItem {
  festivalId: number;
  providerType: "PUBLIC" | "MEMBER";
  title: string;
  category: string;
  instNm: string | null;
  imgUrl: string | null;
  beginDe: string;
  endDe: string;
  region: string;
  status: "OPEN" | "CLOSED";
  /** 삭제 시각. 삭제되지 않았으면 null */
  deletedAt: string | null;
}
/**
 * 관리자 행사 수정 요청. 보낸 값으로 전부 덮어쓴다.
 * 상세를 먼저 불러와 채운 뒤 통째로 보낸다.
 * 출처(providerType)와 진행 상태(status)는 서버가 정하므로 보내지 않는다.
 */
export interface AdminFestivalInput {
  instNm: string | null;
  title: string;
  category: string;
  festivalContent: string | null;
  referenceUrl: string | null;
  region: string;
  regionDetail: string | null;
  imgUrl: string | null;
  /** "2026-10-01T10:00:00" 형태 */
  beginDe: string;
  endDe: string;
  eventTmInfo: string | null;
  partcptExpnInfo: string | null;
  telnoInfo: string | null;
  hostInstNm: string | null;
}
/**
 * 관리자 행사 검색 조건. 비운 항목은 조건을 걸지 않는다.
 * 공개 화면과 같은 GET /festivals 를 쓰지만 페이지 크기가 달라 따로 둔다.
 */
export interface AdminFestivalQuery {
  keyword?: string;
  providerType?: "PUBLIC" | "MEMBER";
  /** true면 종료된 행사를 제외한다 */
  excludeClosed?: boolean;
  /** true면 삭제된 행사도 함께 조회한다. 복구 대상을 찾으려면 필요하다 */
  includeDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
/** 관리자 회원 검색 조건. 비운 항목은 조건을 걸지 않는다. */
export interface AdminMemberQuery {
  /** 닉네임 또는 이메일 부분 일치 */
  keyword?: string;
  role?: Role;
  includeDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
/** 관리자 문의 검색 조건. 비운 항목은 조건을 걸지 않는다. */
export interface AdminInquiryQuery {
  title?: string;
  status?: "PENDING" | "ANSWERED";
  category?: "QUESTION" | "REPORT";
  includeDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
export interface SignupInput {
  email: string;
  password: string;
  nickname: string;
  phone?: string;
  profileImg?: string;
}
