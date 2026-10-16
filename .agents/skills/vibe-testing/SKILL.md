---
name: vibe-testing
description: Implement visual regression testing and qualitative UI "vibe checks" to ensure premium design quality. Use this skill when setting up visual regression suites (Playwright, Percy), performing qualitative UI audits, enforcing design consistency across screens, auditing spacing/typography/pacing, or defining acceptable visual deviation thresholds. Triggers on: "visual regression", "vibe check", "UI quality", "premium feel", "consistent spacing", "Percy", "Playwright visual comparison", "design audit", "visual snapshots", "UI testing", "screenshot testing". Always use this skill when verifying that a UI looks and feels high-quality.
---

# Vibe Testing Skill

Ensuring a UI doesn't just work, but feels premium and consistent. This skill focuses on automated visual regression and the human-centric heuristics of "vibe checking."

---

## The "Vibe Check" Heuristics

A "vibe check" is a qualitative audit for high-quality UI. Use these five criteria:

1. **Spacing Internal Consistency**: Do similar elements have the same padding/gap? (e.g., are all cards using `p-6`?)
2. **Pacing**: Is the transition speed consistent across the app?
3. **Information Density**: Is the screen too crowded or too sparse for its purpose?
4. **Visual Hierarchy**: Is the most important action clearly distinguishable without being obnoxious?
5. **Micro-Polsih**: Are icons aligned correctly? Do shadows feel natural? Are corners consistent?

---

## Automated Visual Regression: Playwright

Playwright is the industry standard for local visual snapshots.

### 1. Basic Snapshot Pattern

```javascript
test('homepage matches snapshot', async ({ page }) => {
  await page.goto('http://localhost:3000');
  
  // Wait for animations and images to load
  await page.waitForLoadState('networkidle');
  
  // Compare to baseline
  await expect(page).toHaveScreenshot('homepage.png', {
    maxDiffPixelRatio: 0.01, // 1% deviation allowed
    animations: 'disabled',  // Stabilize the screenshot
  });
});
```

### 2. Component Isolation Testing

Test specific UI components instead of full pages to reduce fragility.

```javascript
test('button variants', async ({ page }) => {
  await page.goto('/components/button');
  const primaryBtn = page.getByRole('button', { name: 'Primary' });
  
  await expect(primaryBtn).toHaveScreenshot('button-primary.png');
});
```

---

## Managed Visual Regression: Percy / Chromatic

For large-scale teams, use a managed tool that handles diff approval workflows.

### Pattern: Percy Integration

1. **Capture snapshots in CI**:
   ```bash
   percy exec -- playwright test
   ```
2. **Reviewing Diffs**: Percy provides a side-by-side UI where designers can approve intended changes (e.g., updating a color palette) or reject regressions.

---

## Effective Snapshot Strategy

- **Mask Dynamic Data**: Hide dates, user IDs, or counts to prevent false negatives.
  ```javascript
  await page.addStyleTag({ content: '.dynamic-user-id { visibility: hidden; }' });
  ```
- **Stabilize Fonts**: Standardize on one font to prevent "font-smoothing" diffs across machines.
- **Set the Viewport**: Always specify `viewport: { width: 1280, height: 720 }`.

---

## Visual Thresholding Rules

- **Zero Tolerance (0% pixels)**: Core branding, typography, and spacing.
- **Loose Threshold (1% - 3% pixels)**: Complex imagery, anti-aliasing variations, or dynamic content sections.
- **Region-Only**: Use `page.locator('.header').screenshot()` to only test stable regions.

---

## Vibe Check Checklist

Before shipping, perform a manual sweep:
- [ ] **Alignment**: Draw mental lines. Do elements line up on the grid?
- [ ] **Contrast**: Check your primary action against the background.
- [ ] **Empty States**: Do they look intentional or like an error?
- [ ] **Loading States**: Are skeletons the same size as the final content?
- [ ] **Corners**: Are `border-radius` values consistent (e.g., `8px` everywhere)?
- [ ] **Scaling**: Does the UI feel "right" at 110% and 90% zoom?

---

## Reference Resources

- [Playwright Visual Comparisons](https://playwright.dev/docs/test-snapshots)
- [Percy.io Documentation](https://percy.io/docs)
- [Refactoring UI (Adam Wathan & Steve Schoger)](https://www.refactoringui.com/)
