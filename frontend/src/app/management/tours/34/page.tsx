import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { ElevationProfile } from "@/components/elevation-profile";
import { TourMap } from "@/components/tour-map";
import { getManagementTour, TourApiError } from "@/lib/tour-api";
import {
  formatBikeType,
  formatCountry,
  formatDecimal,
  formatInteger,
  formatRouteType,
  formatSurfaceType,
} from "@/lib/tour-format";
import type { TourDetail } from "@/lib/tour-types";

const TOUR_ID = 34;
const DEFAULT_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

export const metadata: Metadata = {
  title: "Predogled MTB ture 34",
};

// This local preview intentionally waits for fresh management data on each request.
export const instant = false;

function Card({
  children,
  className = "",
}: Readonly<{ children: React.ReactNode; className?: string }>) {
  return (
    <article
      className={`rounded-2xl border border-[#dfe4dc] bg-white p-6 shadow-[0_12px_28px_rgba(20,30,23,0.08)] ${className}`}
    >
      {children}
    </article>
  );
}

function Fact({ label, value }: Readonly<{ label: string; value: string }>) {
  return (
    <div className="flex items-start justify-between gap-5 border-b border-stone-200 py-3 last:border-0">
      <dt className="text-stone-500">{label}</dt>
      <dd className="text-right font-bold text-[#17201a]">{value}</dd>
    </div>
  );
}

function formatDuration(tour: TourDetail): string | null {
  if (!tour.estimatedDuration) {
    return null;
  }

  const { minimumHours, maximumHours } = tour.estimatedDuration;
  if (minimumHours === maximumHours) {
    return `${formatDecimal(minimumHours)} h`;
  }
  return `${formatDecimal(minimumHours)}–${formatDecimal(maximumHours)} h`;
}

