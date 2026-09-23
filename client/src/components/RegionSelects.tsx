import { MapPin } from "lucide-react";
import { regionGroups, selectedRegion } from "../lib/regions";

export function RegionSelects({
  value,
  onChange,
  variant = "filter",
}: {
  value: string;
  onChange: (value: string) => void;
  variant?: "filter" | "discovery" | "form";
}) {
  const { province } = selectedRegion(value);
  const className =
    variant === "filter"
      ? "filter-select region-select"
      : variant === "form"
        ? "form-region-select"
        : "region-select";
  return (
    <label className={className}>
      {variant !== "form" && <MapPin size={18} />}
      <div>
        <span>
          시·도
          {variant === "form" && <b className="required">*</b>}
        </span>
        <select
          aria-label="시·도 선택"
          required={variant === "form"}
          value={province?.value || ""}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">
            {variant === "form" ? "시·도 선택" : "전국"}
          </option>
          {regionGroups.map((group) => (
            <option key={group.value} value={group.value}>
              {group.label}
            </option>
          ))}
        </select>
      </div>
    </label>
  );
}
