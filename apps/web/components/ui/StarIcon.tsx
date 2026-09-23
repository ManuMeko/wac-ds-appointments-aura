// apps/web/components/ui/StarIcon.tsx

type StarIconProps = {
  className?: string;
};

export function StarIcon({
  className = "h-4 w-4",
}: StarIconProps) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className={className}
      fill="currentColor"
    >
      <path d="m12 2.8 2.74 5.55 6.13.89-4.44 4.33 1.05 6.11L12 16.8l-5.48 2.88 1.05-6.11-4.44-4.33 6.13-.89L12 2.8Z" />
    </svg>
  );
}