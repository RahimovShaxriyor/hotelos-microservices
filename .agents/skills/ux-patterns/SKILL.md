---
name: ux-patterns
description: Design user experience flows, interaction patterns, information architecture, and UX decisions for digital products. Use this skill when the user needs to think through user flows, onboarding sequences, navigation architecture, empty states, error handling UX, form design, modal patterns, toast/notification systems, loading states, progressive disclosure, accessibility UX, or any question about how an interface should behave rather than how it should look. Triggers on: "how should this flow work", "design the onboarding", "what happens when the user does X", "user journey", "navigation structure", "empty state", "error state", "UX for", "interaction design", "information architecture", "IA", "user research patterns", "how do I handle X in the UI". Always use this skill when the question is about behaviour, flow, or structure rather than visual aesthetics.
---

# UX Patterns Skill

This skill covers the full range of UX design decisions: how interfaces should behave, how users move through products, and how to handle every state a user might encounter.

## UX Decision Framework

Every UX decision should be evaluated against four questions:

1. **Does the user understand where they are?** (orientation)
2. **Does the user know what they can do?** (affordances)
3. **Does the user know what happened?** (feedback)
4. **Can the user recover from mistakes?** (reversibility)

If the answer to any of these is "no" — the UX is broken.

---

## User Flow Design

### Flow Anatomy

Every user flow has three phases:
```
Entry → Core Loop → Exit

Entry:    How does the user arrive? What context do they bring?
Core Loop: The repeating unit of value. What do they do every session?
Exit:     What ends the session? What do they take away?
```

### Flow Mapping Rules

1. **Map the happy path first** — what happens when everything works
2. **Then map every branch** — what if the user doesn't have data? what if the API fails?
3. **Then map the edge cases** — first-time user, power user, error recovery
4. **Never leave a branch without a destination** — every state needs a next action

### Decision Points

At every branch in a flow, ask:
- Is this decision necessary? Can the system decide?
- If the user must decide, do they have enough context?
- What's the default? (Always have a sane default)
- What are the consequences of each choice? Are they communicated?

---

## Onboarding Patterns

### Onboarding Goals

Onboarding is not a tour. It is the fastest path to the user's first moment of value.

**First value moment (FVM)**: The specific instant when the user understands "yes, this is useful for me." Design every onboarding step to reach the FVM as fast as possible. Remove any step that doesn't contribute to FVM.

### Onboarding Patterns by Product Type

**Progressive onboarding** (best for most SaaS)
- Show the empty app immediately
- Introduce features as the user needs them (contextual tooltips, inline callouts)
- Never block the user from exploring

**Setup wizard** (for products that require configuration before value)
- Maximum 5 steps
- Show progress (Step 2 of 4)
- Every step must be skippable or have a sensible default
- Never ask for information you can collect later

**Sample data onboarding** (for data-heavy products)
- Pre-populate with realistic sample data
- Allow user to clear and start fresh at any time
- Clearly label what is sample vs. real

**Interactive tutorial** (for tools with a learning curve)
- Guide through one complete workflow
- Let user do the actions, don't just show them
- Keep it to the single most valuable workflow — not everything

### Onboarding Anti-Patterns

- Requiring email verification before showing any value
- A 10-screen feature tour before the user can use the product
- Asking for credit card before the user has experienced value
- Collecting unused profile information during setup
- Showing empty dashboards without guidance

---

## Navigation Architecture

### Navigation Hierarchy Rules

**Max 2 levels for primary navigation.** Three levels signals an IA problem, not a navigation problem.

### Pattern Selection

| Pattern | When to Use | Max Items |
|---|---|---|
| Top nav | Simple apps, ≤7 primary destinations | 5–7 |
| Left sidebar | Complex apps, many sections, secondary actions | 10–15 primary |
| Collapsing sidebar | Apps where screen real estate matters | 10–15 |
| Tabs | Secondary navigation within a section | 3–6 |
| Bottom nav | Mobile-first apps | 4–5 |
| Breadcrumbs | Deep hierarchical content | Any depth |
| Command palette | Power users, many actions | Unlimited |

### Information Architecture Principles

**Mental model alignment**: Navigation labels must match how users think, not how the engineering team organised the database.

**Progressive disclosure**: Surface the 20% of features used 80% of the time. Hide power features until needed.

**Predictability**: The user should be able to predict where something lives before they look for it.

### Navigation Labels

- Use nouns for destinations: "Projects", "Settings", "Team"
- Use verbs for actions: "Create", "Import", "Export"
- Never use internal jargon in navigation
- Never use the word "Dashboard" as a nav item — use the actual name of what's on it

---

## State Design

Every piece of UI exists in multiple states. Design all of them.

### The 7 States

```
1. Empty          — No data yet (first use, after delete)
2. Loading        — Data is being fetched
3. Partial load   — Some data, more coming
4. Populated      — Normal state with data
5. Error          — Something went wrong
6. Zero results   — Search / filter returned nothing
7. Limit reached  — Quota, plan limit, max items
```

### Empty State Design

Empty states are the most underdesigned state in most products.

Good empty state anatomy:
```
[Illustration or icon]   — Optional but makes it feel less dead
Heading                  — What is empty and why
Body text                — What the user will see here once they add data
Primary CTA              — The single action to fill this space
```

Rules:
- Every empty state needs ONE clear next action
- Never leave a table or list with just column headers
- Empty state copy should describe the value of the filled state, not just "No items found"

