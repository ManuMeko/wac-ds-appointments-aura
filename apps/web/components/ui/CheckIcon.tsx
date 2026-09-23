// apps/web/components/ui/CheckIcon.tsx

type CheckIconProps = {
  className?: string;
};

export function CheckIcon({
  className = "h-5 w-5",
}: CheckIconProps) {
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
      <path d="m5 12 4 4L19 6" />
    </svg>
  );
}