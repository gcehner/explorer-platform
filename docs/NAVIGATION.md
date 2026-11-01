# Navigation and Information Architecture

This document is the source of truth for Explorer Platform's public information architecture,
global navigation, and navigation behavior. The mockups define the visual direction, while this
document defines which destinations and navigation patterns are approved.

## Approved Decisions

### Public brand and internal project name

- The approved public-facing brand name is **MTB 'n' SKI**.
- Public navigation, page metadata, and visible brand references use MTB 'n' SKI.
- The public wordmark uses a small, single-color mountain icon without a subtitle.
- **Explorer Platform** remains the internal project, repository, backend, and architecture name.
  Public branding does not rename packages, APIs, environment variables, or other internal
  identifiers.

### Global navigation

The public site has exactly four primary navigation destinations, in this order:

| Order | Label | Route |
| --- | --- | --- |
| 1 | Domov | `/` |
| 2 | MTB | `/mtb` |
| 3 | Turno smučanje | `/ski-touring` |
| 4 | O meni | `/about` |

The MTB 'n' SKI logo always links to `/`.

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

- minimalist light editorial styling with an off-white `#f8f9f6` background, dark text, and a
  subtle bottom border;
- sticky positioning;
- approximately 64–68 px desktop height;
- left-aligned, restrained MTB 'n' SKI branding with a small muted-green, single-color mountain
  icon linked to `/` and no subtitle; the brand and right-aligned navigation share the same content
  container;
- the four approved primary navigation destinations;
- medium-weight navigation text and a subtle dark-green `#315714` active-section indication;
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

### Homepage hero

The public homepage hero uses the approved image-and-map composition:

- one full-bleed local mountain or outdoor photograph on the left, rendered with `object-fit:
  cover`;
- no text, labels, buttons, metadata, gradient overlay, geometric background, or decorative
  mountain layers over the photograph;
- the real interactive MapLibre map remains unchanged on the right;
- the existing 47%/53% desktop split and responsive stacked layout remain in place;
- a single static image is used initially; slideshow behavior is deferred.

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
- Follow the approved MTB 'n' SKI branding and light-header decision while retaining the content
  typography, colors, spacing, and widths established by the existing mockups.
- Keep navigation behavior consistent across desktop and mobile layouts.
- Preserve the API-first boundary: the frontend reads data through backend APIs and does not own
  domain or publication rules.

## Implementation Status

Implementation status does not change the approved architecture above. It records which parts are
currently available and which remain planned.

### Implemented

- Public App Router route group and shared public layout.
- Shared MTB 'n' SKI mountain branding and minimalist light sticky public header.
- Exactly four primary navigation links in the approved order.
- Active-section highlighting, including nested-route matching.
- Accessible responsive mobile menu with expanded state and Escape-key handling.
- No global footer.
- `/ski-touring` coming-soon page.
- `/about` page with clearly marked biography and contact placeholders.
- `/` homepage based on `docs/mockups/home.html`, with a real MapLibre basemap and truthful
  unavailable-data states where no public collection API exists.
- Development-only `/management/tours/[id]` outside the public route group and disabled in
  production.

### Partially implemented

- The homepage image component and stable hero slot are implemented, but the repository does not
  yet contain an approved local mountain photograph.
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

