import { useId } from "react";
import { AlertTriangle } from "lucide-react";
import { roleNames } from "../lib/format";
import type { Role } from "../lib/types";

/** 제보자 신뢰 등급 배지. 마우스를 올리거나 포커스하면 등급 설명이 뜬다. */
export function TrustGradeBadge({ member }: { member: { role: Role } }) {
  const grade =
    member.role === "ROLE_WARNING"
      ? "warning"
      : member.role === "ROLE_RECOGNIZED"
        ? "maker"
        : member.role === "ROLE_TRUSTED"
          ? "master"
          : "basic";
  const symbol = grade === "maker" ? "m" : grade === "master" ? "M" : null;
  const description =
    member.role === "ROLE_WARNING"
      ? "운영 정책에 따라 현재 활동이 제한된 계정입니다."
      : member.role === "ROLE_RECOGNIZED"
        ? "좋아요 또는 정확해요를 10개 이상 받은 제보자예요."
        : member.role === "ROLE_TRUSTED"
          ? "좋아요와 정확해요를 모두 10개 이상 받은 제보자예요."
          : member.role === "ROLE_ADMIN"
            ? "방구석탈출 운영·관리 계정이에요."
            : "이제 막 탈출 정보를 나누기 시작한 제보자예요.";
  // 목록에서는 같은 제보자의 배지가 여러 번 나오므로 회원 ID만으로는 id가 겹친다.
  const tooltipId = `trust-grade-${useId()}`;

  return (
    <span
      className="trust-grade-wrap"
      tabIndex={0}
      aria-describedby={tooltipId}
    >
      <span className={`badge trust-grade-badge ${grade}`}>
        {grade === "warning" ? (
          <AlertTriangle
            className="trust-grade-warning-icon"
            size={12}
            aria-hidden="true"
          />
        ) : symbol ? (
          <span className="trust-grade-symbol" aria-hidden="true">
            {symbol}
          </span>
        ) : null}
        <span>{roleNames[member.role]}</span>
      </span>
      <span className="trust-grade-tooltip" id={tooltipId} role="tooltip">
        {description}
      </span>
    </span>
  );
}
