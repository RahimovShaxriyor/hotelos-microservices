---
name: animation-systems
description: Design and implement high-performance, purposeful UI animations and transitions. Use this skill when building interactive UI components, orchestrating complex animation sequences, implementing spring-based physics, optimizing for 60fps performance, handling layout transitions (FLIP), or ensuring accessible motion (Reduced Motion). Triggers on: "add animation", "Framer Motion", "GSAP", "spring physics", "layout transitions", "stagger effect", "orchestrate animations", "performance optimization", "interactive UI", "micro-animations", "reduce motion", "A11y motion". Always use this skill when creating delightful, premium-feeling user experiences.
---

# Animation Systems Skill

Create meaningful, high-performance UI animations. This skill provides the mental model for motion design and implementation patterns for Framer Motion and GSAP.

---

## Motion Philosophy: The Three P's

1. **Purpose**: Animation should guide attention, provide feedback, or explain a mental model. Never animate "just because."
2. **Pacing**: Use consistent easing and duration. UI animations should usually be fast (150ms–350ms).
3. **Performance**: Only animate properties that don't trigger layout: `transform` (scale, rotate, translate) and `opacity`.

---

## Implementation: Framer Motion (Recommended for React)

### 1. The `motion` Component

```jsx
import { motion } from 'framer-motion';

// Basic hover and tap effect
<motion.button
  whileHover={{ scale: 1.05 }}
  whileTap={{ scale: 0.95 }}
  transition={{ type: "spring", stiffness: 400, damping: 17 }}
>
  Click Me
</motion.button>
```

### 2. Orchestration (Variants)

Use variants to synchronize parent and child animations.

```jsx
const listVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1, // Stagger children by 0.1s
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0 }
};

function List({ items }) {
  return (
    <motion.ul variants={listVariants} initial="hidden" animate="visible">
      {items.map(item => (
        <motion.li key={item.id} variants={itemVariants}>
          {item.text}
        </motion.li>
      ))}
    </motion.ul>
  );
}
```

### 3. Layout Transitions (Magic Motion)

Use `layout` to automatically animate changes in size, position, or hierarchy.

```jsx
<motion.div layout>
  {isOpen && <Details />}
</motion.div>
```

---

## Implementation: GSAP (Recommended for Vanilla / Performance-Critical)

### 1. The Timeline Pattern

```javascript
import gsap from 'gsap';

const tl = gsap.timeline({ defaults: { ease: "power2.out", duration: 0.4 } });

tl.from(".hero-text", { y: 50, opacity: 0 })
  .from(".cta-button", { scale: 0.8, opacity: 0 }, "-=0.2") // Starts 0.2s early
  .to(".bg-shape", { rotate: 360, duration: 2, repeat: -1, ease: "none" });
```

---

## High-Performance Animation Principles

### 1. Avoid Layout Thrashing
Do **not** animate `width`, `height`, `top`, `left`, `margin`, or `padding`. These trigger a "reflow" which is expensive.
- ❌ `animate={{ width: "100%" }}`
- ✅ `animate={{ scaleX: 1 }}` (and use `transform-origin`)

### 2. Use GPU Acceleration
Ensure elements are promoted to their own compositor layer using `will-change`.
```css
.animated-layer {
  will-change: transform, opacity;
}
```

---

## Accessible Motion (A11y)

Respect users who have "Reduce Motion" enabled in their OS.

```jsx
// Framer Motion Hook
import { useReducedMotion } from 'framer-motion';

function MyComponent() {
  const shouldReduceMotion = useReducedMotion();
  
  const transition = shouldReduceMotion 
    ? { duration: 0 } 
    : { type: "spring", stiffness: 300 };

  return <motion.div animate={{ opacity: 1 }} transition={transition} />;
}
```

---

## Animation Quality Checklist

- [ ] **Eases**: "Power2", "Cubic-Bezier", or "Spring". Never use "Linear" unless for constant rotation.
- [ ] **Duration**: 100ms (fast/micro), 300ms (standard), 500ms (complex/large).
- [ ] **Interruption**: Animations should be cancellable or reversable without "jumping."
- [ ] **Accessibility**: Does the UI work (instantly) with reduced motion?
- [ ] **Pacing**: Do elements enter and exit with consistent physics?
- [ ] **Overkill**: Are there more than 3 distinct animations happening simultaneously? If so, simplify.

---

## Reference Resources

- [Framer Motion Documentation](https://www.framer.com/motion/)
- [GSAP Learning Center](https://gsap.com/resources/)
- [Material Design Motion Patterns](https://m3.material.io/styles/motion/overview)
