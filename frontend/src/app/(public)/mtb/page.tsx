import type { Metadata } from "next";
import Link from "next/link";

import { MtbDiscovery } from "@/components/mtb-discovery";
import type { MtbTourSummary } from "@/lib/mtb-tour-listing";

const DEFAULT_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

export const metadata: Metadata = {
  title: "MTB ture",
  description: "Raziščite objavljene MTB ture na interaktivnem zemljevidu.",
};

export default function MtbPage() {
  const mapStyleUrl =
    process.env.NEXT_PUBLIC_MAP_STYLE_URL?.trim() || DEFAULT_MAP_STYLE_URL;
  const tours: MtbTourSummary[] = [];

  return (
    <main>
      <nav
        className="flex min-h-11 items-center border-b border-[#eef1ed] bg-white px-5 text-xs text-[var(--color-muted)] sm:px-7"
        aria-label="Navigacijska pot"
      >
        <Link
          className="transition-colors hover:text-[var(--color-mtb-dark)] hover:underline"
          href="/"
        >
          Domov
        </Link>
        <span className="px-2" aria-hidden="true">
          /
        </span>
        <span aria-current="page">MTB</span>
      </nav>

      <MtbDiscovery tours={tours} mapStyleUrl={mapStyleUrl} />
    </main>
  );
}
