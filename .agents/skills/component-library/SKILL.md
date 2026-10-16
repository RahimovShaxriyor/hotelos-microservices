---
name: component-library
description: Build reusable UI component libraries, design systems, and component APIs for React, Vue, Svelte, or vanilla HTML/CSS. Use this skill when creating a component library from scratch, designing component APIs and props, building design system components (buttons, inputs, cards, modals, tables, etc.), implementing accessible components, structuring a component folder, writing component stories, or creating compound components. Triggers on: "build a component library", "design system components", "component API", "button component", "input component", "modal component", "table component", "card component", "compound components", "headless components", "accessible components", "Radix", "shadcn", "component props design", "variant props". Always use this skill when the output is reusable components intended to be used across a codebase.
---

# Component Library Skill

Build reusable, accessible UI components with well-designed APIs. This skill covers component architecture, prop design, accessibility, and the full library of common components.

## Component Design Philosophy

A great component:
1. **Does one thing well** — composable over configurable
2. **Has a predictable API** — follows HTML/DOM conventions where possible
3. **Is accessible by default** — ARIA, keyboard navigation, focus management built in
4. **Accepts escape hatches** — `className`, `style`, `ref`, `...rest` props
5. **Is unstyled or theme-aware** — not locked to one visual design

---

## Component API Design

### Prop Naming Conventions

```typescript
// Use HTML-native names where they exist
value, onChange, disabled, placeholder, type

// Use 'variant' not 'type' for visual variants (type is reserved in HTML)
variant: 'primary' | 'secondary' | 'ghost' | 'destructive'

// Use 'size' consistently
size: 'sm' | 'md' | 'lg'

// Boolean props: positive framing, no 'is' prefix needed in JSX
disabled     // not isDisabled
loading      // not isLoading
fullWidth    // not isFullWidth

// Callbacks: on + EventName
onClick, onChange, onClose, onSubmit

// Render props for custom rendering
renderIcon, renderLabel, renderEmpty
```

### Escape Hatches (always include)

```typescript
interface BaseProps {
  className?: string;       // CSS class merging
  style?: React.CSSProperties; // Inline style override
  'data-testid'?: string;   // Testing
}
```

---

## Core Component Implementations

### Button

```tsx
// components/Button/Button.tsx
import { forwardRef, ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive' | 'link';
type ButtonSize = 'sm' | 'md' | 'lg' | 'icon';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary:     'bg-accent text-white hover:bg-accent-hover active:bg-accent-active',
  secondary:   'border border-border bg-bg-surface text-text-primary hover:bg-bg-subtle',
  ghost:       'text-text-primary hover:bg-bg-subtle',
  destructive: 'bg-status-error text-white hover:bg-red-700',
  link:        'text-accent underline-offset-4 hover:underline p-0 h-auto',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm:   'h-7 px-3 text-xs rounded-md gap-1.5',
  md:   'h-9 px-4 text-sm rounded-md gap-2',
  lg:   'h-11 px-5 text-base rounded-lg gap-2',
  icon: 'h-9 w-9 rounded-md',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  leftIcon,
  rightIcon,
  children,
  className,
  ...props
}, ref) => {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      aria-busy={loading}
      className={cn(
        'inline-flex items-center justify-center font-medium transition-colors',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2',
        'disabled:pointer-events-none disabled:opacity-50',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      {...props}
    >
      {loading && <Spinner className="h-4 w-4 animate-spin" aria-hidden />}
      {!loading && leftIcon}
      {children}
      {rightIcon}
    </button>
  );
});

Button.displayName = 'Button';
```

### Input

```tsx
// components/Input/Input.tsx
import { forwardRef, InputHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftAddon?: React.ReactNode;
  rightAddon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({
  label,
  error,
  hint,
  leftAddon,
  rightAddon,
  id,
  className,
  ...props
}, ref) => {
  const inputId = id || label?.toLowerCase().replace(/\s/g, '-');
  const errorId = error ? `${inputId}-error` : undefined;
  const hintId = hint ? `${inputId}-hint` : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-text-primary">
          {label}
          {props.required && <span aria-hidden="true" className="ml-0.5 text-status-error">*</span>}
        </label>
      )}
      <div className="relative flex items-center">
        {leftAddon && (
          <div className="absolute left-3 text-text-muted">{leftAddon}</div>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={!!error}
          aria-describedby={[errorId, hintId].filter(Boolean).join(' ') || undefined}
          className={cn(
            'h-9 w-full rounded-md border border-border bg-bg-surface px-3 py-2',
            'text-sm text-text-primary placeholder:text-text-muted',
            'transition-colors',
            'focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0 focus:border-accent',
            'disabled:cursor-not-allowed disabled:opacity-50',
            error && 'border-status-error focus:ring-status-error',
            leftAddon && 'pl-10',
            rightAddon && 'pr-10',
            className
          )}
          {...props}
        />
        {rightAddon && (
          <div className="absolute right-3 text-text-muted">{rightAddon}</div>
        )}
      </div>
      {error && (
        <p id={errorId} className="text-xs text-status-error" role="alert">{error}</p>
      )}
      {hint && !error && (
        <p id={hintId} className="text-xs text-text-muted">{hint}</p>
      )}
    </div>
  );
});
```

