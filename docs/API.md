# Public API

Management GPX imports accept optional `sourceProvider` and `sourceActivityId` metadata. Both values
must be present together. Garmin imports use `GARMIN_CONNECT` and a canonical positive decimal
activity ID. A new import returns HTTP 201; an already imported source returns HTTP 200 with the
existing `{ "tourId": ..., "status": ... }` response without changing that tour.

The CLI can perform a pre-download duplicate check with:

```text
GET /api/management/imports/gpx/sources/GARMIN_CONNECT/{activityId}
```

The endpoint returns HTTP 200 with the existing import response or HTTP 404 when the source identity
has not been imported. Database uniqueness remains authoritative for concurrent requests.

## MTB tour discovery

### List latest published tours

```text
GET /api/tours/latest?limit=6
```

Returns the newest published MTB tours across the entire catalog. This endpoint is independent of
map bounds and discovery filters. Results use the existing lightweight public tour summary DTO and
are ordered deterministically by `publishedAt DESC, id DESC`.

`limit` is optional, defaults to 6, and must be an integer from 1 through 12. The response is not
cursor-paginated:

```json
{
  "items": []
}
```

Drafts are excluded. Invalid limits return HTTP 400 using the standard
`{ "message": "..." }` error body.

### List published tours

```text
GET /api/tours
```

The endpoint returns lightweight summaries of published MTB tours whose stored route midpoint is
inside the requested geographic viewport. Draft tours are never returned. The endpoint does not
return route geometry, elevation profiles, GPX storage data or photo storage references.

#### Query parameters

`bbox` is required and uses longitude/latitude order:

```text
bbox=west,south,east,north
```

The box is inclusive: a discovery point on its boundary is included. Values must be finite,
longitudes must be between `-180` and `180`, latitudes between `-90` and `90`, south must be less
than north, and west must be less than east.

Discovery endpoints do not currently support antimeridian-crossing boxes. Frontends must clamp map bounds to the valid
longitude/latitude ranges and must not send a wrapped box where west is greater than east.

Optional filters:

| Parameter | Format |
| --- | --- |
| `countryCode` | ISO 3166-1 alpha-2 code; normalized to uppercase |
| `technicalDifficulty` | Comma-separated S0-S5 values, for example `S2,S3` |
| `distanceMinKm` | Non-negative decimal |
| `distanceMaxKm` | Non-negative decimal |
| `ascentMinMeters` | Non-negative integer |
| `ascentMaxMeters` | Non-negative integer |
| `pageSize` | 1-50; default 24 |
| `cursor` | Opaque cursor returned by the preceding page |

Minimum values must not exceed their corresponding maximum values.

Example:

```text
GET /api/tours?bbox=13.2,45.9,14.7,46.8&technicalDifficulty=S2,S3&pageSize=24
```

#### Response

```json
{
  "items": [
    {
      "id": 123,
      "title": "Published tour title",
      "mountainRange": "Editorial geographic area",
      "countryCode": "SI",
      "shortDescription": "Published short description.",
      "startLocation": {
        "name": "Start location",
        "latitude": 46.1234567,
        "longitude": 14.1234567
      },
      "distanceKm": 35.60,
      "totalAscentMeters": 1650,
      "estimatedDuration": {
        "minimumHours": 5.0,
        "maximumHours": 6.5
      },
      "technicalDifficulty": "S4",
      "physicalDifficulty": 5,
      "exposureRating": "E3",
      "carryingPercentage": 15,
      "routeType": "POINT_TO_POINT",
      "publishedAt": "2026-01-15T10:30:00Z"
    }
  ],
  "nextCursor": "opaque-value-or-null",
  "hasMore": true
}
```

Results use fixed ordering by `publishedAt DESC, id DESC`. No exact total-count query is executed.

The cursor contains a version, publication boundary, tour ID, first-page snapshot timestamp and a
SHA-256 fingerprint of the normalized bounding box and filters. It is Base64URL encoded so clients
must treat it as opaque. Encoding is not a cryptographic integrity guarantee; cursor contents do not
grant access and all values are validated and bound as SQL parameters. A cursor cannot be reused
with a different bounding box or filters.

New tours published after the first page's snapshot are excluded from later pages in that pagination
session. Clients must discard the cursor whenever the viewport or filters change.

#### Errors

Invalid query parameters and cursors return HTTP 400:

```json
{
  "message": "Invalid bounding box; expected west,south,east,north."
}
```

The existing `GET /api/tours/{id}` endpoint remains the public detail endpoint and returns 404 for
missing or unpublished tours.

### Discover tours for a map

```text
GET /api/tours/map?bbox=west,south,east,north&zoom=9
```

The endpoint returns a GeoJSON `FeatureCollection` containing bounded cluster or tour-point
features. `bbox` and integer `zoom` are required. Zoom must be from `0` through `22`, inclusive.
The geographic inclusion rule, inclusive bbox boundaries, antimeridian limitation and normalized
filter values are identical to `GET /api/tours`.

The following optional filters are supported:

