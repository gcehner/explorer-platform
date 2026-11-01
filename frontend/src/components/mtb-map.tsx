"use client";

import { useEffect, useRef, useState } from "react";
import * as maplibregl from "maplibre-gl";

maplibregl.setWorkerUrl(
  new URL("maplibre-gl/dist/maplibre-gl-worker.mjs", import.meta.url).toString(),
);

type MapStatus = "loading" | "ready" | "error";

export function MtbMap({ styleUrl }: { styleUrl: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<MapStatus>("loading");

  useEffect(() => {
    const container = containerRef.current;

    if (!container) {
      return;
    }

    let active = true;
    let map: maplibregl.Map | undefined;
    let resizeFrame: number | undefined;
    const resizeObserver = new ResizeObserver(() => {
      if (resizeFrame !== undefined) {
        cancelAnimationFrame(resizeFrame);
      }

      resizeFrame = requestAnimationFrame(() => map?.resize());
    });

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
        console.error("MTB listing map error", event.error);
        if (active && !map?.isStyleLoaded()) {
          setStatus("error");
        }
      });

      resizeObserver.observe(container);
    } catch (error) {
      console.error("MTB listing map initialization failed", error);
      queueMicrotask(() => {
        if (active) {
          setStatus("error");
        }
      });
    }

    return () => {
      active = false;
      resizeObserver.disconnect();
      if (resizeFrame !== undefined) {
        cancelAnimationFrame(resizeFrame);
      }
      map?.remove();
    };
  }, [styleUrl]);

  return (
    <section
      className="mtb-map relative h-full min-h-96 overflow-hidden bg-[#e9efe6]"
      aria-label="Zemljevid MTB tur"
    >
      <div
        ref={containerRef}
        className="absolute inset-0"
        aria-label="Interaktivni zemljevid alpskega prostora"
      />

      {status === "loading" ? (
        <div className="pointer-events-none absolute inset-0 z-10 grid place-items-center bg-[#e4e9e1] text-sm font-bold text-[#576158]">
          Nalagam zemljevid …
        </div>
      ) : null}

      {status === "error" ? (
        <div className="absolute inset-0 z-10 grid place-items-center bg-[#e7ebe4] px-8 text-center">
          <div className="max-w-sm rounded-xl border border-[#cbd3c7] bg-white/95 p-6 shadow-lg">
            <p className="font-bold">Zemljevida trenutno ni mogoče naložiti.</p>
            <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
              Preverite omrežno povezavo in poskusite znova.
            </p>
          </div>
        </div>
      ) : null}
    </section>
  );
}
