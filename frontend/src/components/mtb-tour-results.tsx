import { MtbTourResultCard } from "@/components/mtb-tour-result-card";
import type {
  MtbTourSummary,
  TourListingStatus,
} from "@/lib/mtb-tour-listing";

interface MtbTourResultsProps {
  tours: MtbTourSummary[];
  status?: TourListingStatus;
}

export function MtbTourResults({
  tours,
  status = "ready",
}: MtbTourResultsProps) {
  return (
    <section
      className="h-full min-h-0 border-r border-[var(--color-border)] bg-[#f7f8f5] md:overflow-y-auto"
      aria-label="Seznam MTB tur"
    >
      <div className="flex items-center justify-between gap-3 px-4 pt-4 pb-3 sm:px-5">
        <strong className="text-sm">
          Seznam tur
          <span className="ml-1 font-medium text-[#839084]">· {tours.length}</span>
        </strong>
        <select
          className="cursor-not-allowed rounded-lg border border-[var(--color-border)] bg-white px-2.5 py-2 text-xs text-[#687168] opacity-65"
          aria-label="Razvrsti ture"
          disabled
          title="Razvrščanje bo na voljo z javnim katalogom tur."
        >
          <option>Priporočeno</option>
        </select>
      </div>

      {status === "loading" ? (
        <div className="grid gap-3 px-4 pb-6" aria-live="polite" aria-busy="true">
          {[0, 1, 2].map((item) => (
            <div
              key={item}
              className="h-36 animate-pulse rounded-xl border border-[var(--color-border)] bg-white"
            />
          ))}
          <span className="sr-only">Nalagam ture …</span>
        </div>
      ) : null}

      {status === "error" ? (
        <div
          className="mx-4 rounded-xl border border-[#dfc8c0] bg-[#fff9f7] p-5 text-sm leading-6 text-[#7d3f2d]"
          role="alert"
        >
          Seznama tur trenutno ni mogoče naložiti. Poskusite znova pozneje.
        </div>
      ) : null}

      {status === "ready" && tours.length > 0 ? (
        <div className="grid gap-2.5 px-3 pb-6 sm:px-4">
          {tours.map((tour) => (
            <MtbTourResultCard key={tour.id} tour={tour} />
          ))}
        </div>
      ) : null}

      {status === "ready" && tours.length === 0 ? (
        <div className="flex min-h-72 items-center justify-center px-6 py-12 text-center">
          <div className="max-w-sm">
            <span
              className="mx-auto grid size-12 place-items-center rounded-full border border-[#d6dfd1] bg-white text-2xl text-[var(--color-mtb)]"
              aria-hidden="true"
            >
              △
            </span>
            <h2 className="mt-4 text-lg font-bold tracking-[-0.02em]">
              Javni katalog tur še ni na voljo
            </h2>
            <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
              Objavljene MTB ture bodo prikazane tukaj, ko bo vzpostavljen
              javni seznam tur.
            </p>
          </div>
        </div>
      ) : null}
    </section>
  );
}
