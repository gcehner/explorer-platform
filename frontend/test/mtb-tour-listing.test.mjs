import assert from "node:assert/strict";
import test from "node:test";

import {
  EMPTY_MTB_TOUR_FILTERS,
  normalizeMapBounds,
  parseLatestTours,
  parseTourPage,
  serializeTourQuery,
} from "../src/lib/mtb-tour-listing.ts";

const validBounds = {
  west: 13.123456789,
  south: 45.9,
  east: 14.7,
  north: 46.8,
};

test("normalizes a representable initial map viewport", () => {
  assert.deepEqual(normalizeMapBounds(validBounds), validBounds);
});

test("rejects invalid and antimeridian-crossing map viewports", () => {
  assert.equal(normalizeMapBounds({ ...validBounds, west: 170, east: -170 }), null);
  assert.equal(normalizeMapBounds({ ...validBounds, east: Number.NaN }), null);
  assert.equal(normalizeMapBounds({ ...validBounds, east: 181 }), null);
});

test("serializes the bbox, supported filters, page size and opaque cursor", () => {
  const query = serializeTourQuery(
    validBounds,
    {
      countryCode: "SI",
      technicalDifficulty: ["S2", "S3"],
      distanceMinKm: 30,
      distanceMaxKm: 50,
      ascentMinMeters: 1000,
      ascentMaxMeters: 1500,
    },
    "opaque.cursor-value",
  );

  assert.equal(query.get("bbox"), "13.123457,45.9,14.7,46.8");
  assert.equal(query.get("countryCode"), "SI");
  assert.equal(query.get("technicalDifficulty"), "S2,S3");
  assert.equal(query.get("distanceMinKm"), "30");
  assert.equal(query.get("distanceMaxKm"), "50");
  assert.equal(query.get("ascentMinMeters"), "1000");
  assert.equal(query.get("ascentMaxMeters"), "1500");
  assert.equal(query.get("pageSize"), "24");
  assert.equal(query.get("cursor"), "opaque.cursor-value");
});

test("omits inactive filters", () => {
  const query = serializeTourQuery(validBounds, EMPTY_MTB_TOUR_FILTERS);

  assert.equal(query.has("countryCode"), false);
  assert.equal(query.has("technicalDifficulty"), false);
  assert.equal(query.has("cursor"), false);
});

test("maps the backend tour page without inventing presentation data", () => {
  const page = parseTourPage({
    items: [
      {
        id: 34,
        title: "Hochalpljoch",
        mountainRange: "Karavanke",
        countryCode: "AT",
        shortDescription: "Objavljena tura.",
        startLocation: {
          name: "Izhodišče",
          latitude: 46.5,
          longitude: 14.2,
        },
        distanceKm: 35.6,
        totalAscentMeters: 1650,
        estimatedDuration: { minimumHours: 5, maximumHours: 6.5 },
        technicalDifficulty: "S3",
        physicalDifficulty: 4,
        exposureRating: "E2",
        carryingPercentage: 10,
        routeType: "LOOP",
        publishedAt: "2026-01-15T10:30:00Z",
      },
    ],
    nextCursor: null,
    hasMore: false,
  });

  assert.equal(page.items[0].id, 34);
  assert.equal(page.items[0].distanceKm, 35.6);
  assert.equal(page.items[0].startLocation.name, "Izhodišče");
  assert.equal("coverImageUrl" in page.items[0], false);
  assert.equal("elevationProfile" in page.items[0], false);
});

test("rejects a paginated response without its required cursor", () => {
  assert.throws(
    () => parseTourPage({ items: [], nextCursor: null, hasMore: true }),
    /missing its cursor/,
  );
});

test("maps the global latest-tour response with the shared public summary contract", () => {
  const response = parseLatestTours({
    items: [
      {
        id: 32,
        title: "Published tour",
        mountainRange: "Julian Alps",
        countryCode: "SI",
        shortDescription: "Published summary.",
        startLocation: {
          name: "Start",
          latitude: 46.5,
          longitude: 14.2,
        },
        distanceKm: 30,
        totalAscentMeters: 1200,
        estimatedDuration: { minimumHours: 3, maximumHours: 4 },
        technicalDifficulty: "S2",
        physicalDifficulty: 3,
        exposureRating: "E2",
        carryingPercentage: 10,
        routeType: "LOOP",
        publishedAt: "2026-02-01T10:00:00Z",
      },
    ],
  });

  assert.equal(response.items.length, 1);
  assert.equal(response.items[0].id, 32);
  assert.equal(response.items[0].title, "Published tour");
  assert.equal("routeGeometry" in response.items[0], false);
  assert.equal("coverImageUrl" in response.items[0], false);
});

test("rejects malformed latest-tour responses instead of using fallback tours", () => {
  assert.throws(() => parseLatestTours({ items: "not-an-array" }), /Invalid latest tours/);
});
