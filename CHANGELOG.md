## FPS Overlay v5.1

This release focuses on stability, compliance, and accuracy.
No new features — just making sure everything that's there works perfectly.

### Changes
- Fixed a thread safety issue in the performance tracking engine that could
  cause corrupted FPS readings under heavy load
- Fixed a crash that could occur when reordering metrics while the overlay
  was actively rendering
- Multiplayer MSPT and TPS now correctly show N/A instead of fake static values

### What's in the mod (carried over from v5.0)
Thirteen trackable metrics: FPS, Average FPS, Frame Time, 1% Low, Ping,
TPS, MSPT, Memory, Coordinates, Biome, Chunks, FPS Graph, Min/Max Stats.

Full customization: drag to reposition, reorder metrics, rename labels,
custom color palette, adaptive colors, HUD scale, 10 keybinds, auto-hide on F3.
