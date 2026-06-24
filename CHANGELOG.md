## FPS Overlay v5.3

- Fixed TPS and MSPT showing "N/A" on dedicated servers
- OP players receive exact tick data via the vanilla debug sample stream
- Non-OP players receive an estimated TPS based on server time packets
- support for 26.2

## FPS Overlay v5.2

### New
* **Hide metric labels** New toggle in General HUD settings: "Show Metric Labels." Turn it off and your HUD shows only the numbers and units (like `100fps | 10ms`), with no text clutter. Great for a minimal look.

### Performance fixes
* **Fixed the FPS drop caused by the graph** This was the main bug from last version. The graph was drawing each bar one pixel at a time, which hammered the GPU. It now draws each bar in a single operation, cutting GPU work by up to 50x.
* **Graph overhead when hidden is now zero** Previously the game was still calculating graph data even when the graph wasn't visible. That's fixed — it skips all graph processing when you have it turned off.
* **Internal Cleanups** A couple of smaller internal cleanups to reduce unnecessary memory usage during startup.

## FPS Overlay v5.1

This release focuses on stability, compliance, and accuracy.
No new features — just making sure everything that's there works perfectly.

### Changes
- Fixed a thread safety issue in the performance tracking engine that could
  cause corrupted FPS readings under heavy load
- Fixed a crash that could occur when reordering metrics while the overlay
  was actively rendering
- Multiplayer MSPT and TPS now correctly show N/A instead of fake static values
- Removed CPU and GPU metrics — the only reliable way to read these required
  spawning external system processes

### What's in the mod (carried over from v5.0)
Thirteen trackable metrics: FPS, Average FPS, Frame Time, 1% Low, Ping,
TPS, MSPT, Memory, Coordinates, Biome, Chunks, FPS Graph, Min/Max Stats.

Full customization: drag to reposition, reorder metrics, rename labels,
theme presets (Classic Dark, Light, Glass), custom color palette, adaptive
colors, HUD scale, 10 keybinds, auto-hide on F3.
