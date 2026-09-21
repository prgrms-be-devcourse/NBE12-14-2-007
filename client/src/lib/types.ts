export type Mode = "preview" | "api";
export type Role =
  | "ROLE_WARNING"
  | "ROLE_UNVERIFIED"
  | "ROLE_NORMAL"
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
  submitter?: { id: string; nickname: string };
  preview?: boolean;
  submissionId?: string;
}
export interface SubmissionInput {
  title: string;
  category: string;
  manager: string;
  festivalContent: string;
  region: string;
  regionDetail: string;
  beginDe: string;
  endDe: string;
  eventTmInfo: string;
  submissionContent: string;
  instNm?: string;
  referenceUrl?: string;
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
    submissionContent: string;
    createdAt: string;
    updatedAt: string;
    festival: Festival;
  };
}
export interface PostInput {
  title: string;
  content: string;
  thumbnail?: string;
}
export interface PostSummary {
  id: string;
  member: { id: string; nickname: string; profileImg: string | null };
  festivalId: number;
  festivalTitle: string;
  title: string;
  thumbnail: string | null;
  date: string;
}
export interface PostDetail extends PostSummary {
  content: string;
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
export interface SignupInput {
  email: string;
  password: string;
  nickname: string;
  phone?: string;
  profileImg?: string;
}
