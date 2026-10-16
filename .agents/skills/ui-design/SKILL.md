---
name: ui-design
description: Create production-grade UI design systems, visual interfaces, and beautiful component designs. Use this skill whenever the user wants to design a UI, build a design system, create visual components, style an interface, define color palettes, typography scales, spacing systems, or produce anything that requires high-quality visual design decisions. Triggers on: "design this", "make it look good", "create a UI for", "style this component", "build a design system", "what should my color palette be", "how should I structure my typography", "design language", "visual identity", "design tokens", "dark mode", "light mode", "make this beautiful". Always use this skill when design quality is explicitly important or when the user is building something user-facing.
---

# UI Design Skill

This skill guides the creation of production-grade visual design systems, UI components, and aesthetic decisions. It covers the full spectrum from abstract design philosophy to concrete implementation tokens.

## Core Design Thinking Process

Before making any design decision, establish these four things in order:

**1. Context & Purpose**
What is this interface for? Who uses it? What emotional response should it produce?
- Developer tool → precision, trust, low visual noise
- Consumer product → delight, personality, warmth
- Enterprise software → authority, density, efficiency
- Marketing / landing → conversion, excitement, differentiation

**2. Design Archetype**
Pick one. Don't blend without intent.

| Archetype | Character | Best For |
|---|---|---|
| Minimal / Restrained | Generous whitespace, monochrome, single accent | Premium, developer, fintech |
| Editorial | Strong typography hierarchy, asymmetry, magazine layout | Content, media, blogs |
| Brutalist | Raw structure, stark contrast, no decoration | Art, portfolio, statements |
| Warm / Organic | Soft corners, earthy palette, texture | Wellness, food, lifestyle |
| Dark / Technical | Near-black surfaces, accent scarcity, monospace hints | Dev tools, data, analytics |
| Playful | Saturated palette, personality, illustration | Consumer, youth, creative |
| Luxury | Black + gold/white, extreme restraint, serif type | Fashion, finance, high-end |
| Corporate / SaaS | Clean, trustworthy, blue-leaning, grid-locked | B2B, productivity, enterprise |

**3. Differentiation Hook**
What is the one design decision that makes this unforgettable?
- An unexpected typeface pairing
- A specific color that nobody else in the category uses
- A spatial rhythm that feels wrong until it feels right
- A micro-interaction that delights

**4. Constraints**
Technical: framework, browser support, performance budget
Brand: existing logo, required colors, existing assets
Accessibility: WCAG 2.1 AA minimum (contrast ratios)

---

## Color System Design

### Palette Architecture

Always design in layers, not individual colors:

```
Layer 1 — Backgrounds (2–3 values)
  base:     The page background
  surface:  Card / panel background
  overlay:  Modal / dropdown background

Layer 2 — Borders (2–3 values)
  subtle:   Dividers, inactive
  default:  Most borders
  strong:   Focus rings, active

Layer 3 — Text (3–4 values)
  primary:  Headings, key content
  secondary: Body copy
  muted:    Captions, labels
  disabled: Inactive elements

Layer 4 — Brand / Accent (1–2 values)
  primary:  CTAs, active states, links
  secondary: Supporting accent (use sparingly)

Layer 5 — Semantic (4 values)
  success, warning, error, info
```

### Color Rules

- **The 60-30-10 rule**: 60% neutral, 30% secondary neutral, 10% accent
- **Accent scarcity**: The more you use an accent color, the less it means anything. Use it on ≤10% of elements.
- **Dark mode is not inversion**: Dark mode uses different lightness curves. `#0F0F11` (near-black) reads better than `#000000`. Text is never pure white — use `#F1F0EF`.
- **Semantic colors must clear WCAG AA**: Text on colored backgrounds needs ≥4.5:1 contrast ratio for body, ≥3:1 for large text.

### Palette Generation Approach

Start from one anchor color. Derive everything else.

```
1. Pick your brand/accent color (e.g. #5E6AD2)
2. Extract its hue (225°)
3. Build a neutral scale using that hue at 2–5% saturation
4. Build your semantic colors using the same hue relationships:
   - Success: hue + 120° (green-adjacent)
   - Warning: hue + 60° (amber-adjacent)
   - Error: hue - 30° (red-adjacent)
5. Define light mode and dark mode mappings separately
```

---

## Typography System Design

### Type Scale

Use a modular scale — not arbitrary sizes. Common ratios:
- **Minor Third (1.2)**: Tight, dense, editorial
- **Major Third (1.25)**: Clean SaaS default
- **Perfect Fourth (1.333)**: Strong hierarchy, marketing
- **Golden Ratio (1.618)**: Maximum drama, headlines

Example scale at 1.25 ratio, base 16px:
```
xs:   10px  — Labels, captions
sm:   13px  — Secondary body
base: 16px  — Primary body
md:   20px  — Subheadings
lg:   25px  — Section headings
xl:   31px  — Page headings
2xl:  39px  — Hero headings
3xl:  49px  — Display
```

### Font Pairing Principles

- **One expressive font, one utility font**: Never two expressive fonts.
- **Contrast in weight and width, harmony in era**: Pair fonts from the same design era but different roles.
- **Size relationship**: Display font usually 3–4 steps above body in the scale.

