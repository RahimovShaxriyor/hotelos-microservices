---
name: design-tokens
description: Create, structure, and implement design token systems for any project. Use this skill when building a design token architecture, defining CSS custom properties, creating a theme system, setting up dark/light mode via tokens, exporting tokens for Figma, converting a design to tokens, generating a token file from a brand, or implementing a multi-brand / multi-theme system. Triggers on: "design tokens", "CSS variables", "theme system", "token architecture", "token file", "how do I structure my colors as variables", "multi-theme", "Figma tokens", "W3C design tokens", "token naming", "semantic tokens", "primitive tokens". Always use this skill when the user is trying to create a systematic approach to design values rather than hardcoded CSS.
---

# Design Tokens Skill

Design tokens are the single source of truth for a product's visual design values. This skill covers token architecture, naming, implementation, and tooling.

## Token Architecture

Tokens exist in two layers. Never skip straight to semantic — always build on primitives.

### Layer 1: Primitive Tokens (Raw Values)

Primitives are the palette. They have no context — they simply define what colors, sizes, and values exist.

```
color.gray.50   = #F9FAFB
color.gray.100  = #F3F4F6
color.gray.200  = #E5E7EB
color.gray.300  = #D1D5DB
color.gray.400  = #9CA3AF
color.gray.500  = #6B7280
color.gray.600  = #4B5563
color.gray.700  = #374151
color.gray.800  = #1F2937
color.gray.900  = #111827

color.indigo.400 = #818CF8
color.indigo.500 = #6366F1
color.indigo.600 = #4F46E5
color.indigo.700 = #4338CA
```

### Layer 2: Semantic Tokens (Contextual Meaning)

Semantics assign meaning to primitives. They describe *what the token is for*, not what color it is.

```
color.background.base        = color.gray.50     (light) / color.gray.950  (dark)
color.background.surface     = color.white       (light) / color.gray.900  (dark)
color.background.overlay     = color.white       (light) / color.gray.800  (dark)

color.text.primary           = color.gray.900    (light) / color.gray.50   (dark)
color.text.secondary         = color.gray.600    (light) / color.gray.400  (dark)
color.text.muted             = color.gray.400    (light) / color.gray.600  (dark)
color.text.disabled          = color.gray.300    (light) / color.gray.700  (dark)
color.text.inverse           = color.white       (light) / color.gray.900  (dark)

color.border.subtle          = color.gray.100    (light) / color.gray.800  (dark)
color.border.default         = color.gray.200    (light) / color.gray.700  (dark)
color.border.strong          = color.gray.300    (light) / color.gray.600  (dark)

color.accent.default         = color.indigo.600  (light) / color.indigo.400 (dark)
color.accent.hover           = color.indigo.700  (light) / color.indigo.300 (dark)
color.accent.subtle          = color.indigo.50   (light) / color.indigo.950 (dark)
color.accent.text            = color.indigo.700  (light) / color.indigo.200 (dark)

color.status.success         = color.green.600   (light) / color.green.400  (dark)
color.status.warning         = color.amber.600   (light) / color.amber.400  (dark)
color.status.error           = color.red.600     (light) / color.red.400    (dark)
color.status.info            = color.blue.600    (light) / color.blue.400   (dark)
```

---

## Token Naming Convention

Use a consistent 3–4 part hierarchy: `category.variant.property.state`

```
[category].[variant].[property].[state]

color.text.primary
color.text.primary.hover
color.background.surface
color.border.default.focus
spacing.component.padding.sm
typography.body.fontSize
typography.heading.fontWeight
radius.card
shadow.card.default
shadow.card.hover
```

### Naming Rules

1. **No color names in semantic tokens**: `color.text.primary` not `color.text.gray-900`
2. **No hex values in names**: Never `token-E5E7EB`
3. **Descriptive, not prescriptive**: `color.accent.subtle` not `color.accent.10percent`
4. **Consistent plurality**: Use singular for everything (`color`, not `colors`)
5. **States at the end**: `color.button.background.hover` not `color.button.hover.background`

---

## CSS Custom Properties Implementation

### File Structure

```
tokens/
├── primitives.css        — Raw palette (colors, base sizes)
├── semantic-light.css    — Light mode semantic mappings
├── semantic-dark.css     — Dark mode semantic mappings
└── components.css        — Component-level tokens
```

### primitives.css

