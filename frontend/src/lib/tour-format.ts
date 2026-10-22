import type {
  BikeType,
  RouteType,
  SurfaceType,
} from "@/lib/tour-types";

const decimalFormatter = new Intl.NumberFormat("sl-SI", {
  maximumFractionDigits: 1,
});

const integerFormatter = new Intl.NumberFormat("sl-SI", {
  maximumFractionDigits: 0,
});

export function formatDecimal(value: number): string {
  return decimalFormatter.format(value);
}

export function formatInteger(value: number): string {
  return integerFormatter.format(value);
}

export function formatRouteType(value: RouteType): string {
  return value === "LOOP" ? "Krožna" : "Od točke do točke";
}

const bikeTypeLabels: Record<BikeType, string> = {
  GRAVEL: "Gravel",
  XC: "Cross-country",
  ALL_MOUNTAIN: "All Mountain",
  ENDURO: "Enduro",
};

export function formatBikeType(value: BikeType): string {
  return bikeTypeLabels[value];
}

const surfaceTypeLabels: Record<SurfaceType, string> = {
  ASPHALT: "Asfalt",
  GRAVEL: "Makadam",
  FOREST_ROAD: "Gozdna cesta",
  SINGLETRACK: "Singletrack",
  DOUBLETRACK: "Dvokolesnica",
};

export function formatSurfaceType(value: SurfaceType): string {
  return surfaceTypeLabels[value];
}

export function formatCountry(countryCode: string): string {
  try {
    return (
      new Intl.DisplayNames(["sl"], { type: "region" }).of(countryCode) ??
      countryCode
    );
  } catch {
    return countryCode;
  }
}
