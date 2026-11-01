import type {
  DurationRange,
  ElevationProfileSample,
  TechnicalDifficulty,
} from "@/lib/tour-types";

/** View model expected from the future public MTB collection endpoint. */
export interface MtbTourSummary {
  id: number;
  title: string;
  mountainRange: string | null;
  countryCode: string | null;
  coverImageUrl: string | null;
  distanceKm: number | null;
  totalAscentMeters: number | null;
  estimatedDuration: DurationRange | null;
  technicalDifficulty: TechnicalDifficulty | null;
  elevationProfile: ElevationProfileSample[];
}

export type TourListingStatus = "ready" | "loading" | "error";
