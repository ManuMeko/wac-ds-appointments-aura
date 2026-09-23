// apps/web/components/ui/ArrowIcon.tsx

type ArrowIconProps = {
  className?: string;
};

export function ArrowIcon({
  className = "h-4 w-4",
}: ArrowIconProps) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M5 12h14" />
      <path d="m14 7 5 5-5 5" />
    </svg>
  );
}