import type { ReactNode } from "react";

import type { TradeIconName } from "@/data/types";

const paths: Record<TradeIconName, ReactNode> = {
  revision: (
    <>
      <rect x="6" y="4" width="12" height="16" rx="1.5" />
      <path d="M9 4.5h6V7H9zM9 11h6M9 14h4" />
    </>
  ),
  oil: <path d="M12 3s6 7 6 11a6 6 0 1 1-12 0c0-4 6-11 6-11z" />,
  itv: (
    <>
      <path d="M12 3 5 6v6c0 4.5 3 7 7 9 4-2 7-4.5 7-9V6z" />
      <path d="m9 12 2 2 4-4" />
    </>
  ),
  tires: (
    <>
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M12 4v2.2M12 17.8V20M4 12h2.2M17.8 12H20" />
    </>
  ),
  brakes: (
    <>
      <circle cx="11" cy="12" r="6.5" />
      <circle cx="11" cy="12" r="2" />
      <path d="M16.2 9.2h3.2v5.6h-3.2" />
    </>
  ),
  diagnosis: <path d="M3.5 12H7l2.2-5 4.2 10L16 12h4.5" />,
  quote: (
    <>
      <path d="M7 3.5h7.5L19 8v12.5H7z" />
      <path d="M14.5 3.5V8H19M9.5 13h5M9.5 16.5h3.5" />
    </>
  ),
  parts: (
    <>
      <path d="m12 3 9 5-9 5L3 8z" />
      <path d="m3 12 9 5 9-5M3 16l9 5 9-5" />
    </>
  ),
  schedule: (
    <>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4.5l3 2" />
    </>
  ),
};

export function TradeIcon({
  name,
  className,
}: {
  name: TradeIconName;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={className ?? "h-4 w-4"}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name]}
    </svg>
  );
}
