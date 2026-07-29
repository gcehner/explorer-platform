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

The recommended starting point of the tour.

May include:

- parking area
- mountain hut
- village
- trailhead

Example:

- Planica
- Dom v Tamarju

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

Unit:

- km

---

## Total Ascent

Total accumulated elevation gain.

Unit:

- m

---

## Total Descent

Total accumulated elevation loss.

Unit:

- m

---

## Lowest Point

Lowest elevation along the route.

Unit:

- m

---

## Highest Point

Highest elevation along the route.

Unit:

- m

---

## Estimated Duration

Approximate completion time displayed as a range.

Examples:

- 2–3 h
- 4–5 h
- 6–8 h

---

# Difficulty

## Technical Difficulty

Technical difficulty of the descent using the S0–S5 scale.

---

## Physical Difficulty

Subjective estimate of the required physical fitness.

Scale to be defined.

Example:

- 1–5

---

## Exposure

Exposure rating using the E1–E4 scale.

Describes the consequences of a riding mistake.

---

## Scenic Rating

Subjective rating of the route's scenery.

Scale to be defined.

Example:

- 1–5

---

## Carrying Sections

Estimated percentage of the route where the bike must be carried or pushed.

Examples:

- 0%
- 10%
- 35%

---

# Route Characteristics

## Route Type

Defines whether the route is:

- Loop
- Point-to-Point

---

## Recommended Bike Types

Recommended bike categories.

Multiple values are allowed.

Examples:

- Gravel
- XC
- DownCountry
- Trail
- All Mountain
- Enduro

---

## Surface

Primary riding surfaces.

Multiple values are allowed.

Examples:

- Asphalt
- Gravel
- Forest Road
- Singletrack
- Doubletrack

> Consider whether this should be represented as a simple list or as percentages.

---

# GPX Data

## GPX Track

The original GPX file imported via the CLI.

Acts as the primary source of route geometry and statistics.

---

## Elevation Profile

Elevation profile generated from the GPX track.

Displayed on the tour detail page.

---

# Media

## Cover Image

Primary image representing the tour.

Displayed in lists, search results and the tour header.

---

## Gallery

Collection of photos related to the tour.

---

## Video

Optional video associated with the tour.

May reference YouTube or another supported provider.

---

# Publication

## Status

Publication status of the tour.

Possible values:

- Draft
- Published

Only published tours are visible to the public.

---

## Publication Date

Date when the tour was published.

This is different from the GPX import date or creation date.