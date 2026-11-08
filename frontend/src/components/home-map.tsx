"use client";

import { useEffect, useRef, useState } from "react";
import * as maplibregl from "maplibre-gl";

maplibregl.setWorkerUrl(
  new URL(
    "maplibre-gl/dist/maplibre-gl-worker.mjs",
    import.meta.url,
  ).toString(),
);

interface HomeMapProps {
  styleUrl: string;
}

type MapStatus = "loading" | "ready" | "error";

export function HomeMap({ styleUrl }: HomeMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<MapStatus>("loading");

  useEffect(() => {
    const container = containerRef.current;

    if (!container) {
      return;
    }

    let map: maplibregl.Map | undefined;
    let active = true;
    let resizeFrame: number | undefined;
    let resizeObserver: ResizeObserver | undefined;

    try {
      map = new maplibregl.Map({
        container,
        style: styleUrl,
        center: [14.02, 46.2],
        zoom: 8.4,
        minZoom: 4,
        maxZoom: 17,
        attributionControl: { compact: false },
      });

      map.addControl(new maplibregl.NavigationControl(), "top-right");
      map.addControl(new maplibregl.FullscreenControl(), "top-right");

      map.once("load", () => {
        if (active) {
          map?.resize();
          setStatus("ready");
        }
      });
      map.on("error", (event) => {
        console.error("Homepage map error", event.error);
        if (active && !map?.isStyleLoaded()) {
          setStatus("error");
        }
      });

      resizeObserver = new ResizeObserver(() => {
        if (resizeFrame !== undefined) {
          cancelAnimationFrame(resizeFrame);
        }

        resizeFrame = requestAnimationFrame(() => map?.resize());
      });
      resizeObserver.observe(container);
    } catch (error) {
      console.error("Homepage map initialization failed", error);
      queueMicrotask(() => {
        if (active) {
          setStatus("error");
        }
      });
    }

    return () => {
      active = false;
      resizeObserver?.disconnect();
      if (resizeFrame !== undefined) {
        cancelAnimationFrame(resizeFrame);
      }
      map?.remove();
    };
  }, [styleUrl]);

  return (
    <div className="home-map">
      <div
        ref={containerRef}
        className="h-full w-full"
        aria-label="Interaktivni zemljevid alpskega prostora"
      />

      {status === "loading" ? (
        <div className="pointer-events-none absolute inset-0 z-20 grid place-items-center bg-[#e4e9e1] text-sm font-bold text-[#576158]">
          Nalagam zemljevid …
        </div>
      ) : null}

      {status === "error" ? (
        <div className="absolute inset-0 z-20 grid place-items-center bg-[#e7ebe4] px-8 text-center">
          <div className="max-w-sm border border-[#cbd3c7] bg-white/90 p-6 shadow-lg">
            <p className="font-extrabold text-[#17201a]">
              Zemljevida trenutno ni mogoče naložiti.
            </p>
            <p className="mt-2 text-sm leading-6 text-[#687168]">
              Preverite omrežno povezavo in poskusite znova.
            </p>
          </div>
        </div>
      ) : null}

      <div className="home-map-filterbar">
        <button
          className="home-map-filter"
          type="button"
          disabled
          aria-describedby="home-map-filter-status"
        >
          <span className="home-map-filter-check" aria-hidden="true">✓</span>
          MTB
        </button>
        <span className="home-map-filter-status" id="home-map-filter-status">
          Filtri bodo na voljo z javnim seznamom tur.
        </span>
      </div>
    </div>
  );
}
