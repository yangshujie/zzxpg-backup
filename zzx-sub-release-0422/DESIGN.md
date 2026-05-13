# Design System Strategy: The Celestial Command Interface

## 1. Overview & Creative North Star
**Creative North Star: "The Orbital Curator"**

In high-stakes satellite simulation, the interface must transcend a simple "dashboard" and become an extension of the operator’s intent. This design system rejects the clunky, box-heavy aesthetics of legacy aerospace software in favor of an **Orbital Hierarchy**. 

The design breaks the traditional "grid-lock" through intentional layering and depth. We move beyond standard UI by treating the screen as a multi-dimensional viewport. Components do not sit *on* the background; they float within it. By utilizing wide-aperture spacing (3.5rem and above) against high-density data clusters, we create a rhythmic "Information Pulsing" effect—high-intensity data zones balanced by vast, cinematic negative space.

---

## 2. Colors & Surface Philosophy
This palette is rooted in the "Deep Space Blue" (`#0B0E14`), utilizing a sophisticated tonal range to define importance without visual clutter.

### The "No-Line" Rule
Traditional 1px solid borders are strictly prohibited for sectioning. Use background shifts instead. 
*   **Example:** A secondary data panel should use `surface_container_low` sitting atop a `surface` background. The transition should be felt through the tonal shift, not a hard line.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, frosted glass sheets:
*   **Base Layer:** `surface` (#10131a) - The infinite void.
*   **Structural Sections:** `surface_container_low` (#191c22) - Defining major workspace regions.
*   **Interactive Modules:** `surface_container_high` (#272a31) - Tactical panels and toolsets.
*   **Critical Overlays:** `surface_container_highest` (#32353c) - Dialogs and urgent telemetry alerts.

### The "Glass & Gradient" Rule
To achieve the "Cyber" aesthetic, primary CTAs and active states should leverage a linear gradient from `primary` (#a4e6ff) to `primary_container` (#00d1ff) at a 135-degree angle. This provides a "liquid light" feel that flat hex codes cannot replicate.

---

## 3. Typography: The Technical Editorial
We utilize a pairing of **Space Grotesk** for high-impact displays and **Inter** for mission-critical data legibility.

*   **Display & Headlines (Space Grotesk):** These should feel like navigational waypoints. Use `display-lg` for orbital identifiers. Maintain tight letter-spacing (-0.02em) to keep the "high-tech" look compressed and authoritative.
*   **Body & Labels (Inter):** The workhorse of the system. For complex satellite telemetry, use `label-md` or `label-sm` in all-caps with 0.05em letter spacing to mimic tactical instrumentation.
*   **Hierarchy Tip:** Never use bold weights for body text; instead, use `on_surface_variant` for secondary information and `primary` for emphasized data points.
*   **Accessibility Rule:** To maintain mission-critical legibility, all readable text **must have a font-size of 14px or greater**. Smaller dimensions are strictly reserved for non-essential decorative metadata.

---

## 4. Elevation & Depth
In this system, depth is a functional tool for reducing cognitive load.

*   **The Layering Principle:** Stacking is the primary method of separation. A `surface_container_lowest` card nested inside a `surface_container_low` panel creates a "sunken" effect, perfect for data input fields.
*   **Ambient Shadows:** For floating tactical widgets, use extra-diffused shadows.
    *   *Spec:* `0px 24px 48px rgba(0, 209, 255, 0.06)` — Note the tint of `primary_container` in the shadow to simulate light bleed from the satellite monitors.
*   **The "Ghost Border" Fallback:** If a container requires a boundary (e.g., in a high-density 3D viewport), use the `outline_variant` token at **15% opacity**. This creates a "glimmer" edge rather than a wall.
*   **Glassmorphism:** Apply a `backdrop-filter: blur(12px)` to any component using `surface_container_highest`. This ensures that as satellites move behind the UI in the 3D container, the motion is felt but does not distract.

---

## 5. Components

### Tactical Control Buttons
*   **Primary:** Gradient fill (`primary` to `primary_container`). `0.25rem` (DEFAULT) roundedness. No border.
*   **Secondary/Ghost:** `outline` token at 20% opacity with a `primary` glow on hover.
*   **Status Buttons:** For "Healthy" states, use `secondary_container` (#2ff801) with `on_secondary` text.

### High-Density Data Tables
*   **Layout:** Forbid divider lines. Use `surface_container_low` for the header row and a 1px `surface_container_high` bottom padding on rows to create a "gap" feel.
*   **Typography:** All numeric data must use `label-md` for tabular lining consistency.

### Real-Time Line Charts
*   **Styling:** Use a 2px stroke width for the primary data line with a 10% opacity `primary` fill gradient beneath the line (Area chart style) to give the data "weight."

### Tactical Input Fields
*   **State:** Default fields use `surface_container_low` with a bottom-only `outline_variant` border (2px). On focus, the border animates to a `primary` glow.

### 3D Orbit Containers
*   **Frame:** Use a "Ghost Border" frame. The corners should have 8px L-shaped accents (using `primary`) to simulate a camera viewfinder.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use `20` (4.5rem) spacing to separate unrelated data clusters.
*   **Do** use `secondary` (Neon Green) sparingly. It is a "Status: Nominal" signal, not a decorative color.
*   **Do** embrace asymmetry. Offset your 3D viewports to the right while keeping tactical controls pinned to a high-density left sidebar.

### Don’t:
*   **Don’t** use pure black (#000000). Always use the `surface` tokens to maintain the "Deep Space" depth.
*   **Don’t** use standard "Drop Shadows." If it doesn't look like it's glowing or floating in a vacuum, it’s too heavy.
*   **Don’t** use `xl` roundedness (0.75rem) on tactical components. Keep things "sharp" and "technical" with `sm` (0.125rem) or `DEFAULT` (0.25rem).