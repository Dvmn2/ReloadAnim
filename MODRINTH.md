# ReloadAnim

Client mod that replaces the red resource reload screen with an animation taken from your resource packs.

## Features
- Shows a looping animation instead of the red Mojang screen, both at game start and on resource reloads.
- The animation comes from any resource pack you use; the pack with the highest priority wins.
- Keeps the vanilla fade-in and fade-out.
- Emergency switch, intensity limit and a no-motion mode.

## Requirements
- Minecraft 1.21.11, Fabric Loader, Fabric API
- Works on any server; the mod never connects to servers

## Installation
1. Install Fabric Loader for Minecraft 1.21.11.
2. Put `ReloadAnim-<version>.jar` and Fabric API into the `mods` folder.
3. Enable a resource pack that contains a loading animation (see below) and reload resources with F3+T.
4. The animation appears on the next loading screen.

### Making a pack with an animation
Add these files to your resource pack:

```text
assets/reloadanim/textures/gui/loading.png
assets/reloadanim/textures/gui/loading.png.mcmeta   (optional)
```

`loading.png` is a vertical strip: all frames stacked on top of each other, each frame as wide as the image.
The optional `loading.png.mcmeta` uses the same format as vanilla animated textures:

```json
{
  "animation": {
    "frametime": 2,
    "frames": [0, 1, 2, 3]
  }
}
```

`frametime` is in ticks (1 tick = 50 ms). Without a `.mcmeta` file frames are square and play in order at one tick per frame.
Limits: up to 512 px wide, up to 16384 px tall, up to 256 frames, file size up to 16 MiB.

## Commands
| Command | Description |
|---|---|
| `/ra off` | Disable all mod effects immediately |
| `/ra on` | Enable the mod again |
| `/ra status` | Show the current state |
| `/ra reload` | Re-read the configuration file |

## Configuration
File: `config/reloadanim.json`

| Key | Type | Default | Range | Description |
|---|---|---|---|---|
| `enabled` | boolean | `true` | `true`, `false` | Master switch; `false` brings back the vanilla screen |
| `allow-pack-animations` | boolean | `true` | `true`, `false` | Allow reading animations from your resource packs |
| `intensity` | number | `1.0` | `0.0..1.0` | Animation strength over the black background |
| `reduced-motion` | boolean | `false` | `true`, `false` | Show one still frame instead of playing the animation |

```json
{
  "enabled": true,
  "allow-pack-animations": true,
  "intensity": 1.0,
  "reduced-motion": false
}
```

## Controls
None.

## Privacy and safety
- The mod reads one image and one small text file from your resource packs and keeps a copy in `reloadanim/cache` inside your game folder.
- Nothing is sent over the network.
- Reading packs is on by default because this is the purpose of the mod. Set `allow-pack-animations` to `false` to stop it; the copy in the cache is deleted on the next reload.
- If an animation flashes too much, lower `intensity` or turn on `reduced-motion`.
- If anything looks wrong, type `/ra off`. If you cannot open the game, set `enabled` to `false` in the configuration file.

## Troubleshooting
- **The vanilla screen is still shown after installing a pack.** The animation is shown from the next loading screen on: reload resources once more with F3+T.
- **The first launch after installing the mod shows the red screen.** No animation has been saved yet; it appears once you have loaded the game with a suitable pack.
- **The animation does not appear.** Run `/ra status`. Check the file path and the limits listed above; problems are written to the game log.
- **The animation disappeared after an error.** The mod switches itself off for the session if drawing fails; `/ra on` tries again.

## License
All rights reserved.
