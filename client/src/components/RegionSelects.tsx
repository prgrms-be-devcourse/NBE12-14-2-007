import { MapPin } from "lucide-react";
import { regionGroups, selectedRegion } from "../lib/regions";

export function RegionSelects({
  value,
  onChange,
  variant = "filter",
  supportedOnly = false,
}: {
  value: string;
  onChange: (value: string) => void;
  variant?: "filter" | "discovery";
  supportedOnly?: boolean;
}) {
  const { province, district } = selectedRegion(value);
  const districts = supportedOnly
    ? province?.districts.filter((option) => !option.value.includes(":"))
    : province?.districts;
  const className =
    variant === "filter" ? "filter-select region-select" : "region-select";
  return (
    <>
      <label className={className}>
        <MapPin size={18} />
        <div>
          <span>시·도</span>
          <select
            aria-label="시·도 선택"
            value={province?.value || ""}
            onChange={(e) => onChange(e.target.value)}
          >
            <option value="">전국</option>
            {regionGroups.map((group) => (
              <option key={group.value} value={group.value}>
                {group.label}
              </option>
            ))}
          </select>
        </div>
      </label>
      <label className={className}>
        <MapPin size={18} />
        <div>
          <span>시·군·구</span>
          <select
            aria-label="시·군·구 선택"
            value={district}
            disabled={!districts?.length}
            onChange={(e) => onChange(e.target.value || province?.value || "")}
          >
            <option value="">{province ? "전체" : "시·도 먼저 선택"}</option>
            {districts?.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </label>
    </>
  );
}
