# 🎨 Master Design Systems & Theme Tokens

This document details the exact CSS custom properties, Tailwind CSS utility mappings, and ambient lighting rules for all 6 core design themes supported by **AgentUI-Skills**.

---

## 1. 🌌 Cyberpunk / Dark Ops HUD
> **Best For**: OSINT platforms, security operations centers, digital audio workstations, crypto/Web3 dashboards, and developer command centers.

```css
:root {
  --hud-bg-base: #07090e;
  --hud-bg-surface: rgba(13, 19, 34, 0.85);
  --hud-bg-elevated: rgba(19, 29, 53, 0.90);
  --hud-border-subtle: rgba(255, 255, 255, 0.08);
  --hud-border-glow: rgba(0, 240, 255, 0.35);
  --hud-border-accent: rgba(255, 0, 127, 0.40);
  --hud-cyan: #00f0ff;
  --hud-pink: #ff007f;
  --hud-green: #00ff88;
  --hud-amber: #ffb703;
  --hud-text-primary: #f8fafc;
  --hud-text-secondary: #94a3b8;
  --hud-text-muted: #64748b;
  --hud-glow-cyan: 0 0 20px rgba(0, 240, 255, 0.35);
  --hud-glow-pink: 0 0 20px rgba(255, 0, 127, 0.35);
  --hud-card-shadow: 0 8px 32px rgba(0, 0, 0, 0.65);
}
```

---

## 2. ⚡ Linear / Vercel Minimalist Dark
> **Best For**: High-end SaaS products, developer tools, cloud infrastructure portals, issue trackers, and API reference docks.

```css
:root {
  --linear-bg-base: #000000;
  --linear-bg-surface: #0a0a0a;
  --linear-bg-subtle: #121212;
  --linear-bg-hover: #18181b;
  --linear-border-base: rgba(255, 255, 255, 0.08);
  --linear-border-hover: rgba(255, 255, 255, 0.20);
  --linear-border-active: rgba(255, 255, 255, 0.35);
  --linear-text-title: #ffffff;
  --linear-text-body: #d4d4d8;
  --linear-text-muted: #71717a;
  --linear-shadow-card: 0 1px 1px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.08);
  --linear-shadow-hover: 0 4px 20px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.2);
}
```

---

## 3. 💎 Glassmorphism 2.0 / Vision Pro
> **Best For**: Next-generation consumer web apps, creative portfolios, media players, and desktop environments.

```css
:root {
  --glass-bg-canvas: radial-gradient(circle at 50% 0%, #1e1b4b 0%, #030712 100%);
  --glass-surface: rgba(255, 255, 255, 0.04);
  --glass-border: rgba(255, 255, 255, 0.12);
  --glass-specular: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.02) 100%);
  --glass-blur: blur(24px);
  --glass-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.6);
}
```

---

## 4. 🪐 Deep Space Cosmic
```css
:root {
  --cosmic-bg-base: #06040d;
  --cosmic-bg-surface: rgba(20, 14, 38, 0.75);
  --cosmic-purple: #a855f7;
  --cosmic-indigo: #6366f1;
  --cosmic-cyan: #38bdf8;
  --cosmic-glow: 0 0 35px rgba(168, 85, 247, 0.25);
}
```

---

## 5. 🏛️ Neobrutalism / Bold Electric
```css
:root {
  --neo-bg-canvas: #fef08a;
  --neo-card-bg: #ffffff;
  --neo-border: 2.5px solid #000000;
  --neo-shadow: 5px 5px 0px #000000;
  --neo-shadow-hover: 7px 7px 0px #000000;
  --neo-accent-primary: #ff5252;
  --neo-accent-secondary: #00e5ff;
}
```

---

## 6. 🌿 Nordic Clean / Organic Minimalist Light
```css
:root {
  --nordic-bg-canvas: #fafafa;
  --nordic-bg-surface: #ffffff;
  --nordic-border: #e4e4e7;
  --nordic-text-primary: #18181b;
  --nordic-text-muted: #71717a;
  --nordic-accent: #059669;
  --nordic-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}
```
