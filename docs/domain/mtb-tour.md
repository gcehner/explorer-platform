# MTB Tour

Conceptual data model describing a mountain bike tour. This document defines the domain language and is intentionally independent of implementation details or database design.

---

# Identity

## Title

Human-readable name of the tour.

Examples:

- Stol via Zabreška Planina
- Mangart via Italy

---

## Mountain Range

The primary mountain range or geographical area where the tour takes place.

Examples:

- Julian Alps
- Karawanks
- Kamnik–Savinja Alps
- Dolomites
- Stelvio National Park

> This is **not** an administrative region.

---

## Country

Country where the tour is located.

Examples:

- Slovenia
- Italy
- Austria

---

## Start Location

Starting point of the tour.

Contains:

- **Name** — human-readable name of the starting location
- **Latitude** — geographic latitude
- **Longitude** — geographic longitude

Example:

- Name: `Passo Predil`
- Latitude: `46.4776`
- Longitude: `13.5321`

---

# Description

## Short Description

Short summary displayed in lists and overview cards.

Example:

> Demanding alpine MTB tour with a long technical descent.

---

## Technical Description

Detailed technical description of the route.

Should focus on:

- route overview
- navigation hints
- technical sections
- hazards
- recommendations

This is **not** intended to be a travel story.

---

# Statistics

## Distance

Total route length.

Derived from the GPX track.

Unit:

- km

---

## Total Ascent

Total accumulated elevation gain.

Derived from the GPX track.

Unit:

- m

---

## Total Descent

Total accumulated elevation loss.

Derived from the GPX track.

Unit:

- m

---

## Lowest Point

Lowest elevation along the route.

Derived from the GPX track.

Unit:

- m

---

## Highest Point

Highest elevation along the route.

Derived from the GPX track.

Unit:

- m

---

## Estimated Duration

Estimated time required to complete the tour.

Stored as a range with a minimum and maximum duration.

Values are expressed in hours with one decimal place.

Values use increments of `0.5 h`.

Examples:

- `3.5–4.5 h`
- `5.0–6.0 h`
- `7.5–9.0 h`

---

## Tour Date

Date when the tour was performed.

The date is derived from timestamps contained in the GPX track when available.

If the GPX track does not contain timestamp information, the date may be provided manually as a fallback.

---

# Difficulty

## Technical Difficulty

Technical difficulty of the descent using the S0–S5 scale.

---

## Physical Difficulty

Subjective physical difficulty of the tour.

Scale:

- `1` — least physically demanding
- `2`
- `3`
- `4`
- `5` — most physically demanding

The rating is relative across all published Explorer tours rather than based on an external standard.

The rating takes the overall physical demands of the tour into account, not only distance or total ascent.

---

## Exposure

Exposure rating using the E1–E4 scale.

Describes the consequences of a riding mistake.

---

## Scenic Rating

Subjective rating of the scenic quality of the tour.

Scale:

- `1` — least scenic
- `2`
- `3`
- `4`
- `5` — most scenic

The rating is relative across all published Explorer tours rather than based on an external standard.

---

## Carrying Percentage

Estimated percentage of the total route where the bike must be carried or pushed.

The value includes both:

- carrying the bike,
- pushing the bike.

Expressed as a percentage of the total route.

Example:

`15%`

---

# Route Characteristics

## Route Type

Defines the shape of the tour.

A tour has exactly one route type.

Possible values:

- `Loop` — the tour starts and ends at approximately the same location
- `Point-to-Point` — the tour starts and ends at different locations

---

## Recommended Bike Types

Bike types suitable for the tour.

A tour can be suitable for one or more bike types.

Possible values:

- `Gravel`
- `XC`
- `All Mountain`

Example:

`XC, All Mountain`

---

## Surface Composition

Percentage distribution of riding surfaces along the route.

Possible surface types include:

- Asphalt
- Gravel
- Forest Road
- Singletrack
- Doubletrack

The values should represent the approximate percentage of the total route length.

Example:

- Asphalt: 15%
- Gravel: 30%
- Forest Road: 25%
- Singletrack: 30%

The total should equal 100%.

---

# GPX Data

## GPX Track

The original GPX file imported via the CLI.

Acts as the primary source of route geometry and GPX-derived statistics.

The following data is derived from the GPX track:

- distance
- total ascent
- total descent
- lowest point
- highest point
- tour date, when timestamp information is available
- elevation profile
- route geometry

GPX-derived values are normally not edited manually.

Calculation and processing of GPX data, including elevation calculations, is the responsibility of the CLI.

---

## Elevation Profile

Elevation profile generated from the GPX track.

Displayed on the tour detail page.

---

# Media

## Gallery

Collection of photos related to the tour.

A tour can contain multiple photos.

One photo from the gallery is designated as the cover image.

Photos can be added, removed and reordered before or after publication.

---

## Cover Image

Primary image representing the tour.

The cover image is one of the photos from the gallery rather than a separate image.

Displayed in lists, search results and the tour header.

The cover image can be changed by selecting another photo from the gallery.

---

## Video

Optional YouTube video associated with the tour.

Explorer does not store the video file itself.

The application stores a YouTube reference that is used to embed the video on the tour detail page.

Example:

`https://www.youtube.com/watch?v=...`

---

# Publication

## Status

Publication status of the tour.

Possible values:

- `Draft` — the tour has been imported but is only visible to the owner
- `Published` — the tour is publicly visible

---

## Published At

Date and time when the tour was first published.

The value is set automatically by Explorer when the tour is published.

For draft tours, the value is empty.

The original publication date remains unchanged when an already published tour is edited.