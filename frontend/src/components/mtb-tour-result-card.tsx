import type { MtbTourSummary } from "@/lib/mtb-tour-listing";

const numberFormat = new Intl.NumberFormat("sl-SI", {
  maximumFractionDigits: 1,
});

function formatCountry(countryCode: string | null) {
  if (!countryCode) return null;

  try {
    return new Intl.DisplayNames(["sl"], { type: "region" }).of(countryCode);
  } catch {
    return countryCode;
  }
}

export function MtbTourResultCard({ tour }: { tour: MtbTourSummary }) {
  const country = formatCountry(tour.countryCode);
  const location = [tour.mountainRange, country].filter(Boolean).join(" · ");

  return (
    <article className="grid grid-cols-[112px_minmax(0,1fr)] gap-3 rounded-xl border border-[var(--color-border)] bg-white p-2.5 shadow-[0_5px_20px_rgba(20,35,22,0.055)] sm:grid-cols-[126px_minmax(0,1fr)]">
      <div className="relative min-h-28 overflow-hidden rounded-lg bg-[linear-gradient(155deg,#e4e9df_0_28%,#b8c6b4_28%_45%,#789078_45%_63%,#4d6f55_63%)] sm:min-h-31">
        <span className="absolute inset-0 grid place-items-center text-3xl text-white/75" aria-hidden="true">
          △
        </span>
        {tour.technicalDifficulty ? (
          <span className="absolute top-2 left-2 rounded-md bg-white px-2 py-0.5 text-[10px] font-extrabold text-[var(--color-mtb-dark)] shadow-sm">
            {tour.technicalDifficulty}
          </span>
        ) : null}
      </div>

      <div className="flex min-w-0 flex-col">
        <h2 className="mt-0.5 text-[15px] leading-5 font-bold tracking-[-0.02em]">
          {tour.title}
        </h2>
        {location ? (
          <p className="mt-1 text-[11px] font-semibold text-[#688067]">{location}</p>
        ) : tour.startLocation ? (
          <p className="mt-1 text-[11px] font-semibold text-[#688067]">
            {tour.startLocation.name}
          </p>
        ) : null}

        {tour.shortDescription ? (
          <p className="mt-2 line-clamp-2 text-xs leading-5 text-[var(--color-muted)]">
            {tour.shortDescription}
          </p>
        ) : null}

        <div className="mt-auto flex flex-wrap gap-x-3 gap-y-1 pt-3 text-xs text-[#58665b]">
          {tour.distanceKm !== null ? (
            <span>
              <b className="text-[#26382a]">{numberFormat.format(tour.distanceKm)}</b> km
            </span>
          ) : null}
          {tour.totalAscentMeters !== null ? (
            <span>
              <b className="text-[#26382a]">
                {numberFormat.format(tour.totalAscentMeters)}
              </b>{" "}
              m ↑
            </span>
          ) : null}
          {tour.estimatedDuration ? (
            <span>
              <b className="text-[#26382a]">
                {numberFormat.format(tour.estimatedDuration.minimumHours)}–
                {numberFormat.format(tour.estimatedDuration.maximumHours)}
              </b>{" "}
              h
            </span>
          ) : null}
        </div>
      </div>
    </article>
  );
}
