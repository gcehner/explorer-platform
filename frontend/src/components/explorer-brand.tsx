import Link from "next/link";

interface ExplorerBrandProps {
  className?: string;
}

export function ExplorerBrand({ className = "" }: ExplorerBrandProps) {
  return (
    <Link
      href="/"
      className={`inline-flex items-center gap-2.5 text-[var(--color-foreground)] ${className}`}
      aria-label="MTB – domov"
    >
      <svg
        viewBox="0 0 36 28"
        className="h-7 w-9 text-[var(--color-mtb-dark)]"
        aria-hidden="true"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      >
        <path d="M2 23.5 12.2 6.2l5.1 8.3 3.8-5.8L34 23.5H2Z" />
      </svg>
      <strong className="text-[17px] leading-none font-semibold tracking-[0.045em]">
        MTB
      </strong>
    </Link>
  );
}
