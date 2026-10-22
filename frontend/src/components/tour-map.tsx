"use client";

import { useEffect, useRef, useState } from "react";
import * as maplibregl from "maplibre-gl";

import type { RouteGeometry, StartLocation } from "@/lib/tour-types";

maplibregl.setWorkerUrl(
  new URL(
    "maplibre-gl/dist/maplibre-gl-worker.mjs",
    import.meta.url,
  ).toString(),
);

interface TourMapProps {
  routeGeometry: RouteGeometry | null;
  startLocation: StartLocation | null;
  styleUrl: string;
}

function isCoordinate(value: number[]): value is [number, number] {
  return (
    value.length >= 2 &&
    Number.isFinite(value[0]) &&
    Number.isFinite(value[1])
  );
}

export function TourMap({
  routeGeometry,
  startLocation,
  styleUrl,
}: TourMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [mapError, setMapError] = useState<string | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    const coordinates = (routeGeometry?.coordinates ?? []).filter(isCoordinate);

    if (!container || coordinates.length < 2) {
      return;
    }

    const bounds = coordinates.reduce(
      (routeBounds, coordinate) => routeBounds.extend(coordinate),
      new maplibregl.LngLatBounds(coordinates[0], coordinates[0]),
    );

    const map = new maplibregl.Map({
      container,
      style: styleUrl,
      bounds,
      fitBoundsOptions: {
        padding: { top: 52, right: 52, bottom: 52, left: 52 },
        maxZoom: 15,
      },
      attributionControl: { compact: false },
    });

    map.addControl(new maplibregl.NavigationControl(), "top-right");

    map.once("load", () => {
      setMapError(null);

      map.addSource("tour-route", {
        type: "geojson",
        data: {
          type: "Feature",
          properties: {},
          geometry: {
            type: "LineString",
            coordinates,
          },
        },
      });

      map.addLayer({
        id: "tour-route-outline",
        type: "line",
        source: "tour-route",
        layout: {
          "line-cap": "round",
          "line-join": "round",
        },
        paint: {
          "line-color": "#ffffff",
          "line-opacity": 0.9,
          "line-width": 7,
        },
      });

      map.addLayer({
        id: "tour-route-line",
        type: "line",
        source: "tour-route",
        layout: {
          "line-cap": "round",
          "line-join": "round",
        },
        paint: {
          "line-color": "#4d7e20",
          "line-width": 4,
        },
      });

      if (startLocation) {
        new maplibregl.Marker({ color: "#17201a" })
          .setLngLat([startLocation.longitude, startLocation.latitude])
          .setPopup(
            new maplibregl.Popup({ offset: 24 }).setText(startLocation.name),
          )
          .addTo(map);
      }
    });

    map.on("error", () => {
      if (!map.isStyleLoaded()) {
        setMapError("Zemljevida trenutno ni mogoče naložiti.");
      }
    });

    return () => map.remove();
  }, [routeGeometry, startLocation, styleUrl]);

  if (!routeGeometry || routeGeometry.coordinates.length < 2) {
    return (
      <div className="flex min-h-96 items-center justify-center rounded-xl bg-stone-100 px-6 text-center text-stone-500">
        Trasa za to turo še ni na voljo.
      </div>
    );
  }

  return (
    <div className="relative h-96 overflow-hidden rounded-xl border border-stone-200 bg-stone-100">
      <div
        ref={containerRef}
        className="h-full w-full"
        aria-label="Interaktivni zemljevid trase"
      />
      {mapError ? (
        <div className="absolute inset-x-4 top-4 z-10 rounded-lg bg-white/95 p-3 text-sm text-red-800 shadow">
          {mapError}
        </div>
      ) : null}
    </div>
  );
}
