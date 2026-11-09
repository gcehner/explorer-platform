import { MtbTourResultCard } from "@/components/mtb-tour-result-card";
import type {
  MtbTourSummary,
  TourListingStatus,
} from "@/lib/mtb-tour-listing";

interface MtbTourResultsProps {
  tours: MtbTourSummary[];
  status: TourListingStatus;
  hasMore: boolean;
  loadingMore: boolean;
  loadMoreError: boolean;
  onRetry: () => void;
  onLoadMore: () => void;
}

export function MtbTourResults({
  tours,
  status,
  hasMore,
  loadingMore,
  loadMoreError,
  onRetry,
  onLoadMore,
}: MtbTourResultsProps) {
  return (
    <section
      className="h-full min-h-0 border-r border-[var(--color-border)] bg-[#f7f8f5] md:overflow-y-auto"
      aria-label="Seznam MTB tur"
    >
      <div className="flex items-center justify-between gap-3 px-4 pt-4 pb-3 sm:px-5">
        <strong className="text-sm">Seznam tur</strong>
        <select
          className="cursor-not-allowed rounded-lg border border-[var(--color-border)] bg-white px-2.5 py-2 text-xs text-[#687168] opacity-65"
          aria-label="Razvrsti ture"
          disabled
          title="Javni API trenutno uporablja vrstni red po datumu objave."
        >
          <option>Najnovejše</option>
        </select>
      </div>

      {status === "waiting-for-map" ? (
        <div className="px-6 py-12 text-center text-sm leading-6 text-[var(--color-muted)]" aria-live="polite">
          Pripravljam območje zemljevida …
        </div>
      ) : null}

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

      {status === "invalid-bounds" ? (
        <div className="mx-4 rounded-xl border border-[#d8d5bd] bg-[#fffdf3] p-5 text-sm leading-6 text-[#665f2f]" role="status">
          Vidnega območja zemljevida ni mogoče uporabiti za iskanje tur. Zemljevid
          premaknite v običajno območje brez prehoda čez datumsko mejo in stran
          znova naložite.
        </div>
      ) : null}

      {status === "error" ? (
        <div className="mx-4 rounded-xl border border-[#dfc8c0] bg-[#fff9f7] p-5 text-sm leading-6 text-[#7d3f2d]" role="alert">
          <p>Seznama tur trenutno ni mogoče naložiti.</p>
          <button
            type="button"
            className="mt-3 rounded-lg border border-[#c89f91] bg-white px-3 py-2 font-semibold hover:border-[#9e6b5a]"
            onClick={onRetry}
          >
            Poskusi znova
          </button>
        </div>
      ) : null}

      {status === "ready" && tours.length > 0 ? (
        <div className="grid gap-2.5 px-3 pb-6 sm:px-4">
          {tours.map((tour) => (
            <MtbTourResultCard key={tour.id} tour={tour} />
          ))}

          {loadMoreError ? (
            <div className="rounded-xl border border-[#dfc8c0] bg-[#fff9f7] p-4 text-center text-sm text-[#7d3f2d]" role="alert">
              <p>Dodatnih tur ni bilo mogoče naložiti.</p>
              <button
                type="button"
                className="mt-2 rounded-lg border border-[#c89f91] bg-white px-3 py-2 font-semibold"
                onClick={onLoadMore}
              >
                Poskusi znova
              </button>
            </div>
          ) : null}

          {hasMore && !loadMoreError ? (
            <button
              type="button"
              className="mx-auto mt-2 min-w-32 rounded-lg border border-[#9cb98b] bg-white px-4 py-2.5 text-sm font-semibold text-[var(--color-mtb-dark)] transition-colors hover:bg-[var(--color-surface-soft)] disabled:cursor-wait disabled:opacity-60"
              disabled={loadingMore}
              aria-busy={loadingMore}
              onClick={onLoadMore}
            >
              {loadingMore ? "Nalagam …" : "Naloži več"}
            </button>
          ) : null}
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
              Za izbrano območje ni tur
            </h2>
            <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
              Nobena objavljena MTB tura ne ustreza trenutnemu območju in filtrom.
            </p>
          </div>
        </div>
      ) : null}
    </section>
  );
}
