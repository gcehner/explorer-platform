export type PublicationStatus = "DRAFT" | "PUBLISHED";
export type TechnicalDifficulty = "S0" | "S1" | "S2" | "S3" | "S4" | "S5";
export type ExposureRating = "E1" | "E2" | "E3" | "E4";
export type RouteType = "LOOP" | "POINT_TO_POINT";
export type BikeType = "GRAVEL" | "XC" | "ALL_MOUNTAIN" | "ENDURO";
export type SurfaceType =
  | "ASPHALT"
  | "GRAVEL"
  | "FOREST_ROAD"
  | "SINGLETRACK"
  | "DOUBLETRACK";

export interface StartLocation {
  name: string;
  latitude: number;
  longitude: number;
}

export interface TourStatistics {
  distanceKm: number;
  totalAscentMeters: number;
  totalDescentMeters: number;
  lowestPointMeters: number;
  highestPointMeters: number;
}

export interface DurationRange {
  minimumHours: number;
  maximumHours: number;
}

export interface ElevationProfileSample {
  distanceKm: number;
  elevationMeters: number;
}

export interface RouteGeometry {
  type: "LineString";
  coordinates: number[][];
}

export interface SurfaceShare {
  surfaceType: SurfaceType;
  percentage: number;
}

export interface Photo {
  id: number;
  imageReference: string;
  caption: string | null;
  displayOrder: number;
}

export interface TourDetail {
  id: number;
  title: string | null;
  mountainRange: string | null;
  countryCode: string | null;
  startLocation: StartLocation | null;
  shortDescription: string | null;
  technicalDescription: string | null;
  statistics: TourStatistics | null;
  estimatedDuration: DurationRange | null;
  tourDate: string | null;
  technicalDifficulty: TechnicalDifficulty | null;
  physicalDifficulty: number | null;
  exposureRating: ExposureRating | null;
  scenicRating: number | null;
  carryingPercentage: number | null;
  routeType: RouteType | null;
  recommendedBikeTypes: BikeType[];
  surfaceComposition: SurfaceShare[];
  publicationStatus: PublicationStatus;
  publishedAt: string | null;
  elevationProfile: ElevationProfileSample[];
  routeGeometry: RouteGeometry | null;
  photos: Photo[];
  coverPhotoId: number | null;
  youtubeVideoId: string | null;
}