### Loading State Design

```
< 100ms  — No loading indicator needed
100–500ms — Skeleton screen (preferred) or spinner
500ms–3s  — Skeleton screen with progress hint
> 3s     — Progress bar, cancel option, background loading with notification
```

**Skeleton screens vs spinners**: Skeleton screens (gray placeholder shapes matching the content layout) reduce perceived wait time by 40% compared to spinners. Use them for any list or card content.

### Error State Design

Every error needs:
1. What went wrong (plain language, not error codes)
2. Why it went wrong (if helpful and not technical)
3. What the user should do next
4. A way to retry or recover

```
❌ "Error 503"
❌ "Something went wrong"
✅ "We couldn't save your changes — your internet connection dropped. Try again?"
```

### Zero Results Design

Zero results ≠ empty state. The user did something (searched, filtered) and got nothing.

```
"No results for 'payment flow'"

Did you mean 'payment page'?
Try clearing your filters
Browse all templates →
```

---

## Form UX Patterns

### Form Design Rules

1. **One column** for most forms — two columns increase cognitive load and break on mobile
2. **Label above input** — never placeholder-as-label
3. **Inline validation** — validate on blur, not on keystroke (or on submit only for short forms)
4. **Error messages below the field** — not in a summary at the top
5. **Required vs optional** — mark optional fields, not required (most fields in a form should be required)
6. **Smart defaults** — pre-fill what you know, default to the most common choice
7. **Autofocus** — on the first field when the form opens

### Progressive Forms

For long forms, break into sections:
- Group related fields visually
- Show only what's needed at each step
- Never paginate a form without showing total steps and allowing back-navigation
- Save progress automatically when possible

### Validation Timing

```
On keystroke:      Only for real-time feedback (password strength meter, character count)
On blur (leave):   For format validation (email, phone, URL)
On submit:         For cross-field validation (password confirmation)
```

---

## Notification & Feedback Patterns

### Choosing the Right Feedback Pattern

| Pattern | Use For | Duration |
|---|---|---|
| Toast / Snackbar | Success, non-critical info | 3–5 seconds auto-dismiss |
| Inline message | Form errors, contextual warnings | Persistent until resolved |
| Banner | Product-level announcements, degraded service | Dismissible, persistent |
| Modal dialog | Destructive actions requiring confirmation | Manual dismiss required |
| Tooltip | Field hints, icon labels | Hover / focus triggered |
| Badge | Unread count, status indicators | Always visible |

### Toast Rules

- Max one toast visible at a time (queue if needed)
- Success toasts: 3 seconds, auto-dismiss
- Error toasts: persistent until dismissed manually
- Include undo action on toasts for reversible actions (delete, archive)
- Position: bottom-right desktop, bottom-center mobile

### Modal Rules

- Use modals ONLY for: destructive action confirmation, forms that create a new object, media lightboxes
- Never use modals for: information that could be inline, navigation, complex multi-step flows
- Always: escape key closes, clicking backdrop closes (except destructive confirmations), first input autofocused
- Never: nest modals

---

## Accessibility UX Patterns

### Keyboard Navigation

Every interactive element must be reachable and usable by keyboard:
- Tab order follows visual reading order
- Skip links for main content
- Focus rings visible and high-contrast
- Keyboard shortcuts documented

### Screen Reader Patterns

```
Images: alt text that describes function, not appearance
Icons: aria-label on icon-only buttons ("Close dialog", not "X")
Loading states: aria-live="polite" for dynamic content updates
Forms: label elements associated with inputs via htmlFor / id
Tables: caption, thead, th with scope attributes
```

### Accessible Interaction Patterns

- **Touch targets**: Minimum 44×44px on mobile
- **Focus management**: When a modal opens, focus moves inside it; when it closes, focus returns to trigger
- **Error announcements**: Errors are announced to screen readers on form submit
- **Color alone**: Never use color as the only differentiator for state — add icon or text

---

## UX Pattern Reference

### Common Interaction Patterns

**Optimistic UI**: Update the UI immediately, then sync to server in background. On failure, revert with error message. Best for: likes, reactions, drag-and-drop reordering, inline edits.

**Infinite scroll vs pagination**: Use pagination when users need to return to a specific position. Use infinite scroll for feeds where position doesn't matter.

**Progressive disclosure**: Show the simplest version first. Reveal advanced options on demand. "Advanced settings →" not all settings upfront.

**Undo vs confirmation**: Prefer undo over confirmation dialogs for reversible actions. "Message deleted. Undo?" is better UX than "Are you sure you want to delete this message?".

**Drag and drop**: Always provide an alternative (keyboard reorder, input-based order). Never require drag and drop as the only way to do something.

**Inline editing**: Click-to-edit for single values. Open a panel or modal for multi-field edits. Always show explicit save/cancel affordances.

---

## Flows by Product Type

### SaaS / Productivity App Flow
```
Signup → Onboarding (FVM) → Core Loop (Create → Use → Review) → Share/Collaborate → Settings/Upgrade
```

### E-commerce Flow
```
Discovery → Product Detail → Cart → Checkout (Address → Payment → Confirm) → Post-purchase
```

### Developer Tool Flow
```
Install/Setup → First integration → Documentation → Advanced configuration → Team/API keys
```

### Dashboard / Analytics Flow
```
Overview → Drill-down → Filter/Segment → Export/Share → Alert setup
```