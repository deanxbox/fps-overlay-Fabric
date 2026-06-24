## FPS Overlay v5.2

### New
* **Hide metric labels** New toggle in General HUD settings: "Show Metric Labels." Turn it off and your HUD shows only the numbers and units (like `100fps | 10ms`), with no text clutter. Great for a minimal look.

### Performance fixes
* **Fixed the FPS drop caused by the graph** This was the main bug from last version. The graph was drawing each bar one pixel at a time, which hammered the GPU. It now draws each bar in a single operation, cutting GPU work by up to 50x.
* **Graph overhead when hidden is now zero** Previously the game was still calculating graph data even when the graph wasn't visible. That's fixed — it skips all graph processing when you have it turned off.
* **Internal Cleanups** A couple of smaller internal cleanups to reduce unnecessary memory usage during startup.