### Strong Pairing Patterns

```
Editorial authority:
  Display: Playfair Display (serif, high contrast)
  Body: DM Sans (geometric sans)

Modern SaaS:
  Display: Sora (geometric, distinctive)
  Body: IBM Plex Sans (technical, readable)

Premium / Luxury:
  Display: Cormorant Garamond (old-style serif)
  Body: Jost (clean geometric sans)

Developer / Technical:
  Display: Space Grotesk (technical personality)
  Body: Inter (maximum readability) — but pick a more distinctive body if possible

Warm / Human:
  Display: Nunito (rounded, friendly)
  Body: Source Serif 4 (readable, human)
```

### Weight Usage

- **Never use more than 3 weights** in one interface
- Establish a clear rule: e.g. regular (400) = body, medium (500) = labels, bold (700) = headings only
- Heavy weights (800–900) should appear on ≤2 element types max

---

## Spacing System Design

### Grid Unit

Pick one base unit and derive everything from it. Standard: **8px**.

```
4px   — Tight internal spacing (icon padding, badge padding)
8px   — Default gap between related elements
16px  — Standard component padding
24px  — Gap between sibling components
32px  — Section padding (compact)
48px  — Section padding (standard)
64px  — Section padding (spacious / premium)
96px  — Section padding (editorial / hero)
```

### Spacing Philosophy

**Dense** (developer tools, data-heavy): Base 4px, reduce to 4/8/12/16
**Standard** (SaaS, productivity): Base 8px, standard scale
**Spacious** (marketing, premium): Base 8px, use higher steps liberally
**Breathing room** (luxury, editorial): Invert expectations — more space than content

---

## Component Design Patterns

### Buttons

```
Primary: Filled, brand color background, white text
Secondary: Outlined, brand color border, brand color text
Ghost: No border, brand color text
Destructive: Red fill or red outline
Disabled: 40% opacity, cursor-not-allowed

Size scale:
  sm: 28px height, 12px padding, 13px text
  md: 36px height, 16px padding, 14px text
  lg: 44px height, 20px padding, 16px text
```

### Cards

**Flat delineation** (trust, restraint): `border: 1px solid var(--border)` — no shadow
**Elevated** (material, depth): `box-shadow: 0 1px 3px rgba(0,0,0,0.1)` — subtle
**Floating** (dramatic depth): Multi-layer shadow

Rule: Pick one card style and use it everywhere. Mixing shadow approaches destroys hierarchy.

### Forms

- Label always above input (not placeholder-as-label)
- Error state: red border + red text below input
- Focus state: brand color ring, `outline: 2px solid var(--accent); outline-offset: 2px`
- Input height: 36px (compact), 40px (standard), 44px (comfortable)

### Navigation

| Pattern | Use When |
|---|---|
| Top nav | Marketing sites, simple apps |
| Fixed sidebar | Dense apps, many navigation items |
| Collapsing sidebar | Apps with variable screen needs |
| Bottom nav | Mobile-first |
| Breadcrumbs | Deep hierarchy |

---

## Dark Mode Implementation

### The Rules

1. **Never use CSS `filter: invert()`** — it breaks images and produces wrong colors
2. **Separate color tokens for each mode** — don't calculate dark from light
3. **Reduce saturation in dark mode** — saturated colors vibrate on dark backgrounds
4. **Near-black backgrounds outperform true black** — `#0F0F11` not `#000000`
5. **Never use pure white text** — `#F1F0EF` reads better than `#FFFFFF`

### Implementation Pattern

```css
:root {
  --bg-base: #FFFFFF;
  --bg-surface: #F9FAFB;
  --text-primary: #111827;
  --text-secondary: #6B7280;
  --border: #E5E7EB;
  --accent: #4F46E5;
}

[data-theme="dark"] {
  --bg-base: #0F0F11;
  --bg-surface: #1A1A1F;
  --text-primary: #F1F0EF;
  --text-secondary: #9CA3AF;
  --border: #2D2D35;
  --accent: #818CF8;  /* Lighter shade — same hue, lighter in dark */
}
```

---

## Design Review Checklist

Before shipping any UI, verify:

**Visual consistency**
- [ ] Single typeface system — no rogue fonts
- [ ] All spacing follows the base unit grid
- [ ] One card style throughout
- [ ] Consistent border-radius across components

**Color**
- [ ] Accent used on ≤10% of elements
- [ ] All text passes WCAG AA contrast
- [ ] Dark mode tested (if applicable)
- [ ] No semantic color used for decoration

**Typography**
- [ ] Clear hierarchy — 3 distinguishable heading levels
- [ ] Body text 15–16px minimum, 1.5 line-height minimum
- [ ] No more than 3 weights in use
- [ ] Max line length 65–75 characters (prose)

**Interaction**
- [ ] All interactive elements have visible hover and focus states
- [ ] Focus rings present (keyboard navigation)
- [ ] Loading states designed for async operations
- [ ] Empty states designed

---

## Reference Files

Load these when needed:
- `references/color-palettes.md` — 20 ready-to-use curated palettes by archetype
- `references/type-pairings.md` — 15 verified font pairings with specimens
- `references/component-specs.md` — Exact measurements for common components
- `references/accessibility.md` — WCAG compliance patterns and contrast checking