# Changelog

## v5.1

### Evolution & Multi-loader
- **Migrated to Architectury**: I've completely rewritten the mod so it now runs natively on both Fabric and NeoForge from the exact same codebase.
- **Minecraft 1.21.11**: Fully up to date and ready for the latest version of Minecraft.
- **Mojang Mappings**: Switched over to official Mojang mappings behind the scenes, which will make maintaining and updating the mod much easier going forward.

### New Metrics & Data
- **13 Trackable Metrics**: The HUD now supports FPS, Avg FPS, Frame Time, 1% Low, Memory, CPU Usage, GPU Usage, Ping, MSPT, TPS, Chunks, Coordinates, and Biome—all toggleable individually.
- **1% Low FPS**: A dedicated stat that tracks your worst frame spikes over a rolling window, so you can see your true minimum performance.
- **CPU & GPU Usage**: Real-time system utilization with native support for Windows, Linux, and macOS through platform-specific providers.
- **Chunks Tracking**: See your visible vs. completed chunks at a glance, so you know exactly how much of the world has loaded around you.
- **Coordinates & Biome**: Your current XYZ position and the biome you're standing in, displayed right on the overlay without needing F3.
- **Average FPS**: A rolling average calculated from a 1000-frame buffer for buttery-smooth readings that filter out momentary spikes.
- **Min/Max Stats**: An optional toggle to display the session minimum and maximum values alongside your FPS and Ping.
- **FPS Graph**: A live performance graph rendered directly on the HUD that samples at 100ms intervals—up to 600 data points—so you can visualize your frame rate history over time.

### Rendering & UI Refinement
- **Matrix Scaling**: The HUD now scales perfectly without any pixel distortion or blurriness, thanks to the new `Matrix3x2fStack` rendering implementation.
- **Configurable HUD Scale**: A smooth slider from 20% to 150% so you can dial in the exact size you want, down to 5% increments.
- **Two Overlay Styles**: Choose between the horizontal Navbar layout (metrics flow inline with dividers) or the classic Default vertical stack.
- **Adaptive Performance Colors**: Your FPS, MSPT, Ping, Memory, CPU, GPU, and more will now automatically shift between green → yellow → red in real-time based on configurable good/warning/bad thresholds, helping you spot lag spikes instantly.
- **Text Effects**: Pick between None, Shadow, or Outline text rendering to make the overlay readable on any background.
- **Rounded Background**: The HUD background uses pixel-perfect rounded corners for a modern, polished look.
- **Visual Branding**: The mod finally has a polished icon that shows off exactly what the HUD looks like.

### Configuration & Customization
- **Config Hub Screen**: A central hub that branches into Settings, Position Editor, and Metric Arrangement—everything's one click away.
- **YACL Integration**: The settings menu is now powered by YetAnotherConfigLib v3.8.1, giving you sliders, color pickers, enum cycling, and tick boxes with a cohesive, premium feel.
- **Full Color Customization**: Every color is individually tweakable with YACL's alpha-enabled color picker—background, label, value, unit, divider, and the good/warning/bad adaptive colors.
- **Theme Presets**: Instantly switch between Classic Dark, Light, Glass, or go fully Custom and design your own palette from scratch.
- **Drag-to-Position Editor**: A dedicated screen where you can click and drag the overlay preview anywhere on your screen, with a position cycle button and a reset offset shortcut.
- **Metric Order Screen**: A full drag-and-drop management screen where you can reorder every metric, toggle visibility, rename labels with custom display names, and reset everything to defaults—all with scrolling, column headers, toast notifications, and accent bracket decorations.
- **Custom Metric Labels**: Rename any metric's display label (e.g., change "FPS" to "Frames") and see a live preview of your alias right in the arrangement screen.
- **Configurable Update Interval**: Fine-tune how often the HUD refreshes—from 16ms (every frame) up to 1000ms (once per second)—with named presets for common values.
- **X/Y Offset Sliders**: Nudge the overlay position pixel-by-pixel with ±1000px sliders, independent of the anchor position.
- **Background Opacity Slider**: Dial the background transparency from fully transparent to fully opaque, with a percentage readout.
- **Session Statistics Reset**: A keybind-able action to clear your Avg FPS, 1% Low, Min/Max, and graph data on the fly.

### Keybindings
- **10 Configurable Keybinds**: Toggle Overlay (O), Toggle FPS (F8), Toggle Frame Time (F9), Toggle Memory (F10), Toggle Ping (F11), Toggle Coords (F7), Toggle Graph (F5), Open Config (P), Open Position Editor (F6), and Reset Stats (F4)—all rebindable in Controls.
- **Keybinding Master Toggle**: A config option to completely enable or disable all keybindings at once, so they never conflict with other mods.

### Fixes & Integration
- **NeoForge Config Button**: You can finally click the "Config" button directly from the NeoForge mod list—no more digging through folders.
- **ModMenu Integration**: On Fabric, the mod integrates with ModMenu so the settings screen is always one click away from the mod list.
- **F3 Compatibility**: There's a handy toggle to automatically hide the overlay whenever you open the F3 debug screen, keeping your display from getting cluttered with text.
- **Navbar Row Wrapping**: If your enabled metrics exceed the screen width in Navbar mode, the overlay automatically wraps to multiple rows instead of clipping off-screen.
- **Config Validation & Migration**: The config file is fully validated on load—any corrupted, missing, or out-of-range values are silently repaired to safe defaults without losing the rest of your settings.