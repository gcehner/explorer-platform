import Link from "next/link";

interface ExplorerBrandProps {
  className?: string;
}

export function ExplorerBrand({ className = "" }: ExplorerBrandProps) {
  return (
    <Link
      href="/"
      className={`inline-flex items-center gap-3 text-white ${className}`}
      aria-label="Explorer – domov"
    >
      <span className="explorer-brand-mark" aria-hidden="true" />
      <span>
        <strong className="block text-[19px] leading-5 tracking-[0.04em]">
          EXPLORER
        </strong>
        <small className="block text-[10px] leading-4 tracking-[0.08em] text-[#a4c779]">
          MTB &amp; SKI ADVENTURES
        </small>
      </span>
    </Link>
  );
}
