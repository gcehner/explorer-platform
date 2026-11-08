# Navigation and Information Architecture

This document is the source of truth for Explorer Platform's public information architecture,
global navigation, and navigation behavior. The product is exclusively focused on MTB tours.

## Approved Decisions

### Public brand and internal project name

- The temporary public-facing brand is **MTB** until a final MTB-only name is selected.
- Public navigation, metadata, and visible brand references use MTB.
- The public wordmark uses the existing small, single-color mountain icon without a subtitle.
- **Explorer Platform** remains the internal repository, backend, and architecture name. Branding
  does not rename packages, APIs, environment variables, databases, or other internal identifiers.

### Global navigation

The public site has exactly three primary navigation destinations:

| Order | Label | Route |
| --- | --- | --- |
| 1 | Domov | `/` |
| 2 | MTB ture | `/mtb` |
| 3 | O meni | `/about` |

The MTB logo always links to `/`. Tour details are not top-level navigation destinations. Do not
add dropdowns or unrelated utility links without a new architecture decision.

### Public page structure

| Route | Purpose | Status |
| --- | --- | --- |
| `/` | MTB homepage based on `docs/mockups/home.html` | Implemented |
| `/mtb` | MTB discovery, list, filters, and map based on `docs/mockups/mtb.html` | Visual shell implemented; API integration planned |
| `/mtb/[id]` | Public published MTB tour detail based on `docs/mockups/tour-detail.html` | Planned |
| `/about` | Project/author introduction and contact | Implemented with content placeholders |

There is no second-activity route or placeholder in this product. No compatibility redirect is
required because no documented public URL commitment exists for removed placeholders.

### Shared header

All public pages use the existing shared header:

- minimalist light editorial styling with an off-white `#f8f9f6` background, dark text, and a
  subtle bottom border;
- sticky positioning and approximately 64–68 px desktop height;
- left-aligned MTB mountain branding linked to `/`;
- right-aligned desktop navigation using the three approved destinations;
- subtle dark-green `#315714` active-section indication;
- parent-section highlighting for nested `/mtb/[id]` routes;
- accessible responsive mobile navigation with correct expanded state, Escape-key handling,
  keyboard access, and visible focus behavior.

### Detail navigation

Public MTB detail pages include semantic breadcrumbs and a clear link back to `/mtb`:

```text
Domov / MTB ture / Tour name
```

The current tour title is the final, non-linked breadcrumb item.

### Homepage

The homepage preserves the approved image-and-map composition:

- full-bleed local MTB/outdoor image slot on the left;
- real interactive MapLibre map on the right;
- existing 47%/53% desktop split and responsive stacked layout;
- truthful empty states where public data is not yet connected;
- primary exploration links lead to `/mtb`;
- no fabricated tours, map markers, or statistics.

### No global footer

Explorer Platform has no global footer. No bottom navigation or decorative global bottom section
replaces it.

### Management separation

`/management/tours/[id]` remains a development-only preview route outside the public layout and
navigation. It must remain unavailable in production until access control exists. Public pages
must use public APIs and never link to or depend on a management tour ID.

### Implementation principles

- Use the shared public route group, navigation, and branding components.
- Preserve semantic links and accessible mobile interactions.
- Preserve the API-first boundary and existing MTB domain rules.
- Keep the domain focused on MTB rather than adding speculative activity abstractions.
- Keep visual styling aligned with the approved mockups.

## Implementation Status

### Implemented

- Shared light sticky header with MTB branding and three primary links.
- Active-section highlighting and accessible mobile menu.
- No global footer.
- Homepage visual shell with MapLibre and truthful unavailable-data states.
- `/mtb` discovery visual shell with filters, empty results, and MapLibre.
- `/about` structure with biography and contact placeholders.
- Development-only `/management/tours/[id]` preview.
- Backend public list, map-cluster, geometry-discovery, and tour-detail APIs.

### Planned

- Connect `/mtb` to the existing public discovery APIs.
- Implement `/mtb/[id]` using the public published-tour detail API.
- Add public detail breadcrumbs and listing-return navigation.
- Replace content placeholders with approved biography, contact, and local imagery.
