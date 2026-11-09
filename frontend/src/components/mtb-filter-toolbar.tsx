"use client";

import { useState } from "react";

import type {
  MtbTourFilters,
} from "@/lib/mtb-tour-listing";
import type { TechnicalDifficulty } from "@/lib/tour-types";

interface MtbFilterToolbarProps {
  filters: MtbTourFilters;
  onChange: (filters: MtbTourFilters) => void;
}

const technicalLevels: TechnicalDifficulty[] = ["S0", "S1", "S2", "S3", "S4", "S5"];

function hasActiveFilters(filters: MtbTourFilters) {
  return (
    filters.countryCode !== null ||
    filters.technicalDifficulty.length > 0 ||
    filters.distanceMinKm !== null ||
    filters.distanceMaxKm !== null ||
    filters.ascentMinMeters !== null ||
    filters.ascentMaxMeters !== null
  );
}

export function MtbFilterToolbar({
  filters,
  onChange,
}: MtbFilterToolbarProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const active = hasActiveFilters(filters);
  const distanceValue =
    filters.distanceMaxKm === 30
      ? "under-30"
      : filters.distanceMinKm === 30 && filters.distanceMaxKm === 50
        ? "30-50"
        : filters.distanceMinKm === 50
          ? "over-50"
          : "all";
  const ascentValue =
    filters.ascentMaxMeters === 1000
      ? "under-1000"
      : filters.ascentMinMeters === 1000 && filters.ascentMaxMeters === 1500
        ? "1000-1500"
        : filters.ascentMinMeters === 1500
          ? "over-1500"
          : "all";

  const openFilters = () => setAdvancedOpen(true);
  const reset = () =>
    onChange({
      countryCode: null,
      technicalDifficulty: [],
      distanceMinKm: null,
      distanceMaxKm: null,
      ascentMinMeters: null,
      ascentMaxMeters: null,
    });

  return (
    <section
      className="relative z-10 flex-none border-b border-[var(--color-border)] bg-white px-4 py-3 sm:px-7"
      aria-label="Filtri MTB tur"
    >
      <div className="flex flex-wrap items-center gap-2">
        <button
          className={`flex min-h-10 items-center rounded-lg border px-3 text-xs font-semibold transition-colors ${
            !active
              ? "border-[#d4e1cc] bg-[var(--color-surface-soft)] text-[var(--color-mtb-dark)]"
              : "border-[#d9e0d7] bg-white text-[#39483b] hover:border-[#88a476]"
          }`}
          type="button"
          onClick={reset}
        >
          Vse ture
        </button>

        <button
          className="flex min-h-10 cursor-not-allowed items-center gap-4 rounded-lg border border-[#d9e0d7] bg-white px-3 text-xs font-semibold text-[#39483b] opacity-55"
          type="button"
          disabled
          title="Filtriranje po gorovju ali območju v javnem API-ju še ni podprto."
        >
          Območje
          <span className="text-[#889387]" aria-hidden="true">⌄</span>
        </button>

        {[
          ["Težavnost", filters.technicalDifficulty.length > 0],
          ["Dolžina", distanceValue !== "all"],
          ["Vzpon", ascentValue !== "all"],
        ].map(([label, selected]) => (
          <button
            key={String(label)}
            className={`flex min-h-10 items-center gap-4 rounded-lg border px-3 text-xs font-semibold transition-colors hover:border-[#88a476] ${
              selected
                ? "border-[#9cb98b] bg-[#e6f0df] text-[var(--color-mtb-dark)]"
                : "border-[#d9e0d7] bg-white text-[#39483b]"
            }`}
            type="button"
            onClick={openFilters}
          >
            {label}
            <span className="text-[#889387]" aria-hidden="true">⌄</span>
          </button>
        ))}

        <button
          className="flex min-h-10 items-center gap-3 rounded-lg border border-[#d9e0d7] bg-white px-3 text-xs font-semibold text-[#39483b] transition-colors hover:border-[#88a476] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-mtb-dark)] xl:ml-auto"
          type="button"
          aria-expanded={advancedOpen}
          aria-controls="mtb-advanced-filters"
          onClick={() => setAdvancedOpen((open) => !open)}
        >
          <span aria-hidden="true">☷</span>
          Vsi filtri
          <span className="text-[#889387]" aria-hidden="true">
            {advancedOpen ? "⌃" : "⌄"}
          </span>
        </button>

        <button
          className="min-h-10 rounded-lg px-2 text-xs font-medium text-[var(--color-muted)] disabled:cursor-not-allowed disabled:opacity-40"
          type="button"
          disabled={!active}
          onClick={reset}
        >
          ↻ Ponastavi
        </button>
      </div>

      {advancedOpen ? (
        <div
          id="mtb-advanced-filters"
          className="mt-3 grid gap-4 rounded-xl border border-[var(--color-border)] bg-[#f9faf8] p-4 sm:grid-cols-2 lg:grid-cols-3"
        >
          <label className="block">
            <span className="mb-1.5 block text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
              Država
            </span>
            <select
              className="h-10 w-full rounded-lg border border-[var(--color-border)] bg-white px-2.5 text-xs"
              value={filters.countryCode ?? ""}
              onChange={(event) =>
                onChange({
                  ...filters,
                  countryCode: event.target.value || null,
                })
              }
            >
              <option value="">Vse države</option>
              <option value="SI">Slovenija</option>
              <option value="IT">Italija</option>
              <option value="AT">Avstrija</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
              Dolžina
            </span>
            <select
              className="h-10 w-full rounded-lg border border-[var(--color-border)] bg-white px-2.5 text-xs"
              value={distanceValue}
              onChange={(event) => {
                const value = event.target.value;
                onChange({
                  ...filters,
                  distanceMinKm: value === "30-50" ? 30 : value === "over-50" ? 50 : null,
                  distanceMaxKm: value === "under-30" ? 30 : value === "30-50" ? 50 : null,
                });
              }}
            >
              <option value="all">Vse dolžine</option>
              <option value="under-30">Do 30 km</option>
              <option value="30-50">30–50 km</option>
              <option value="over-50">50 km ali več</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
              Skupni vzpon
            </span>
            <select
              className="h-10 w-full rounded-lg border border-[var(--color-border)] bg-white px-2.5 text-xs"
              value={ascentValue}
              onChange={(event) => {
                const value = event.target.value;
                onChange({
                  ...filters,
                  ascentMinMeters:
                    value === "1000-1500" ? 1000 : value === "over-1500" ? 1500 : null,
                  ascentMaxMeters:
                    value === "under-1000" ? 1000 : value === "1000-1500" ? 1500 : null,
                });
              }}
            >
              <option value="all">Vsi vzponi</option>
              <option value="under-1000">Do 1000 m</option>
              <option value="1000-1500">1000–1500 m</option>
              <option value="over-1500">1500 m ali več</option>
            </select>
          </label>

          <fieldset className="sm:col-span-2 lg:col-span-3">
            <legend className="mb-1.5 text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
              Tehnična težavnost spusta
            </legend>
            <div className="flex flex-wrap gap-1.5">
              {technicalLevels.map((level) => {
                const selected = filters.technicalDifficulty.includes(level);
                return (
                  <button
                    key={level}
                    className={`rounded-md border px-3 py-2 text-xs ${
                      selected
                        ? "border-[#9cb98b] bg-[#e6f0df] font-bold text-[var(--color-mtb-dark)]"
                        : "border-[var(--color-border)] bg-white text-[var(--color-muted)]"
                    }`}
                    type="button"
                    aria-pressed={selected}
                    onClick={() =>
                      onChange({
                        ...filters,
                        technicalDifficulty: selected
                          ? filters.technicalDifficulty.filter((value) => value !== level)
                          : [...filters.technicalDifficulty, level],
                      })
                    }
                  >
                    {level}
                  </button>
                );
              })}
            </div>
          </fieldset>

          <p className="text-xs leading-5 text-[var(--color-muted)] sm:col-span-2 lg:col-span-3">
            Filtriranje po območju in drugi uredniški filtri še niso podprti v javnem API-ju.
          </p>
        </div>
      ) : null}
    </section>
  );
}
