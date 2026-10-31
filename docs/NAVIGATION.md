# Navigation and Information Architecture

This document is the source of truth for Explorer Platform's public information architecture,
global navigation, and navigation behavior. The mockups define the visual direction, while this
document defines which destinations and navigation patterns are approved.

## Approved Decisions

### Global navigation

The public site has exactly four primary navigation destinations, in this order:

| Order | Label | Route |
| --- | --- | --- |
| 1 | Domov | `/` |
| 2 | MTB | `/mtb` |
| 3 | Turno smučanje | `/ski-touring` |
| 4 | O meni | `/about` |

The Explorer logo always links to `/`.

No additional primary navigation items, dropdowns, or utility destinations should be introduced
without an explicit architecture decision.

### Public page structure

| Route | Purpose | Visual reference |
| --- | --- | --- |
| `/` | Homepage | `docs/mockups/home.html` |
| `/mtb` | MTB tour listing | `docs/mockups/mtb.html` |
| `/mtb/tours/[id]` | Public MTB tour detail | `docs/mockups/tour-detail.html` |
| `/ski-touring` | Ski-touring listing; coming-soon page until implemented | Existing shared visual system |
| `/ski-touring/tours/[id]` | Future public ski-touring detail | Same detail-navigation principles as MTB |
| `/about` | Personal introduction and contact page | Existing shared visual system |

### Shared header

All public pages use one consistent global header:

- dark charcoal styling based on the existing mockups;
- sticky positioning;
- approximately 72 px desktop height;
- Explorer twin-mountain branding linked to `/`;
- the four approved primary navigation destinations;
- a clear active-section indication;
- parent-section highlighting for nested detail routes, so `/mtb/tours/[id]` keeps MTB active and
  `/ski-touring/tours/[id]` keeps Turno smučanje active;
- responsive mobile navigation with a semantic menu button, correct expanded state, keyboard
  access, and visible focus behavior.

### Detail navigation

Tour detail pages include breadcrumbs and a clear route back to their relevant listing.

Example:

```text
Domov / MTB / Tour name
```

The same principle applies to future ski-touring details:

```text
Domov / Turno smučanje / Tour name
```

Breadcrumbs must use semantic links for navigable ancestors. The current tour title is the final,
non-linked item.

### No global footer

Explorer Platform has no global footer. This is an explicit and final design decision.

- No shared footer appears on any public page.
- No bottom navigation or decorative global bottom section replaces it.
- Individual pages may end with content specific to that page, but not with a global footer.

### Management separation

`/management/tours/[id]` is a development-only preview route.

- It is not part of the public navigation.
- It is not wrapped by the shared public layout.
- It must remain unavailable in production until appropriate access control exists.
- Public pages must never fetch from the unauthenticated management API.
- Public pages and navigation must not link to a specific management tour or assume that any
  particular tour ID exists.
- Public tour details use the public `/api/tours/{id}` API and therefore expose only published
  tours.

### Implementation principles

- Use shared, reusable navigation and branding components.
- Use a Next.js App Router route group and shared layout for public pages.
- Use semantic links and accessible mobile interactions.
- Keep the primary navigation limited to the four approved destinations.
- Follow the branding, typography, colors, spacing, and widths established by the existing
  mockups.
- Keep navigation behavior consistent across desktop and mobile layouts.
- Preserve the API-first boundary: the frontend reads data through backend APIs and does not own
  domain or publication rules.

## Implementation Status

Implementation status does not change the approved architecture above. It records which parts are
currently available and which remain planned.

### Implemented

- Public App Router route group and shared public layout.
- Shared Explorer twin-mountain branding and sticky public header.
- Exactly four primary navigation links in the approved order.
- Active-section highlighting, including nested-route matching.
- Accessible responsive mobile menu with expanded state and Escape-key handling.
- No global footer.
- `/ski-touring` coming-soon page.
- `/about` page with clearly marked biography and contact placeholders.
- Development-only `/management/tours/[id]` outside the public route group and disabled in
  production.

### Partially implemented

- `/` exists, but the complete homepage based on `docs/mockups/home.html` is not yet implemented.
- `/mtb` exists as a truthful holding page, but the tour listing, map, filters, and results are not
  yet implemented.
- `/about` has its approved structure, but real biography and contact content is still required.

### Planned

- Public MTB detail route at `/mtb/tours/[id]`.
- Breadcrumbs and listing-return navigation on public tour detail pages.
- Full MTB listing backed by a public collection API; no management data or fabricated tours may
  be used as a substitute.
- Full ski-touring listing.
- Future ski-touring detail route at `/ski-touring/tours/[id]`.

