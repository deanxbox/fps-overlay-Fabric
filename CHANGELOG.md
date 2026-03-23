# Changelog

## v5.0

### Evolution & Multi-loader
- **Migrated to Architectury**: I've completely rewritten the mod so it now runs natively on both Fabric and NeoForge from the exact same codebase.
- **Minecraft 1.21.11**: Fully up to date and ready for the latest version of Minecraft.
- **Mojang Mappings**: Switched over to official Mojang mappings behind the scenes, which will make maintaining and updating the mod much easier going forward.

### Rendering & UI Refinement
- **Matrix Scaling**: The HUD now scales perfectly without any pixel distortion or blurriness, thanks to the new rendering implementation.
- **Simplified HUD Scale**: I ditched the granular slider in favor of three carefully dialed-in presets: Small (0.65x), Normal (0.8x), and Big (0.95x).
- **New Default Layout**: Fresh installs will automatically use the Top Left position at the Small scale to give you a much cleaner, minimalist vibe right out of the box.
- **Adaptive Performance Colors**: Your FPS, MSPT, and Memory stats will now automatically shift colors in real-time if performance drops, helping you spot lag spikes instantly.
- **Visual Branding**: The mod finally has a polished icon that shows off exactly what the HUD looks like.

### Fixes & Integration
- **NeoForge Config**: You can finally click the "Config" button directly from the NeoForge mod list—no more digging through folders.
- **Cloth Config**: The settings menu now integrates deeply with Cloth Config, giving everything a cohesive and premium feel.
- **F3 Compatibility**: There's a handy new toggle to automatically hide the overlay whenever you open the F3 debug screen, keeping your display from getting cluttered with text.