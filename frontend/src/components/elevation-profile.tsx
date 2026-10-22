import type { ElevationProfileSample } from "@/lib/tour-types";
import { formatDecimal, formatInteger } from "@/lib/tour-format";

interface ElevationProfileProps {
  samples: ElevationProfileSample[];
}

const WIDTH = 760;
const HEIGHT = 300;
const MARGIN = { top: 20, right: 20, bottom: 42, left: 66 };

export function ElevationProfile({ samples }: ElevationProfileProps) {
  if (samples.length < 2) {
    return (
      <div className="flex min-h-72 items-center justify-center rounded-xl bg-stone-50 px-6 text-center text-stone-500">
        Višinski profil za to turo še ni na voljo.
      </div>
    );
  }

  const minimumElevation = Math.min(
    ...samples.map((sample) => sample.elevationMeters),
  );
  const maximumElevation = Math.max(
    ...samples.map((sample) => sample.elevationMeters),
  );
  const maximumDistance = Math.max(
    ...samples.map((sample) => sample.distanceKm),
  );
  const elevationRange = Math.max(maximumElevation - minimumElevation, 1);
  const distanceRange = Math.max(maximumDistance, 0.1);
  const plotWidth = WIDTH - MARGIN.left - MARGIN.right;
  const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom;

  const points = samples.map((sample) => {
    const x = MARGIN.left + (sample.distanceKm / distanceRange) * plotWidth;
    const y =
      MARGIN.top +
      (1 - (sample.elevationMeters - minimumElevation) / elevationRange) *
        plotHeight;
    return { x, y };
  });
  const linePath = points
    .map((point, index) => `${index === 0 ? "M" : "L"}${point.x} ${point.y}`)
    .join(" ");
  const baseline = MARGIN.top + plotHeight;
  const areaPath = `${linePath} L${points.at(-1)?.x} ${baseline} L${points[0].x} ${baseline} Z`;
  const elevationTicks = [0, 0.5, 1].map((ratio) => ({
    y: MARGIN.top + ratio * plotHeight,
    value: maximumElevation - ratio * elevationRange,
  }));
  const distanceTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    x: MARGIN.left + ratio * plotWidth,
    value: ratio * maximumDistance,
  }));

  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      className="h-auto w-full"
      role="img"
      aria-label={`Višinski profil od ${formatInteger(minimumElevation)} do ${formatInteger(maximumElevation)} metrov`}
    >
      <defs>
        <linearGradient id="elevation-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#6e9d40" stopOpacity="0.5" />
          <stop offset="100%" stopColor="#6e9d40" stopOpacity="0.06" />
        </linearGradient>
      </defs>

      {elevationTicks.map((tick) => (
        <g key={tick.y}>
          <line
            x1={MARGIN.left}
            x2={WIDTH - MARGIN.right}
            y1={tick.y}
            y2={tick.y}
            stroke="#dfe4dc"
          />
          <text
            x={MARGIN.left - 10}
            y={tick.y + 4}
            textAnchor="end"
            className="fill-stone-500 text-[12px]"
          >
            {formatInteger(tick.value)} m
          </text>
        </g>
      ))}

      <path d={areaPath} fill="url(#elevation-fill)" />
      <path
        d={linePath}
        fill="none"
        stroke="#4d7e20"
        strokeWidth="3"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />

      {distanceTicks.map((tick) => (
        <text
          key={tick.x}
          x={tick.x}
          y={HEIGHT - 14}
          textAnchor={tick.value === 0 ? "start" : tick.value === maximumDistance ? "end" : "middle"}
          className="fill-stone-500 text-[12px]"
        >
          {formatDecimal(tick.value)} km
        </text>
      ))}
    </svg>
  );
}
