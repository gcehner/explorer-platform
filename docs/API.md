# Public API

## MTB tour discovery

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

Phase 1 does not support antimeridian-crossing boxes. Frontends must clamp map bounds to the valid
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

## Planned discovery endpoints

Server-side map clustering and bounded simplified geometry are planned as separate endpoints. They
are not implemented yet:

```text
GET /api/tours/map
GET /api/tours/geometry
```

They will reuse the same normalized filters and discovery-point geographic inclusion semantics.
