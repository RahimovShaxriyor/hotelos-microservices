# 💫 Master Animation & Micro-Interaction Keyframes

```css
@keyframes pulse-ring {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 0.3; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

@keyframes scanline-sweep {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(1000%); }
}

@keyframes border-glow-rotate {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes shimmer-sweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes subtle-float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-4px); }
}
```