- `countryCode`
- `technicalDifficulty` (comma-separated, for example `S2,S3`)
- `distanceMinKm` and `distanceMaxKm`
- `ascentMinMeters` and `ascentMaxMeters`

Pagination parameters are not accepted because the complete map representation is bounded by
adaptive aggregation instead.

Example cluster response:

```json
{
  "type": "FeatureCollection",
  "metadata": {
    "zoom": 9,
    "mode": "CLUSTERS",
    "cellSizeMeters": 22013.864,
    "aggregationAttempt": 0,
    "featureCount": 1,
    "featureCap": 1000
  },
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [14.05, 46.30]
      },
      "properties": {
        "kind": "cluster",
        "count": 12,
        "clusterKey": "z9:a0:141:474",
        "bounds": [13.98, 46.21, 14.12, 46.39]
      }
    }
  ]
}
```

The example values are illustrative. Cluster coordinates are the average of actual member points,
so they remain inside the requested rectangular viewport. Cluster `bounds` describe the exact
longitude/latitude extent of member discovery points, not the full grid cell. A single-member
cluster therefore has equal west/east and south/north values.

Tour features contain only lightweight marker data:

```json
{
  "type": "Feature",
  "geometry": {
    "type": "Point",
    "coordinates": [14.05, 46.30]
  },
  "properties": {
    "kind": "tour",
    "tourId": 123,
    "title": "Published tour title",
    "technicalDifficulty": "S3"
  }
}
```

#### Zoom and density policy

- Zoom `0-9`: cluster features only, including cells containing one tour.
- Zoom `10-11`: multi-tour cells are clusters; single-tour cells are tour features.
- Zoom `12-22`: individual tours are returned when their number fits the feature cap. If not, the
  endpoint falls back to cluster-only aggregation.

The initial globally anchored Web Mercator grid targets approximately 72 screen pixels per cell.
The production feature cap is 1,000. If a result exceeds the cap, the server doubles the cell size
and reaggregates in PostgreSQL, for at most 12 deterministic attempts. Features are never silently
truncated. A still-unrepresentable request returns HTTP 422 and asks the client to use a smaller
viewport or more restrictive filters.

`metadata.mode` is `CLUSTERS`, `MIXED` or `TOURS`. `cellSizeMeters` and `aggregationAttempt` expose
the actual aggregation used. Cluster keys are stable for the same zoom, aggregation attempt,
catalog state and filters; clients must treat them as opaque presentation identifiers.

Invalid bbox, zoom or filters return HTTP 400 using the same minimal `{ "message": "..." }` body
as list discovery. The response never contains route geometry, elevation profiles, GPX references
or storage paths.

### Discover route geometry for a map

```text
GET /api/tours/geometry?bbox=west,south,east,north&zoom=13
```

The endpoint returns published MTB route segments as a GeoJSON `FeatureCollection` for MapLibre
visualization. It is available only at integer zoom levels `12-22`. `bbox` and `zoom` are required.
The optional country, technical-difficulty, distance and ascent filters are identical to the list
and map endpoints. Cursor pagination is not supported.

Tour membership is determined only from the generated discovery point and the original inclusive
requested bbox. A separately padded clipping box never adds tours. The clipping box expands the
bbox width and height by 20% on every side, then clamps longitude to `[-180, 180]` and latitude to
the Web Mercator range `[-85.05112878, 85.05112878]`. A selected tour with no non-empty,
non-degenerate linear segment inside that clipping box is omitted from the features.

Example response:

```json
{
  "type": "FeatureCollection",
  "metadata": {
    "zoom": 13,
    "simplificationToleranceMeters": 28.664,
    "tourCount": 2,
    "vertexCount": 1475
  },
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [14.05, 46.30],
          [14.06, 46.31]
        ]
      },
      "properties": {
        "tourId": 123
      }
    }
  ]
}
```

Both `LineString` and `MultiLineString` geometries are returned because viewport clipping can split
a route into disconnected segments. Coordinates use `[longitude, latitude]` order.

PostgreSQL clips the stored EPSG:4326 line with `ST_ClipByBox2D`, extracts linear components,
transforms them to EPSG:3857, and applies `ST_SimplifyPreserveTopology`. The tolerance targets 1.5
screen pixels:

```text
toleranceMeters = 1.5 * 40075016.68557849 / (256 * 2^zoom)
```

EPSG:3857 scale distortion increases at high latitudes. The result is intentionally suitable only
for visual rendering; it must not be used for navigation, measurement or GPX export. Original
stored geometry and GPX data are unchanged.

The production limits are 50 selected tours and 50,000 returned vertices. The server checks at
most 51 matching tours before doing spatial geometry processing. PostGIS calculates the simplified
vertex budget before serializing GeoJSON. Tours and coordinates are never truncated or dropped to
fit a successful response. Exceeding either limit returns:

```json
{
  "code": "GEOMETRY_TOO_DENSE",
  "message": "Too many route geometries for this viewport. Display clusters or zoom in."
}
```

The status is HTTP 422, allowing clients to keep displaying cluster or point features. Missing or
invalid bbox, zoom or filters return HTTP 400. Antimeridian-crossing boxes remain unsupported.
