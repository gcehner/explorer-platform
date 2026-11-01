"use client";

import { useState } from "react";

import { MtbFilterToolbar } from "@/components/mtb-filter-toolbar";
import { MtbMap } from "@/components/mtb-map";
import { MtbTourResults } from "@/components/mtb-tour-results";
import type { MtbTourSummary } from "@/lib/mtb-tour-listing";

interface MtbDiscoveryProps {
  tours: MtbTourSummary[];
  mapStyleUrl: string;
}

export function MtbDiscovery({ tours, mapStyleUrl }: MtbDiscoveryProps) {
  const [mobileView, setMobileView] = useState<"list" | "map">("list");

  return (
    <div className="md:flex md:h-[calc(100dvh-var(--site-header-height)-44px)] md:min-h-[620px] md:flex-col">
      <MtbFilterToolbar />

      <div
        className="flex border-b border-[var(--color-border)] bg-white p-2 md:hidden"
        role="group"
        aria-label="Prikaz tur"
      >
        <button
          type="button"
          className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold ${
            mobileView === "list"
              ? "bg-[var(--color-surface-soft)] text-[var(--color-mtb-dark)]"
              : "text-[var(--color-muted)]"
          }`}
          aria-pressed={mobileView === "list"}
          onClick={() => setMobileView("list")}
        >
          Seznam
        </button>
        <button
          type="button"
          className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold ${
            mobileView === "map"
              ? "bg-[var(--color-surface-soft)] text-[var(--color-mtb-dark)]"
              : "text-[var(--color-muted)]"
          }`}
          aria-pressed={mobileView === "map"}
          onClick={() => setMobileView("map")}
        >
          Zemljevid
        </button>
      </div>

      <div className="min-h-0 flex-1 md:grid md:grid-cols-[minmax(340px,46%)_minmax(0,1fr)] xl:grid-cols-[minmax(390px,42%)_minmax(0,1fr)]">
        <div className={mobileView === "list" ? "block" : "hidden md:block"}>
          <MtbTourResults tours={tours} />
        </div>
        <div
          className={`${
            mobileView === "map" ? "block" : "hidden md:block"
          } h-[min(65vh,520px)] md:h-auto`}
        >
          <MtbMap styleUrl={mapStyleUrl} />
        </div>
      </div>
    </div>
  );
}