```css
:root {
  /* Gray scale */
  --color-gray-50:  #F9FAFB;
  --color-gray-100: #F3F4F6;
  --color-gray-200: #E5E7EB;
  --color-gray-300: #D1D5DB;
  --color-gray-400: #9CA3AF;
  --color-gray-500: #6B7280;
  --color-gray-600: #4B5563;
  --color-gray-700: #374151;
  --color-gray-800: #1F2937;
  --color-gray-900: #111827;
  --color-gray-950: #0A0A0F;

  /* Brand / Accent scale */
  --color-accent-50:  #EEF2FF;
  --color-accent-100: #E0E7FF;
  --color-accent-200: #C7D2FE;
  --color-accent-300: #A5B4FC;
  --color-accent-400: #818CF8;
  --color-accent-500: #6366F1;
  --color-accent-600: #4F46E5;
  --color-accent-700: #4338CA;
  --color-accent-800: #3730A3;
  --color-accent-900: #312E81;

  /* Spacing primitives */
  --space-1:  4px;
  --space-2:  8px;
  --space-3:  12px;
  --space-4:  16px;
  --space-6:  24px;
  --space-8:  32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;
  --space-20: 80px;
  --space-24: 96px;

  /* Border radius primitives */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;
  --radius-xl: 12px;
  --radius-2xl: 16px;
  --radius-full: 9999px;
}
```

### semantic-light.css

```css
:root, [data-theme="light"] {
  /* Backgrounds */
  --bg-base:    var(--color-gray-50);
  --bg-surface: #FFFFFF;
  --bg-overlay: #FFFFFF;
  --bg-subtle:  var(--color-gray-100);
  --bg-muted:   var(--color-gray-200);

  /* Text */
  --text-primary:   var(--color-gray-900);
  --text-secondary: var(--color-gray-600);
  --text-muted:     var(--color-gray-400);
  --text-disabled:  var(--color-gray-300);
  --text-inverse:   #FFFFFF;
  --text-accent:    var(--color-accent-700);

  /* Borders */
  --border-subtle:  var(--color-gray-100);
  --border-default: var(--color-gray-200);
  --border-strong:  var(--color-gray-300);
  --border-accent:  var(--color-accent-500);
  --border-focus:   var(--color-accent-600);

  /* Accent */
  --accent:          var(--color-accent-600);
  --accent-hover:    var(--color-accent-700);
  --accent-active:   var(--color-accent-800);
  --accent-subtle:   var(--color-accent-50);
  --accent-muted:    var(--color-accent-100);
  --accent-text:     var(--color-accent-700);

  /* Status */
  --status-success:      #16A34A;
  --status-success-bg:   #F0FDF4;
  --status-warning:      #CA8A04;
  --status-warning-bg:   #FEFCE8;
  --status-error:        #DC2626;
  --status-error-bg:     #FEF2F2;
  --status-info:         #2563EB;
  --status-info-bg:      #EFF6FF;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);
  --shadow-focus: 0 0 0 3px rgba(79,70,229,0.2);
}
```

### semantic-dark.css

```css
[data-theme="dark"] {
  --bg-base:    #0F0F11;
  --bg-surface: #1A1A1F;
  --bg-overlay: #222228;
  --bg-subtle:  #222228;
  --bg-muted:   #2D2D35;

  --text-primary:   #F1F0EF;
  --text-secondary: #9CA3AF;
  --text-muted:     #6B7280;
  --text-disabled:  #4B5563;
  --text-inverse:   #0F0F11;
  --text-accent:    var(--color-accent-300);

  --border-subtle:  #1E1E24;
  --border-default: #2D2D35;
  --border-strong:  #3D3D48;
  --border-accent:  var(--color-accent-500);
  --border-focus:   var(--color-accent-400);

  --accent:          var(--color-accent-400);
  --accent-hover:    var(--color-accent-300);
  --accent-active:   var(--color-accent-200);
  --accent-subtle:   rgba(99,102,241,0.1);
  --accent-muted:    rgba(99,102,241,0.2);
  --accent-text:     var(--color-accent-300);

  --status-success:      #4ADE80;
  --status-success-bg:   rgba(74,222,128,0.1);
  --status-warning:      #FACC15;
  --status-warning-bg:   rgba(250,204,21,0.1);
  --status-error:        #F87171;
  --status-error-bg:     rgba(248,113,113,0.1);
  --status-info:         #60A5FA;
  --status-info-bg:      rgba(96,165,250,0.1);

  --shadow-sm: 0 1px 2px rgba(0,0,0,0.4);
  --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.5), 0 2px 4px -1px rgba(0,0,0,0.3);
  --shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.6), 0 4px 6px -2px rgba(0,0,0,0.4);
  --shadow-focus: 0 0 0 3px rgba(129,140,248,0.3);
}
```

