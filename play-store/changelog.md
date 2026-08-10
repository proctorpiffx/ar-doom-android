# AR Doom — Changelog

## v0.1.0-alpha (August 2026)

### Initial Release

**Features:**
- ARCore 6-DOF tracking — walk around your real space to play
- Wave-based survival mode with escalating difficulty (50 waves)
- 5 enemy types: Soldier, Imp, Demon, Cacodemon, Baron of Hell
- 5 weapons: Pistol, Shotgun, Chaingun, Plasma Rifle, BFG
- Enemy AI with state machine: Idle → Chase → Attack → Hurt → Dying → Dead
- Enemies spawn on detected surfaces and physically chase the player
- Touch controls: tap to fire, swipe to switch weapons, swipe down for ammo
- Haptic feedback: fire, hit, damage, weapon switch
- DOOM-style HUD: health (color-coded), ammo, score, wave counter, weapon name
- Wave announcement overlays
- Billboarded sprite rendering with placeholder textures (replaceable with DOOM art)
- OpenGL ES 3.0 rendering pipeline with 3 shaders
- Environmental HDR lighting estimation from ARCore
- Depth-assisted surface detection
- Landscape-only fullscreen experience
- No ads, no tracking, no network required

**Known limitations:**
- Placeholder textures (colored quads) — not real DOOM sprites yet
- No sound effects bundled (audio system ready, needs .wav files)
- No app icon graphics (using default)
- Single game mode (survival) — no campaign/story
- No save/load between sessions
- No leaderboard or social features
