import type { ReactElement } from "react";

const GREEN = "#22C55E";
const RED = "#EF4444";
const ORANGE = "#F59E0B";
const YELLOW = "#F5B83D";
const BLUE = "#3B82F6";
const PURPLE = "#8B5CF6";
const GREY = "#64748B";
const WHITE = "#FFFFFF";

// Bright, simple pictures so a child who can't read yet can tell the buttons apart.
const ICONS = {
  home: (
    <>
      <polygon points="2,11.5 12,3 22,11.5" fill={RED} />
      <rect x="5" y="10.5" width="14" height="10.5" rx="1" fill={YELLOW} />
      <rect x="10" y="14" width="4" height="7" rx="0.8" fill="#8B5A2B" />
    </>
  ),
  practice: (
    <>
      <circle cx="8" cy="17" r="3.6" fill={BLUE} />
      <rect x="10.3" y="3.5" width="2.2" height="14" fill={BLUE} />
      <path d="M12.5 3.5c3.5 1 6 3.3 6 6.8-1.6-1.7-3.8-2.3-6-2.3z" fill={BLUE} />
    </>
  ),
  songs: (
    <>
      <rect x="2.5" y="5" width="5.5" height="16" rx="1" fill={RED} />
      <rect x="9" y="3" width="5.5" height="18" rx="1" fill={GREEN} />
      <rect x="15.5" y="6" width="6" height="15" rx="1" fill={BLUE} />
      <rect x="3.8" y="8" width="3" height="1.4" fill={WHITE} />
      <rect x="10.3" y="6" width="3" height="1.4" fill={WHITE} />
      <rect x="16.9" y="9" width="3.2" height="1.4" fill={WHITE} />
    </>
  ),
  devices: (
    <>
      <rect x="1.5" y="6" width="21" height="13" rx="2" fill={WHITE} stroke="#334155" strokeWidth="1.5" />
      <rect x="5" y="6" width="2.4" height="7.5" fill="#1F2937" />
      <rect x="9.2" y="6" width="2.4" height="7.5" fill="#1F2937" />
      <rect x="14.6" y="6" width="2.4" height="7.5" fill="#1F2937" />
      <rect x="18.6" y="6" width="2.4" height="7.5" fill="#1F2937" />
    </>
  ),
  play: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={GREEN} />
      <polygon points="9.5,7 17.5,12 9.5,17" fill={WHITE} />
    </>
  ),
  pause: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={ORANGE} />
      <rect x="8" y="7.5" width="3" height="9" rx="0.8" fill={WHITE} />
      <rect x="13" y="7.5" width="3" height="9" rx="0.8" fill={WHITE} />
    </>
  ),
  stop: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={RED} />
      <rect x="7.8" y="7.8" width="8.4" height="8.4" rx="1.2" fill={WHITE} />
    </>
  ),
  import: (
    <>
      <path d="M2 7a2 2 0 0 1 2-2h5l2 2h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z" fill={YELLOW} />
      <circle cx="17" cy="16.5" r="5.5" fill={GREEN} />
      <rect x="16" y="13" width="2" height="7" rx="0.5" fill={WHITE} />
      <rect x="13.5" y="15.5" width="7" height="2" rx="0.5" fill={WHITE} />
    </>
  ),
  trash: (
    <>
      <rect x="9.5" y="2.5" width="5" height="2.5" rx="1" fill="#B91C1C" />
      <rect x="4" y="5" width="16" height="2.8" rx="1" fill="#B91C1C" />
      <rect x="6" y="8.5" width="12" height="13" rx="2" fill={RED} />
      <rect x="9" y="11" width="1.6" height="8" rx="0.8" fill={WHITE} />
      <rect x="13.4" y="11" width="1.6" height="8" rx="0.8" fill={WHITE} />
    </>
  ),
  sheet: (
    <>
      <rect x="4" y="2" width="16" height="20" rx="2" fill={WHITE} stroke="#475569" strokeWidth="1.5" />
      <path d="M7 7h10M7 10h10M7 13h10" stroke="#94A3B8" strokeWidth="1.2" />
      <ellipse cx="10.5" cy="17" rx="2.3" ry="1.8" fill={BLUE} />
      <rect x="12" y="9" width="1.4" height="8" fill={BLUE} />
    </>
  ),
  speaker: (
    <>
      <polygon points="2.5,9 7,9 12.5,4.5 12.5,19.5 7,15 2.5,15" fill={PURPLE} />
      <path d="M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11" fill="none" stroke={PURPLE} strokeWidth="2" strokeLinecap="round" />
    </>
  ),
  again: (
    <>
      <path d="M19.5 12a7.5 7.5 0 1 1-2.2-5.3" fill="none" stroke={GREEN} strokeWidth="3.2" strokeLinecap="round" />
      <polygon points="21,3 21,10.5 13.5,10.5" fill={GREEN} />
    </>
  ),
  settings: (
    <>
      {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => (
        <rect key={angle} x="10.3" y="1.5" width="3.4" height="5" rx="1" fill={GREY} transform={`rotate(${angle} 12 12)`} />
      ))}
      <circle cx="12" cy="12" r="6.8" fill={GREY} />
      <circle cx="12" cy="12" r="2.8" fill={WHITE} />
    </>
  ),
  ok: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={GREEN} />
      <polyline points="7,12.5 10.5,16 17,9" fill="none" stroke={WHITE} strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  cancel: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={RED} />
      <path d="M8.3 8.3l7.4 7.4M15.7 8.3l-7.4 7.4" stroke={WHITE} strokeWidth="2.8" strokeLinecap="round" />
    </>
  ),
  back: (
    <>
      <circle cx="12" cy="12" r="10.5" fill={GREY} />
      <path d="M13.5 7l-5 5 5 5M8.8 12h7.5" fill="none" stroke={WHITE} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  wand: (
    <>
      <path d="M3.5 20.5l11-11" stroke="#8B5A2B" strokeWidth="3" strokeLinecap="round" />
      <polygon points="17.5,2.0 18.7,4.9 21.8,5.1 19.4,7.1 20.1,10.1 17.5,8.5 14.9,10.1 15.6,7.1 13.2,5.1 16.3,4.9" fill="#FACC15" stroke="#EAB308" strokeWidth="0.8" />
    </>
  ),
  note: (
    <>
      <ellipse cx="9" cy="17" rx="4" ry="3.2" fill="#EC4899" />
      <rect x="11.2" y="3.5" width="2.2" height="13.5" fill="#EC4899" />
    </>
  ),
  chord: (
    <>
      <ellipse cx="9.5" cy="19" rx="3.5" ry="2.6" fill={PURPLE} />
      <ellipse cx="9.5" cy="13.5" rx="3.5" ry="2.6" fill={PURPLE} />
      <ellipse cx="9.5" cy="8" rx="3.5" ry="2.6" fill={PURPLE} />
      <rect x="12.3" y="2" width="2" height="17" fill={PURPLE} />
    </>
  ),
  star: <polygon points="12.0,2.5 14.6,8.9 21.5,9.4 16.2,13.9 17.9,20.6 12.0,16.9 6.1,20.6 7.8,13.9 2.5,9.4 9.4,8.9" fill="#FACC15" stroke="#EAB308" strokeWidth="1" strokeLinejoin="round" />,
  search: (
    <>
      <circle cx="10" cy="10" r="6" fill="none" stroke={GREY} strokeWidth="2.8" />
      <path d="M14.5 14.5l6 6" stroke={GREY} strokeWidth="3.2" strokeLinecap="round" />
    </>
  ),
} satisfies Record<string, ReactElement>;

export type IconName = keyof typeof ICONS;

export const ICON_NAMES = Object.keys(ICONS) as IconName[];

export function Icon({ name }: { name: IconName }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false" data-icon={name}>
      {ICONS[name]}
    </svg>
  );
}