---

## W3C Design Token Format (for Figma + Tool Export)

The W3C Design Token Community Group format is the standard for tool interoperability.

```json
{
  "$schema": "https://design-tokens.org/schema.json",
  "color": {
    "gray": {
      "50": { "$value": "#F9FAFB", "$type": "color" },
      "900": { "$value": "#111827", "$type": "color" }
    },
    "accent": {
      "600": { "$value": "#4F46E5", "$type": "color" }
    }
  },
  "semantic": {
    "background": {
      "base": {
        "$value": "{color.gray.50}",
        "$type": "color",
        "$description": "Page background in light mode"
      }
    },
    "text": {
      "primary": {
        "$value": "{color.gray.900}",
        "$type": "color"
      }
    }
  },
  "spacing": {
    "4": { "$value": "16px", "$type": "dimension" },
    "6": { "$value": "24px", "$type": "dimension" }
  },
  "typography": {
    "body": {
      "fontSize":   { "$value": "16px", "$type": "dimension" },
      "lineHeight": { "$value": "1.5",  "$type": "number" },
      "fontFamily": { "$value": "Inter, system-ui, sans-serif", "$type": "fontFamily" }
    }
  },
  "borderRadius": {
    "card": { "$value": "8px",  "$type": "dimension" },
    "button": { "$value": "6px", "$type": "dimension" }
  }
}
```

---

## Component Tokens

After global semantic tokens, define component-scoped tokens for complex components:

```css
/* Button component tokens */
:root {
  --btn-height-sm: 28px;
  --btn-height-md: 36px;
  --btn-height-lg: 44px;
  --btn-padding-x-sm: 12px;
  --btn-padding-x-md: 16px;
  --btn-padding-x-lg: 20px;
  --btn-font-size-sm: 13px;
  --btn-font-size-md: 14px;
  --btn-font-size-lg: 15px;
  --btn-radius: var(--radius-md);
  --btn-primary-bg: var(--accent);
  --btn-primary-bg-hover: var(--accent-hover);
  --btn-primary-text: var(--text-inverse);
}

/* Card component tokens */
:root {
  --card-bg: var(--bg-surface);
  --card-border: var(--border-default);
  --card-radius: var(--radius-lg);
  --card-padding: var(--space-6);
  --card-shadow: var(--shadow-sm);
}
```

---

## Token Tooling

### Recommended Tools

| Tool | Use Case |
|---|---|
| Style Dictionary (Amazon) | Transform tokens to any output format (CSS, JS, iOS, Android) |
| Theo (Salesforce) | Alternative transformer |
| Token Transformer | Figma Tokens → W3C format |
| Cobalt UI | Multi-theme generation |
| Panda CSS | Token-first CSS-in-JS |
| Tailwind CSS | Utility-first token system |

### Style Dictionary Config

```json
{
  "source": ["tokens/**/*.json"],
  "platforms": {
    "css": {
      "transformGroup": "css",
      "prefix": "token",
      "buildPath": "dist/css/",
      "files": [{
        "destination": "variables.css",
        "format": "css/variables"
      }]
    },
    "js": {
      "transformGroup": "js",
      "buildPath": "dist/js/",
      "files": [{
        "destination": "tokens.js",
        "format": "javascript/es6"
      }]
    }
  }
}
```

---

## Multi-Brand Token Architecture

For products serving multiple brands from one codebase:

```
tokens/
├── primitives/
│   ├── global.json      — Non-brand values (spacing, radius, etc.)
│   ├── brand-a.json     — Brand A color primitives
│   └── brand-b.json     — Brand B color primitives
├── semantic/
│   ├── global.json      — Brand-agnostic semantic tokens
│   ├── brand-a.json     — Brand A semantic mappings
│   └── brand-b.json     — Brand B semantic mappings
└── components/
    └── global.json      — Component tokens (reference semantic tokens)
```

Switching brand at runtime:
```html
<html data-brand="brand-a" data-theme="light">
```

```css
[data-brand="brand-a"] { --accent: var(--brand-a-accent); }
[data-brand="brand-b"] { --accent: var(--brand-b-accent); }
```