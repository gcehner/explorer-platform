"use client";

import { useState } from "react";

const filterLabels = ["Vse ture", "Območje", "Težavnost", "Dolžina", "Vzpon"];
const advancedFields = [
  "Tip ture",
  "Država",
  "Gorovje / območje",
  "Dolžina",
  "Skupni vzpon",
  "Fizična težavnost",
  "Izpostavljenost",
  "Vrsta kolesa",
  "Podlaga",
  "Trajanje",
  "Najvišja točka",
  "Nošenje / potiskanje",
] as const;

const unavailableMessage =
  "Filtri bodo na voljo, ko bo vzpostavljen javni seznam tur.";

export function MtbFilterToolbar() {
  const [advancedOpen, setAdvancedOpen] = useState(false);

  return (
    <section
      className="relative z-10 flex-none border-b border-[var(--color-border)] bg-white px-4 py-3 sm:px-7"
      aria-label="Filtri MTB tur"
    >
      <div className="flex flex-wrap items-center gap-2">
        {filterLabels.map((label, index) => (
          <button
            key={label}
            className={`flex min-h-10 cursor-not-allowed items-center gap-4 rounded-lg border px-3 text-xs font-semibold opacity-65 ${
              index === 0
                ? "border-[#d4e1cc] bg-[var(--color-surface-soft)] text-[var(--color-mtb-dark)]"
                : "border-[#d9e0d7] bg-white text-[#39483b]"
            }`}
            type="button"
            disabled
            title={unavailableMessage}
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
          className="min-h-10 cursor-not-allowed rounded-lg px-2 text-xs font-medium text-[var(--color-muted)] opacity-55"
          type="button"
          disabled
          title={unavailableMessage}
        >
          ↻ Ponastavi
        </button>
      </div>

      {advancedOpen ? (
        <div
          id="mtb-advanced-filters"
          className="mt-3 grid gap-4 rounded-xl border border-[var(--color-border)] bg-[#f9faf8] p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
        >
          {advancedFields.map((label) => (
            <label key={label} className="block">
              <span className="mb-1.5 block text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
                {label}
              </span>
              <select
                className="h-10 w-full cursor-not-allowed rounded-lg border border-[var(--color-border)] bg-white px-2.5 text-xs text-[var(--color-muted)] opacity-65"
                disabled
                aria-label={label}
              >
                <option>Na voljo z javnim katalogom</option>
              </select>
            </label>
          ))}

          <fieldset className="sm:col-span-2 lg:col-span-3 xl:col-span-4">
            <legend className="mb-1.5 text-[11px] font-bold tracking-[0.04em] text-[#647065] uppercase">
              Tehnična težavnost spusta
            </legend>
            <div className="flex flex-wrap gap-1.5">
              {["S0", "S1", "S2", "S3", "S4", "S5"].map((level) => (
                <button
                  key={level}
                  className="cursor-not-allowed rounded-md border border-[var(--color-border)] bg-white px-3 py-2 text-xs text-[var(--color-muted)] opacity-65"
                  type="button"
                  disabled
                >
                  {level}
                </button>
              ))}
            </div>
          </fieldset>

          <p className="text-xs leading-5 text-[var(--color-muted)] sm:col-span-2 lg:col-span-3 xl:col-span-4">
            {unavailableMessage}
          </p>
        </div>
      ) : null}
    </section>
  );
}
