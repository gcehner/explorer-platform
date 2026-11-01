import Image from "next/image";
import Link from "next/link";

import type { MtbTourSummary } from "@/lib/mtb-tour-listing";

function formatCountry(countryCode: string | null) {
  if (!countryCode) {
    return null;
  }

  try {
    return new Intl.DisplayNames(["sl"], { type: "region" }).of(countryCode);
  } catch {
    return countryCode;
  }
}

function elevationPoints(tour: MtbTourSummary) {
  const samples = tour.elevationProfile;

  if (samples.length < 2) {
    return null;
  }

  const elevations = samples.map((sample) => sample.elevationMeters);
  const minimum = Math.min(...elevations);
  const range = Math.max(...elevations) - minimum || 1;
  const maximumDistance = samples.at(-1)?.distanceKm || 1;

  return samples
    .map((sample) => {
      const x = (sample.distanceKm / maximumDistance) * 260;
      const y = 22 - ((sample.elevationMeters - minimum) / range) * 20;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");
}

export function MtbTourResultCard({ tour }: { tour: MtbTourSummary }) {
  const country = formatCountry(tour.countryCode);
  const location = [tour.mountainRange, country].filter(Boolean).join(" · ");
  const profilePoints = elevationPoints(tour);

  return (
    <Link
      href={`/mtb/tours/${tour.id}`}
      className="grid grid-cols-[112px_minmax(0,1fr)] gap-3 rounded-xl border border-[var(--color-border)] bg-white p-2.5 shadow-[0_5px_20px_rgba(20,35,22,0.055)] transition hover:border-[#82a36b] hover:shadow-[0_5px_22px_rgba(49,87,20,0.1)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-mtb-dark)] sm:grid-cols-[126px_minmax(0,1fr)]"
    >
      <div className="relative min-h-28 overflow-hidden rounded-lg bg-[#e4e9df] sm:min-h-31">
        {tour.coverImageUrl ? (
          <Image
            src={tour.coverImageUrl}
            alt=""
            fill
            unoptimized
            sizes="126px"
            className="object-cover"
          />
        ) : (
          <span
            className="absolute inset-0 grid place-items-center text-3xl text-[#8fa087]"
            aria-hidden="true"
          >
            △
          </span>
        )}
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
        ) : null}

        <div className="mt-auto flex flex-wrap gap-x-3 gap-y-1 pt-3 text-xs text-[#58665b]">
          {tour.distanceKm !== null ? (
            <span><b className="text-[#26382a]">{tour.distanceKm}</b> km</span>
          ) : null}
          {tour.totalAscentMeters !== null ? (
            <span><b className="text-[#26382a]">{tour.totalAscentMeters}</b> m ↑</span>
          ) : null}
          {tour.estimatedDuration ? (
            <span>
              <b className="text-[#26382a]">
                {tour.estimatedDuration.minimumHours}–{tour.estimatedDuration.maximumHours}
              </b>{" "}
              h
            </span>
          ) : null}
        </div>

        {profilePoints ? (
          <svg className="mt-2 h-6 w-full" viewBox="0 0 260 24" preserveAspectRatio="none" aria-hidden="true">
            <polyline
              points={profilePoints}
              fill="none"
              stroke="#4e7f22"
              strokeWidth="1.8"
              vectorEffect="non-scaling-stroke"
            />
          </svg>
        ) : null}
      </div>
    </Link>
  );
}
