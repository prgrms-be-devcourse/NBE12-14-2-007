import { regions } from "./format";

export interface RegionOption {
  value: string;
  label: string;
}
export interface RegionGroup extends RegionOption {
  districts: RegionOption[];
}

// Browse-only options. These are not additions to the backend FestivalRegion enum.
// Keep existing GYEONGGI_* values so saved search URLs continue to work.
const districtNames: Record<string, string[]> = {
  GANGWON: [
    "춘천시",
    "원주시",
    "강릉시",
    "동해시",
    "태백시",
    "속초시",
    "삼척시",
    "홍천군",
    "횡성군",
    "영월군",
    "평창군",
    "정선군",
    "철원군",
    "화천군",
    "양구군",
    "인제군",
    "고성군",
    "양양군",
  ],
  SEOUL: [
    "종로구",
    "중구",
    "용산구",
    "성동구",
    "광진구",
    "동대문구",
    "중랑구",
    "성북구",
    "강북구",
    "도봉구",
    "노원구",
    "은평구",
    "서대문구",
    "마포구",
    "양천구",
    "강서구",
    "구로구",
    "금천구",
    "영등포구",
    "동작구",
    "관악구",
    "서초구",
    "강남구",
    "송파구",
    "강동구",
  ],
  // Incheon district reorganization effective 2026-07-01: see README sources.
  INCHEON: [
    "제물포구",
    "영종구",
    "미추홀구",
    "연수구",
    "남동구",
    "부평구",
    "계양구",
    "서구",
    "검단구",
    "강화군",
    "옹진군",
  ],
  BUSAN: [
    "중구",
    "서구",
    "동구",
    "영도구",
    "부산진구",
    "동래구",
    "남구",
    "북구",
    "해운대구",
    "사하구",
    "금정구",
    "강서구",
    "연제구",
    "수영구",
    "사상구",
    "기장군",
  ],
  DAEGU: [
    "중구",
    "동구",
    "서구",
    "남구",
    "북구",
    "수성구",
    "달서구",
    "달성군",
    "군위군",
  ],
  GWANGJU: ["동구", "서구", "남구", "북구", "광산구"],
  DAEJEON: ["동구", "중구", "서구", "유성구", "대덕구"],
  ULSAN: ["중구", "남구", "동구", "북구", "울주군"],
  SEJONG: [],
  CHUNGBUK: [
    "청주시",
    "충주시",
    "제천시",
    "보은군",
    "옥천군",
    "영동군",
    "증평군",
    "진천군",
    "괴산군",
    "음성군",
    "단양군",
  ],
  CHUNGNAM: [
    "천안시",
    "공주시",
    "보령시",
    "아산시",
    "서산시",
    "논산시",
    "계룡시",
    "당진시",
    "금산군",
    "부여군",
    "서천군",
    "청양군",
    "홍성군",
    "예산군",
    "태안군",
  ],
  JEONBUK: [
    "전주시",
    "군산시",
    "익산시",
    "정읍시",
    "남원시",
    "김제시",
    "완주군",
    "진안군",
    "무주군",
    "장수군",
    "임실군",
    "순창군",
    "고창군",
    "부안군",
  ],
  JEONNAM: [
    "목포시",
    "여수시",
    "순천시",
    "나주시",
    "광양시",
    "담양군",
    "곡성군",
    "구례군",
    "고흥군",
    "보성군",
    "화순군",
    "장흥군",
    "강진군",
    "해남군",
    "영암군",
    "무안군",
    "함평군",
    "영광군",
    "장성군",
    "완도군",
    "진도군",
    "신안군",
  ],
  GYEONGBUK: [
    "포항시",
    "경주시",
    "김천시",
    "안동시",
    "구미시",
    "영주시",
    "영천시",
    "상주시",
    "문경시",
    "경산시",
    "의성군",
    "청송군",
    "영양군",
    "영덕군",
    "청도군",
    "고령군",
    "성주군",
    "칠곡군",
    "예천군",
    "봉화군",
    "울진군",
    "울릉군",
  ],
  GYEONGNAM: [
    "창원시",
    "진주시",
    "통영시",
    "사천시",
    "김해시",
    "밀양시",
    "거제시",
    "양산시",
    "의령군",
    "함안군",
    "창녕군",
    "고성군",
    "남해군",
    "하동군",
    "산청군",
    "함양군",
    "거창군",
    "합천군",
  ],
  JEJU: ["제주시", "서귀포시"],
};

export const regionGroups: RegionGroup[] = [
  {
    value: "GYEONGGI",
    label: regions.GYEONGGI,
    districts: Object.entries(regions)
      .filter(([value]) => value.startsWith("GYEONGGI_"))
      .map(([value, label]) => ({
        value,
        label: label.replace("경기도 ", ""),
      })),
  },
  ...Object.entries(districtNames).map(([value, names]) => ({
    value,
    label: regions[value],
    districts: names.map((label) => ({ value: `${value}:${label}`, label })),
  })),
].map((group) => ({
  ...group,
  districts: group.districts.sort((a, b) =>
    a.label.localeCompare(b.label, "ko"),
  ),
}));

export function selectedRegion(value: string) {
  const province = regionGroups.find(
    (group) =>
      group.value === value || group.districts.some((d) => d.value === value),
  );
  return {
    province,
    district:
      province?.value === value
        ? ""
        : province?.districts.find((d) => d.value === value)?.value || "",
  };
}

export function matchesRegion(eventRegion: string, filter: string) {
  const { province, district } = selectedRegion(filter);
  if (!province) return !filter;
  if (district) return eventRegion === district;
  return (
    eventRegion === province.value ||
    province.districts.some((d) => d.value === eventRegion)
  );
}
