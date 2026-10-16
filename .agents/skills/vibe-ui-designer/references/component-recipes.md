# 🧩 Battle-Tested Component Recipes

Production-ready, framework-agnostic component implementations in Tailwind CSS and pure CSS.

---

## 1. Floating Island Dynamic Navbar (Glassmorphic)
```html
<nav class="fixed top-5 left-1/2 -translate-x-1/2 z-50 flex items-center gap-6 rounded-full border border-white/10 bg-slate-950/70 px-6 py-2.5 shadow-2xl shadow-black/50 backdrop-blur-xl">
  <div class="flex items-center gap-2">
    <div class="h-3 w-3 rounded-full bg-cyan-400 shadow-[0_0_10px_#00f0ff]"></div>
    <span class="font-mono text-sm font-extrabold tracking-wider text-white">VIBE<span class="text-cyan-400">UI</span></span>
  </div>
  <div class="h-4 w-px bg-white/15"></div>
  <div class="flex items-center gap-1 font-mono text-xs text-slate-300">
    <a href="#overview" class="rounded-full px-3 py-1.5 transition-colors hover:bg-white/10 hover:text-white">Overview</a>
    <a href="#components" class="rounded-full px-3 py-1.5 transition-colors hover:bg-white/10 hover:text-white">Components</a>
    <a href="#themes" class="rounded-full px-3 py-1.5 transition-colors hover:bg-white/10 hover:text-white">Themes</a>
  </div>
  <button class="rounded-full bg-gradient-to-r from-cyan-400 to-blue-500 px-4 py-1.5 font-mono text-xs font-bold text-slate-950 transition-all hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] active:scale-95">
    Deploy ⚡
  </button>
</nav>
```

---

## 2. Interactive Spotlight Bento Grid Card
```html
<div class="group relative overflow-hidden rounded-2xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:border-cyan-500/30 hover:shadow-2xl hover:shadow-cyan-500/10">
  <div class="pointer-events-none absolute -right-16 -top-16 h-40 w-40 rounded-full bg-cyan-500/15 blur-3xl transition-transform duration-500 group-hover:scale-150"></div>
  <div class="relative z-10">
    <div class="flex items-center justify-between">
      <span class="font-mono text-xs font-semibold tracking-wider text-cyan-400">TELEMETRY // LIVE</span>
      <span class="flex h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_8px_#00ff88]"></span>
    </div>
    <h3 class="mt-4 font-sans text-xl font-bold text-white group-hover:text-cyan-300 transition-colors">
      Real-Time Signal Mesh
    </h3>
    <p class="mt-2 text-sm leading-relaxed text-slate-400">
      Zero-latency distributed audio streaming with client-side WebAssembly filtering and spectral rendering.
    </p>
    <div class="mt-6 flex items-center justify-between border-t border-white/5 pt-4">
      <div class="font-mono text-xs text-slate-400">Latency: <span class="text-emerald-400">1.2ms</span></div>
      <span class="font-mono text-xs font-bold text-cyan-400 group-hover:translate-x-1 transition-transform">EXPLORE →</span>
    </div>
  </div>
</div>
```

---

## 3. Raycast-Style Spotlight Command Palette (`Cmd+K`)
```html
<div class="fixed inset-0 z-50 flex items-start justify-center bg-black/60 p-4 pt-24 backdrop-blur-md">
  <div class="w-full max-w-xl overflow-hidden rounded-2xl border border-white/15 bg-slate-950/90 shadow-2xl shadow-black/80 backdrop-blur-2xl">
    <div class="flex items-center border-b border-white/10 px-4 py-3">
      <span class="text-slate-400 text-lg mr-3">🔍</span>
      <input type="text" placeholder="Type a command or search actions..." class="w-full bg-transparent font-mono text-sm text-white placeholder-slate-500 outline-none">
      <kbd class="rounded border border-white/20 bg-white/5 px-2 py-0.5 font-mono text-[10px] text-slate-400">ESC</kbd>
    </div>
    <div class="max-h-72 overflow-y-auto p-2">
      <div class="px-3 py-1.5 font-mono text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Suggestions</div>
      <div class="flex items-center justify-between rounded-xl px-3 py-2.5 transition-colors hover:bg-cyan-500/10 hover:text-cyan-300 cursor-pointer">
        <div class="flex items-center gap-3">
          <span>⚡</span>
          <span class="text-sm font-medium text-slate-200">Trigger DSP Stem Separation</span>
        </div>
        <kbd class="font-mono text-xs text-slate-500">↵</kbd>
      </div>
    </div>
  </div>
</div>
```
