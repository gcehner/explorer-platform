"use client";

import { useEffect, useState } from "react";

import {
  fetchLatestMtbTours,
  type MtbTourSummary,
} from "@/lib/mtb-tour-listing";
import {
  formatCountry,
  formatDecimal,
  formatInteger,
} from "@/lib/tour-format";

type LatestToursStatus = "loading" | "ready" | "error";

const dateFormat = new Intl.DateTimeFormat("sl-SI", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

function HomeLatestTourCard({ tour }: { tour: MtbTourSummary }) {
  const country = tour.countryCode ? formatCountry(tour.countryCode) : null;
  const location = [tour.mountainRange, country].filter(Boolean).join(" · ");
  const difficulty = [tour.technicalDifficulty, tour.exposureRating]
    .filter(Boolean)
    .join(" · ");
  const publishedAt = new Date(tour.publishedAt);
  const publishedDate = Number.isNaN(publishedAt.getTime())
    ? null
    : dateFormat.format(publishedAt);

  return (
    <article className="home-latest-card">
      <div className="home-latest-image" aria-hidden="true">
        <span>MTB</span>
        <b>△</b>
      </div>
      <div className="home-latest-body">
        <h3>{tour.title}</h3>
        {location ? <div className="home-latest-region">{location}</div> : null}
        {tour.shortDescription ? <p>{tour.shortDescription}</p> : null}
        <div className="home-latest-meta">
          {tour.distanceKm !== null ? (
            <span>↗ {formatDecimal(tour.distanceKm)} km</span>
          ) : null}
          {tour.totalAscentMeters !== null ? (
            <span>△ {formatInteger(tour.totalAscentMeters)} m</span>
          ) : null}
          {tour.estimatedDuration ? (
            <span>
              ◷ {formatDecimal(tour.estimatedDuration.minimumHours)}–
              {formatDecimal(tour.estimatedDuration.maximumHours)} h
            </span>
          ) : null}
          {difficulty ? <span>{difficulty}</span> : null}
          {publishedDate ? (
            <time dateTime={tour.publishedAt}>{publishedDate}</time>
          ) : null}
        </div>
      </div>
    </article>
  );
}

export function HomeLatestTours() {
  const [retryGeneration, setRetryGeneration] = useState(0);
  const [status, setStatus] = useState<LatestToursStatus>("loading");
  const [tours, setTours] = useState<MtbTourSummary[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => {
      if (!controller.signal.aborted) setStatus("loading");
    });

    void fetchLatestMtbTours(controller.signal)
      .then((response) => {
        if (controller.signal.aborted) return;
        setTours(response.items);
        setStatus("ready");
      })
      .catch(() => {
        if (controller.signal.aborted) return;
        setTours([]);
        setStatus("error");
      });

    return () => controller.abort();
  }, [retryGeneration]);

  if (status === "loading") {
    return (
      <div className="home-latest-loading" aria-live="polite" aria-busy="true">
        <div />
        <div />
        <span className="sr-only">Nalagam najnovejše ture …</span>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="home-tour-empty" role="alert">
        <span className="home-tour-empty-icon" aria-hidden="true">△</span>
        <div>
          <h3>Tur trenutno ni mogoče naložiti</h3>
          <p>Preverite povezavo in poskusite znova.</p>
          <button
            type="button"
            className="home-latest-retry"
            onClick={() => setRetryGeneration((generation) => generation + 1)}
          >
            Poskusi znova
          </button>
        </div>
      </div>
    );
  }

  if (tours.length === 0) {
    return (
      <div className="home-tour-empty">
        <span className="home-tour-empty-icon" aria-hidden="true">△</span>
        <div>
          <h3>Objavljenih tur še ni</h3>
          <p>Najnovejše objavljene MTB ture bodo prikazane tukaj.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="home-latest-list">
      {tours.map((tour) => (
        <HomeLatestTourCard key={tour.id} tour={tour} />
      ))}
    </div>
  );
}
