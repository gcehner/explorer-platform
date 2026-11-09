import type {
  DurationRange,
  ExposureRating,
  RouteType,
  StartLocation,
  TechnicalDifficulty,
} from "@/lib/tour-types";

export const MTB_TOUR_PAGE_SIZE = 24;

export interface MtbTourSummary {
  id: number;
  title: string;
  mountainRange: string | null;
  countryCode: string | null;
  shortDescription: string | null;
  startLocation: StartLocation | null;
  distanceKm: number | null;
  totalAscentMeters: number | null;
  estimatedDuration: DurationRange | null;
  technicalDifficulty: TechnicalDifficulty | null;
  physicalDifficulty: number | null;
  exposureRating: ExposureRating | null;
  carryingPercentage: number | null;
  routeType: RouteType | null;
  publishedAt: string;
}

export interface MtbTourPage {
  items: MtbTourSummary[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface MtbLatestTours {
  items: MtbTourSummary[];
}

export interface MtbBoundingBox {
  west: number;
  south: number;
  east: number;
  north: number;
}

export interface MtbTourFilters {
  countryCode: string | null;
  technicalDifficulty: TechnicalDifficulty[];
  distanceMinKm: number | null;
  distanceMaxKm: number | null;
  ascentMinMeters: number | null;
  ascentMaxMeters: number | null;
}

export const EMPTY_MTB_TOUR_FILTERS: MtbTourFilters = {
  countryCode: null,
  technicalDifficulty: [],
  distanceMinKm: null,
  distanceMaxKm: null,
  ascentMinMeters: null,
  ascentMaxMeters: null,
};

export type TourListingStatus =
  | "waiting-for-map"
  | "loading"
  | "ready"
  | "error"
  | "invalid-bounds";

export function normalizeMapBounds(bounds: MtbBoundingBox): MtbBoundingBox | null {
  const { west, south, east, north } = bounds;

  if (![west, south, east, north].every(Number.isFinite)) {
    return null;
  }
  if (west < -180 || east > 180 || south < -90 || north > 90) {
    return null;
  }
  if (west >= east || south >= north) {
    return null;
  }

  return { west, south, east, north };
}

function formatCoordinate(value: number): string {
  return Number(value.toFixed(6)).toString();
}

export function serializeTourQuery(
  bounds: MtbBoundingBox,
  filters: MtbTourFilters,
  cursor?: string | null,
): URLSearchParams {
  const normalizedBounds = normalizeMapBounds(bounds);
  if (!normalizedBounds) {
    throw new Error("Invalid MTB discovery bounds.");
  }

  const query = new URLSearchParams({
    bbox: [
      normalizedBounds.west,
      normalizedBounds.south,
      normalizedBounds.east,
      normalizedBounds.north,
    ]
      .map(formatCoordinate)
      .join(","),
    pageSize: MTB_TOUR_PAGE_SIZE.toString(),
  });

  if (filters.countryCode) query.set("countryCode", filters.countryCode);
  if (filters.technicalDifficulty.length > 0) {
    query.set("technicalDifficulty", filters.technicalDifficulty.join(","));
  }
  if (filters.distanceMinKm !== null) {
    query.set("distanceMinKm", filters.distanceMinKm.toString());
  }
  if (filters.distanceMaxKm !== null) {
    query.set("distanceMaxKm", filters.distanceMaxKm.toString());
  }
  if (filters.ascentMinMeters !== null) {
    query.set("ascentMinMeters", filters.ascentMinMeters.toString());
  }
  if (filters.ascentMaxMeters !== null) {
    query.set("ascentMaxMeters", filters.ascentMaxMeters.toString());
  }
  if (cursor) query.set("cursor", cursor);

  return query;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function nullableNumber(value: unknown): number | null {
  if (value === null) return null;
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error("Invalid numeric tour summary field.");
  }
  return value;
}

function nullableString(value: unknown): string | null {
  if (value === null) return null;
  if (typeof value !== "string") {
    throw new Error("Invalid text tour summary field.");
  }
  return value;
}

function parseTourSummary(value: unknown): MtbTourSummary {
  if (
    !isRecord(value) ||
    typeof value.id !== "number" ||
    typeof value.title !== "string" ||
    typeof value.publishedAt !== "string"
  ) {
    throw new Error("Invalid tour summary.");
  }

  let startLocation: StartLocation | null = null;
  if (value.startLocation !== null) {
    if (
      !isRecord(value.startLocation) ||
      typeof value.startLocation.name !== "string" ||
      typeof value.startLocation.latitude !== "number" ||
      typeof value.startLocation.longitude !== "number"
    ) {
      throw new Error("Invalid tour start location.");
    }
    startLocation = {
      name: value.startLocation.name,
      latitude: value.startLocation.latitude,
      longitude: value.startLocation.longitude,
    };
  }

  let estimatedDuration: DurationRange | null = null;
  if (value.estimatedDuration !== null) {
    if (
      !isRecord(value.estimatedDuration) ||
      typeof value.estimatedDuration.minimumHours !== "number" ||
      typeof value.estimatedDuration.maximumHours !== "number"
    ) {
      throw new Error("Invalid estimated tour duration.");
    }
    estimatedDuration = {
      minimumHours: value.estimatedDuration.minimumHours,
      maximumHours: value.estimatedDuration.maximumHours,
    };
  }

  return {
    id: value.id,
    title: value.title,
    mountainRange: nullableString(value.mountainRange),
    countryCode: nullableString(value.countryCode),
    shortDescription: nullableString(value.shortDescription),
    startLocation,
    distanceKm: nullableNumber(value.distanceKm),
    totalAscentMeters: nullableNumber(value.totalAscentMeters),
    estimatedDuration,
    technicalDifficulty: nullableString(value.technicalDifficulty) as TechnicalDifficulty | null,
    physicalDifficulty: nullableNumber(value.physicalDifficulty),
    exposureRating: nullableString(value.exposureRating) as ExposureRating | null,
    carryingPercentage: nullableNumber(value.carryingPercentage),
    routeType: nullableString(value.routeType) as RouteType | null,
    publishedAt: value.publishedAt,
  };
}

export function parseTourPage(value: unknown): MtbTourPage {
  if (
    !isRecord(value) ||
    !Array.isArray(value.items) ||
    typeof value.hasMore !== "boolean" ||
    (value.nextCursor !== null && typeof value.nextCursor !== "string")
  ) {
    throw new Error("Invalid tour page response.");
  }
  if (value.hasMore && !value.nextCursor) {
    throw new Error("A paginated tour response is missing its cursor.");
  }

  return {
    items: value.items.map(parseTourSummary),
    nextCursor: value.nextCursor,
    hasMore: value.hasMore,
  };
}

export function parseLatestTours(value: unknown): MtbLatestTours {
  if (!isRecord(value) || !Array.isArray(value.items)) {
    throw new Error("Invalid latest tours response.");
  }

  return { items: value.items.map(parseTourSummary) };
}

export async function fetchMtbTours(
  bounds: MtbBoundingBox,
  filters: MtbTourFilters,
  signal: AbortSignal,
  cursor?: string | null,
): Promise<MtbTourPage> {
  const query = serializeTourQuery(bounds, filters, cursor);
  const response = await fetch(`/api/tours?${query.toString()}`, {
    headers: { Accept: "application/json" },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Tour listing request failed with HTTP ${response.status}.`);
  }

  return parseTourPage(await response.json());
}

export async function fetchLatestMtbTours(
  signal: AbortSignal,
  limit = 6,
): Promise<MtbLatestTours> {
  const response = await fetch(`/api/tours/latest?limit=${limit}`, {
    headers: { Accept: "application/json" },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Latest tours request failed with HTTP ${response.status}.`);
  }

  return parseLatestTours(await response.json());
}