### Card

```tsx
// components/Card/Card.tsx — Compound component pattern
import { HTMLAttributes, forwardRef } from 'react';
import { cn } from '@/lib/utils';

// Root
const Card = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('rounded-lg border border-border bg-bg-surface text-text-primary shadow-sm', className)}
      {...props}
    />
  )
);

// Sub-components
const CardHeader = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('flex flex-col gap-1.5 p-6', className)} {...props} />
  )
);

const CardTitle = forwardRef<HTMLHeadingElement, HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h3 ref={ref} className={cn('text-lg font-semibold leading-none tracking-tight', className)} {...props} />
  )
);

const CardContent = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('p-6 pt-0', className)} {...props} />
  )
);

const CardFooter = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('flex items-center p-6 pt-0', className)} {...props} />
  )
);

// Usage:
// <Card>
//   <CardHeader>
//     <CardTitle>Linear</CardTitle>
//   </CardHeader>
//   <CardContent>...</CardContent>
//   <CardFooter><Button>View Skill</Button></CardFooter>
// </Card>

export { Card, CardHeader, CardTitle, CardContent, CardFooter };
```

### Modal / Dialog

```tsx
// components/Modal/Modal.tsx
// Uses native <dialog> element for built-in accessibility
import { useEffect, useRef } from 'react';
import { cn } from '@/lib/utils';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
}

export function Modal({ open, onClose, title, description, children, className }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open) {
      dialog.showModal();
    } else {
      dialog.close();
    }
  }, [open]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const handleClose = () => onClose();
    dialog.addEventListener('close', handleClose);
    return () => dialog.removeEventListener('close', handleClose);
  }, [onClose]);

  return (
    <dialog
      ref={dialogRef}
      className={cn(
        'rounded-xl border border-border bg-bg-surface p-0 shadow-lg',
        'backdrop:bg-black/50',
        'open:animate-in open:fade-in-0 open:zoom-in-95',
        'w-full max-w-lg mx-auto mt-[10vh]',
        className
      )}
      onClick={e => { if (e.target === dialogRef.current) onClose(); }}
    >
      <div className="flex flex-col gap-4 p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">{title}</h2>
            {description && <p className="mt-1 text-sm text-text-secondary">{description}</p>}
          </div>
          <button
            onClick={onClose}
            aria-label="Close dialog"
            className="rounded-md p-1 text-text-muted hover:text-text-primary hover:bg-bg-subtle transition-colors"
          >
            <XIcon className="h-4 w-4" />
          </button>
        </div>
        {children}
      </div>
    </dialog>
  );
}
```

### Badge / Tag

```tsx
type BadgeVariant = 'default' | 'success' | 'warning' | 'error' | 'info' | 'accent';

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-bg-subtle text-text-secondary border-border',
  success: 'bg-status-success-bg text-status-success border-transparent',
  warning: 'bg-status-warning-bg text-status-warning border-transparent',
  error:   'bg-status-error-bg text-status-error border-transparent',
  info:    'bg-status-info-bg text-status-info border-transparent',
  accent:  'bg-accent-subtle text-accent-text border-transparent',
};

export function Badge({ variant = 'default', children, className }: BadgeProps) {
  return (
    <span className={cn(
      'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium',
      variantStyles[variant],
      className
    )}>
      {children}
    </span>
  );
}
```

---

## Component Folder Structure

```
src/components/
├── ui/                     — Primitive, unstyled / lightly-styled
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── index.ts        — export { Button } from './Button'
│   ├── Input/
│   ├── Card/
│   ├── Modal/
│   ├── Badge/
│   ├── Toast/
│   ├── Spinner/
│   └── index.ts            — Re-export all UI components
├── forms/                  — Form-specific components
│   ├── FormField.tsx       — Label + Input + Error wrapper
│   ├── Select.tsx
│   └── Checkbox.tsx
├── layout/                 — Page structure
│   ├── Container.tsx
│   ├── Stack.tsx
│   └── Grid.tsx
└── domain/                 — App-specific compound components
    ├── SkillCard.tsx
    └── SkillTable.tsx
```

---

## Accessibility Requirements by Component

| Component | ARIA Roles | Keyboard | Focus |
|---|---|---|---|
| Button | `button` (native) | Enter / Space = activate | Visible ring |
| Input | `textbox` (native) | Tab focus | Visible ring |
| Modal | `dialog`, `aria-modal`, `aria-labelledby` | Escape = close, Tab trapped inside | Moves to first element on open |
| Dropdown | `combobox` / `listbox` | Arrow keys, Enter, Escape | Stays on trigger when closed |
| Tabs | `tablist`, `tab`, `tabpanel` | Arrow keys between tabs | Focus follows selection |
| Toast | `alert` or `status` with `aria-live` | Dismiss button | Non-blocking |
| Table | `table`, `th scope` | Tab through cells if interactive | — |

---

## cn() Utility (Tailwind class merging)

```typescript
// lib/utils.ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

Dependencies: `npm install clsx tailwind-merge`

This merges conditional classes and resolves Tailwind conflicts:
```typescript
cn('px-4 py-2', isLarge && 'px-6', 'px-3')
// → 'py-2 px-3'  (last px wins, not both)
```