function formatTourDate(value: string): string {
  return new Intl.DateTimeFormat("sl-SI", {
    dateStyle: "long",
    timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}

function ErrorState({ message }: Readonly<{ message: string }>) {
  return (
    <main className="mx-auto flex min-h-screen w-[min(760px,92vw)] items-center">
      <Card>
        <p className="text-sm font-extrabold tracking-[0.12em] text-red-700 uppercase">
          Predogled ni na voljo
        </p>
        <h1 className="mt-3 text-3xl font-bold">Ture 34 ni bilo mogoče naložiti.</h1>
        <p className="mt-4 text-stone-600">{message}</p>
      </Card>
    </main>
  );
}

export default async function TourPreviewPage() {
  if (process.env.NODE_ENV === "production") {
    notFound();
  }

  let tour: TourDetail;
  try {
    tour = await getManagementTour(TOUR_ID);
  } catch (error) {
    if (error instanceof TourApiError && error.status === 404) {
      notFound();
    }
    return (
      <ErrorState
        message={
          error instanceof Error
            ? error.message
            : "Pri povezavi z backendom je prišlo do nepričakovane napake."
        }
      />
    );
  }

  const statistics = tour.statistics;
  const duration = formatDuration(tour);
  const mapStyleUrl =
    process.env.NEXT_PUBLIC_MAP_STYLE_URL?.trim() || DEFAULT_MAP_STYLE_URL;
  const title = tour.title?.trim() || `MTB tura #${tour.id}`;
  const locationParts = [
    tour.mountainRange,
    tour.countryCode ? formatCountry(tour.countryCode) : null,
  ].filter(Boolean) as string[];
  const heroMetrics = [
    statistics ? `${formatDecimal(statistics.distanceKm)} km` : null,
    statistics
      ? `${formatInteger(statistics.totalAscentMeters)} m vzpona`
      : null,
    duration,
    tour.technicalDifficulty ? `Spust ${tour.technicalDifficulty}` : null,
    tour.routeType ? formatRouteType(tour.routeType) : null,
  ].filter(Boolean) as string[];

  const facts = [
    tour.routeType
      ? { label: "Tip ture", value: formatRouteType(tour.routeType) }
      : null,
    tour.startLocation
      ? { label: "Izhodišče", value: tour.startLocation.name }
      : null,
    tour.recommendedBikeTypes.length
      ? {
          label: "Priporočeno kolo",
          value: tour.recommendedBikeTypes.map(formatBikeType).join(", "),
        }
      : null,
    tour.physicalDifficulty !== null
      ? {
          label: "Fizična težavnost",
          value: `${tour.physicalDifficulty} / 5`,
        }
      : null,
    tour.exposureRating
      ? { label: "Izpostavljenost", value: tour.exposureRating }
      : null,
    tour.scenicRating !== null
      ? { label: "Razglednost", value: `${tour.scenicRating} / 5` }
      : null,
    tour.carryingPercentage !== null
      ? {
          label: "Delež prenašanja",
          value: `${tour.carryingPercentage} %`,
        }
      : null,
    tour.tourDate
      ? { label: "Datum ture", value: formatTourDate(tour.tourDate) }
      : null,
  ].filter(Boolean) as { label: string; value: string }[];

  const metrics = statistics
    ? [
        { label: "Dolžina", value: `${formatDecimal(statistics.distanceKm)} km` },
        {
          label: "Skupni vzpon",
          value: `${formatInteger(statistics.totalAscentMeters)} m`,
        },
        {
          label: "Skupni spust",
          value: `${formatInteger(statistics.totalDescentMeters)} m`,
        },
        {
          label: "Najvišja točka",
          value: `${formatInteger(statistics.highestPointMeters)} m`,
        },
        {
          label: "Najnižja točka",
          value: `${formatInteger(statistics.lowestPointMeters)} m`,
        },
        ...(duration ? [{ label: "Predviden čas", value: duration }] : []),
      ]
    : [];

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-white/10 bg-[#101513] text-white">
        <div className="mx-auto flex h-[72px] w-[min(1420px,94vw)] items-center">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-full border border-[#8aba4f]/50 text-xl text-[#8aba4f]">
              △
            </span>
            <span>
              <strong className="block text-lg tracking-[0.08em]">EXPLORER</strong>
              <small className="text-[10px] tracking-[0.12em] text-[#9fc177]">
                MTB &amp; TURNO SMUČANJE
              </small>
            </span>
          </div>
          <span className="ml-auto rounded-full border border-amber-200/25 bg-amber-100/10 px-3 py-1 text-xs font-bold tracking-wide text-amber-100">
            LOKALNI MANAGEMENT PREDOGLED
          </span>
        </div>
      </header>

      <main className="mx-auto mb-16 w-[min(1420px,94vw)]">
        <div className="flex h-14 items-center text-sm text-stone-500">
          Tura #{tour.id} · podatki iz management API
        </div>

        <section className="tour-hero relative min-h-[430px] overflow-hidden rounded-2xl shadow-[0_12px_28px_rgba(20,30,23,0.12)]">
          <div className="absolute top-5 right-5 z-10 rounded-full border border-white/20 bg-black/45 px-3 py-1.5 text-xs font-extrabold tracking-[0.12em] text-white backdrop-blur uppercase">
            {tour.publicationStatus}
          </div>
          <div className="absolute right-6 bottom-5 z-10 text-xs text-white/60">
            Predogled brez naslovne fotografije
          </div>
          <div className="absolute right-6 bottom-16 left-6 z-10 max-w-3xl text-white sm:right-auto sm:bottom-12 sm:left-12">
            <p className="text-sm font-extrabold tracking-[0.12em] text-[#a7d66d] uppercase">
              MTB · {tour.publicationStatus === "DRAFT" ? "osnutek" : "objavljena tura"}
            </p>
            <h1 className="mt-3 text-4xl leading-tight font-bold tracking-[-0.035em] sm:text-6xl">
              {title}
            </h1>
            {locationParts.length ? (
              <p className="mt-2 text-lg text-stone-200">
                {locationParts.join(" · ")}
              </p>
            ) : null}
            {heroMetrics.length ? (
              <div className="mt-6 flex flex-wrap gap-2">
                {heroMetrics.map((metric) => (
                  <span
                    key={metric}
                    className="rounded-full border border-white/15 bg-black/55 px-3 py-2 text-sm backdrop-blur"
                  >
                    {metric}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
        </section>

        <section className="mt-6 grid gap-6 lg:grid-cols-[1.35fr_0.85fr]">
          <Card>
            <h2 className="text-2xl font-bold">O turi</h2>
            {tour.shortDescription ? (
              <p className="mt-4 text-lg leading-8 text-stone-700">
                {tour.shortDescription}
              </p>
            ) : (
              <p className="mt-4 italic text-stone-500">
                Kratek opis še ni dodan.
              </p>
            )}
            {tour.technicalDescription ? (
              <>
                <h3 className="mt-7 font-bold">Tehnični opis</h3>
                <p className="mt-2 whitespace-pre-line leading-7 text-stone-700">
                  {tour.technicalDescription}
                </p>
              </>
            ) : (
              <p className="mt-5 italic text-stone-500">
                Tehnični opis še ni dodan.
              </p>
            )}
            {locationParts.length ? (
              <div className="mt-6 flex flex-wrap gap-2">
                {locationParts.map((part) => (
                  <span
                    key={part}
                    className="rounded-full border border-[#dfe4dc] bg-[#eef1ea] px-3 py-2 text-sm font-bold text-[#315714]"
                  >
                    {part}
                  </span>
                ))}
              </div>
            ) : null}
            {tour.carryingPercentage !== null && tour.carryingPercentage > 0 ? (
              <div className="mt-6 rounded-lg border-l-4 border-[#4d7e20] bg-[#edf2e8] p-4 text-stone-700">
                Na približno {tour.carryingPercentage} % trase je pričakovano
                nošenje ali potiskanje kolesa.
              </div>
            ) : null}
          </Card>

          <Card>
            <h2 className="text-2xl font-bold">Ključni podatki</h2>
            {facts.length ? (
              <dl className="mt-2 grid gap-x-6 md:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
                {facts.map((fact) => (
                  <Fact key={fact.label} {...fact} />
                ))}
              </dl>
            ) : (
              <p className="mt-4 italic text-stone-500">
                Uredniški podatki še niso dodani.
              </p>
            )}
          </Card>
        </section>

        {metrics.length ? (
          <section className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
            {metrics.map((metric) => (
              <div
                key={metric.label}
                className="min-h-28 rounded-xl border border-[#dfe4dc] bg-white p-5 shadow-[0_12px_28px_rgba(20,30,23,0.08)]"
              >
                <small className="block text-stone-500">{metric.label}</small>
                <strong className="mt-2 block text-xl">{metric.value}</strong>
              </div>
            ))}
          </section>
        ) : null}

        <section className="mt-6 grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
          <Card>
            <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
              <div>
                <p className="text-sm font-extrabold tracking-[0.1em] text-[#4d7e20] uppercase">
                  Dejanska GPX trasa
                </p>
                <h2 className="mt-1 text-2xl font-bold">Zemljevid</h2>
              </div>
              {tour.startLocation ? (
                <div className="text-right text-sm">
                  <strong className="block">{tour.startLocation.name}</strong>
                  <span className="text-stone-500">
                    {tour.startLocation.latitude.toFixed(5)}, {tour.startLocation.longitude.toFixed(5)}
                  </span>
                </div>
              ) : null}
            </div>
            <TourMap
              routeGeometry={tour.routeGeometry}
              startLocation={tour.startLocation}
              styleUrl={mapStyleUrl}
            />
          </Card>

          <Card>
            <p className="text-sm font-extrabold tracking-[0.1em] text-[#4d7e20] uppercase">
              Meritve z dejanske trase
            </p>
            <h2 className="mt-1 text-2xl font-bold">Višinski profil</h2>
            <div className="mt-5">
              <ElevationProfile samples={tour.elevationProfile} />
            </div>

            {tour.surfaceComposition.length ? (
              <div className="mt-6 border-t border-stone-200 pt-5">
                <h3 className="font-bold">Podlaga</h3>
                <div className="mt-4 grid gap-3">
                  {tour.surfaceComposition.map((surface) => (
                    <div
                      key={surface.surfaceType}
                      className="grid grid-cols-[110px_1fr_48px] items-center gap-3 text-sm"
                    >
                      <span>{formatSurfaceType(surface.surfaceType)}</span>
                      <span className="h-2.5 overflow-hidden rounded-full bg-stone-100">
                        <span
                          className="block h-full rounded-full bg-[#4d7e20]"
                          style={{ width: `${surface.percentage}%` }}
                        />
                      </span>
                      <strong className="text-right">{surface.percentage} %</strong>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
          </Card>
        </section>
      </main>
    </>
  );
}
