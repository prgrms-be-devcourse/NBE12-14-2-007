/** 서버 DeletionReason 중 관리자 삭제에 사용할 수 있는 분류만 제공한다. */
export const deletionReasons = {
  DUPLICATE: "중복 등록",
  INCORRECT_INFORMATION: "잘못된 정보",
  SPAM: "도배·스팸",
  ABUSIVE_CONTENT: "욕설·비방",
  PRIVACY_EXPOSURE: "개인정보 노출",
  OTHER: "기타",
} as const;

export type AdminDeletionReason = keyof typeof deletionReasons;
export interface LatestDeletionHistory {
  action: "DELETE" | "RESTORE";
  actorType: "AUTHOR" | "ADMIN";
  reasonCode: AdminDeletionReason | "AUTHOR_REQUEST" | "ADMIN_RESTORE";
  reasonLabel: string;
  reasonDetail: string | null;
  createdAt: string;
}

export interface MyContentItem {
  type: DeletionTarget;
  targetId: string;
  title: string;
  createdAt: string;
  deletedAt: string | null;
  parentDeleted: boolean;
  originalPath: string | null;
  deletion: LatestDeletionHistory | null;
}
export type DeletionTarget =
  "FESTIVAL" | "POST" | "COMMENT" | "COMMUNITY_POST" | "COMMUNITY_COMMENT";
