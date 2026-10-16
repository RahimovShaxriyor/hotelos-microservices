---
name: accessibility-patterns
description: Design and build WCAG 2.1 (AA/AAA) compliant web interfaces with robust accessibility patterns. Use this skill when designing accessible UI components, implementing ARIA roles and attributes, managing keyboard focus, creating skip links, auditing for accessibility (A11y), ensuring color contrast compliance, or building screen-reader friendly layouts. Triggers on: "make it accessible", "WCAG compliance", "ARIA roles", "keyboard navigation", "focus trap", "screen reader support", "color contrast", "alt text", "A11y patterns", "accessible tabs", "accessible modal", "accessible accordion". Always use this skill when building UI components to ensure inclusivity and compliance.
---

# Accessibility Patterns Skill

Build inclusive, compliant, and highly usable web interfaces. This skill provides the architectural mental model and implementation patterns for WCAG 2.1 AA/AAA compliance.

## The A11y Mental Model

Accessibility is not a checklist; it's a structural requirement. Think of it in four layers (POUR):

1. **Perceivable**: Can users see/hear it? (Contrast, Alt text, Captions)
2. **Operable**: Can users interact with it? (Keyboard, No traps, Timing)
3. **Understandable**: Can users follow the logic? (Errors, Labels, Predictability)
4. **Robust**: Does it work across tech? (Clean HTML, Valid ARIA)

---

## Interactive Component Patterns

Modern web apps use complex components that often break accessibility. Use these patterns:

### 1. Accessible Modals (Dialogs)

A modal must trap focus and close on `Esc`.

```javascript
/* 
  Required Attributes:
  - role="dialog" or "alertdialog"
  - aria-modal="true"
  - aria-labelledby="[id_of_title]"
  - aria-describedby="[id_of_description]"
*/

function Modal({ isOpen, onClose, title, children }) {
  const modalRef = useRef();

  // 1. Focus Trap Logic
  useEffect(() => {
    if (isOpen) {
      const focusableElements = modalRef.current.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      const handleTab = (e) => {
        if (e.key === 'Tab') {
          if (e.shiftKey && document.activeElement === firstElement) {
            lastElement.focus();
            e.preventDefault();
          } else if (!e.shiftKey && document.activeElement === lastElement) {
            firstElement.focus();
            e.preventDefault();
          }
        }
      };

      document.addEventListener('keydown', handleTab);
      firstElement?.focus(); // Initial focus
      return () => document.removeEventListener('keydown', handleTab);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="overlay" onClick={onClose}>
      <div 
        ref={modalRef}
        role="dialog" 
        aria-modal="true" 
        aria-labelledby="modal-title"
      >
        <h2 id="modal-title">{title}</h2>
        {children}
        <button onClick={onClose} aria-label="Close modal">×</button>
      </div>
    </div>
  );
}
```

### 2. Accessible Tabs

Tabs require specific keyboard interactions: Left/Right to switch.

```html
<!-- Pattern: Tabbed Interface -->
<div class="tabs">
  <div role="tablist" aria-label="Product Features">
    <button role="tab" 
            aria-selected="true" 
            aria-controls="panel-1" 
            id="tab-1">
      Performance
    </button>
    <button role="tab" 
            aria-selected="false" 
            aria-controls="panel-2" 
            id="tab-2" 
            tabindex="-1">
      Security
    </button>
  </div>

  <div role="tabpanel" 
       id="panel-1" 
       aria-labelledby="tab-1">
    <!-- Content 1 -->
  </div>
  <div role="tabpanel" 
       id="panel-2" 
       aria-labelledby="tab-2" 
       hidden>
    <!-- Content 2 -->
  </div>
</div>
```

---

## Semantic HTML & ARIA Rules

### Rule 1: The First Rule of ARIA
**If you can use a native HTML element with the behavior you need already built-in, do that instead of using ARIA.**
- ❌ `<div onclick="..." role="button">`
- ✅ `<button type="button">`

### Rule 2: Visual vs. Audio
Use `aria-hidden="true"` for decorative icons. Use `sr-only` (Screen Reader Only) classes for labels that should be heard but not seen.

```html
<button>
  <span class="sr-only">Add to favorites</span>
  <svg aria-hidden="true">...</svg>
</button>
```

### Rule 3: Live Regions
Use `aria-live` for content that updates dynamically (toasts, count updates).
- `aria-live="polite"`: Wait until the user is idle to announce.
- `aria-live="assertive"`: Announce immediately (use sparingly).

---

## Contrast & Visual Accessibility

### Target Standards:
- **WCAG AA**: 4.5:1 for normal text, 3:1 for large text (18pt+).
- **WCAG AAA**: 7:1 for normal text, 4.5:1 for large text.

### Implementation Checklist:
- [ ] No information is conveyed by color alone (e.g., error fields must have icons or text, not just red borders).
- [ ] Targets (buttons/links) are at least 44x44px for touch/motor accessibility.
- [ ] Focus indicators (outlines) are clearly visible and never removed with `outline: none` without a high-visibility replacement.

---

## Accessibility Audit Checklist

When reviewing code, verify:
- [ ] **Tab Order**: Does the navigation move logically? No skips or traps?
- [ ] **Alt Text**: Do images have meaningful `alt` text (or `alt=""` if decorative)?
- [ ] **Form Labels**: Every input must have a `<label for="...">` or `aria-labelledby`.
- [ ] **Heading Hierarchy**: Only one `h1`, no skipping levels (e.g., `h1` directly to `h3`).
- [ ] **Landmarks**: Use `<nav>`, `<main>`, `<header>`, `<footer>`, `<aside>` correctly.
- [ ] **Language**: Does the `<html>` tag have a `lang="en"` attribute?

---

## Reference Resources

- [W3C WAI-ARIA Authoring Practices](https://www.w3.org/WAI/ARIA/apg/)
- [A11y Project Checklist](https://www.a11yproject.com/checklist/)
- [Inclusive Components (Heydon Pickering)](https://inclusive-components.design/)
