### Notes
- This mod currently only adds three effects; there are no enchantments, weapons, or items to trigger them yet.
- No potions either (you can add your own via KubeJS if you know how; I plan to add them later).
- Uploaded as-is for now. ψ(｀∇´)ψ
- Of course [I&F: RLCraft Edition]（ https://www.curseforge.com/minecraft/mc-mods/iceandfire-rlcraft ）The electric dragon weapon inside is so powerful!

### Fire Dragon
- Effect: `fire_attack`
- Attack: ignites the target (5s × multiplier) + Blaze mark + knockback
- If the target is Frozen, it will not be ignited (ice beats fire).

### Ice Dragon
- Effect: `ice_attack`
- Effect: Freeze mark + Slowness III + Mining Fatigue III (10s × multiplier) + extinguish + shatter ice

### Lightning Dragon
- Effect: `lightning_attack`
- Effect: Chain Lightning (center + nearby targets, AOE) + Shock stun 3s
- Lightning damage mode: defaults to "magic lightning"; can be set to "vanilla lightning".

### Elemental Reactions

The three base statuses (`frozen` / `blaze` / `shocked`, fixed 3s) combine in pairs to trigger **Elemental Reactions**, dealing extra magic damage.

### Reaction Combinations

| Combo | Reaction |
|---|---|
| Ice + Fire | Melt |
| Ice + Lightning | Superconduct |
| Fire + Lightning | Overload |

Trigger rule: **when an attacker applies an element, it checks the other two elements already on the target.** So when the target has multiple element marks, one attack can trigger multiple reactions at once.

| Attack | Applies | Triggers |
|---|---|---|
| Fire Dragon | `blaze` | `frozen` → Melt, `shocked` → Overload |
| Ice Dragon | `frozen` | `blaze` → Melt, `shocked` → Superconduct |
| Lightning Dragon | `shocked` | `frozen` → Superconduct, `blaze` → Overload |

### Damage Calculation

Single reaction damage = base damage `10.0` × (applied element level × existing element level × 0.1), levels start at 1 (level I = 1).

| Level Combo | Multiplier | Single Reaction Damage | Hearts |
|---|---|---|---|
| 1×1 | 10% | 1.0 | 0.5 heart |
| 1×2 / 2×1 | 20% | 2.0 | 1 heart |
| 2×2 | 40% | 4.0 | 2 hearts |
| 2×3 / 3×2 | 60% | 6.0 | 3 hearts |
| 3×3 (default max) | 90% | 9.0 | 4.5 hearts |

- **Damage merge**: multiple reactions triggered by the same attack stack into a single hit.
- **Reaction mark**: after triggering, the target briefly gets a mark effect (`melt` / `superconduct` / `overload`) that does not chain into the next reaction.

### Levels & Multipliers

Above the configured max level, the effect is capped (default max 3).

**Multiplier formula** (shared):

```
multiplier = 1.0 + min(level, maxEffectiveLevel) × 0.25
```

### Per-Level Effects (default max 3)

| Level | Multiplier | Fire Ignite | Fire Knockback | Ice Duration | Lightning Center Dmg | Lightning Chain Dmg |
|---|---|---|---|---|---|---|
| 0 (I) | ×1.00 | 5s | 1.0 | 10s | 5.0 | 4.0 |
| 1 (II) | ×1.25 | 6.25s | 1.25 | 12.5s | 6.25 | 5.0 |
| 2 (III) | ×1.50 | 7.5s | 1.50 | 15s | 7.5 | 6.0 |
| 3 (IV, default max) | ×1.75 | 8.75s | 1.75 | 17.5s | 8.75 | 7.0 |

### Lightning Chain Growth (separate from multiplier)

```
searchRadius = min(8 + level × 2, 14)
targetCount  = min(6 + level × 1, 9)
```

Chain damage decreases per jump (−12.5% each, minimum 10%).

### Config

Config file: `config/if_dragon_blessings-common.toml`

| Option | Description | Default |
|---|---|---|
| `elementalReactions.enabled` | Master toggle for elemental reactions | `true` |
| `dragonAttacks.fireAttackMaxLevel` | Fire Dragon max effective level | `3` |
| `dragonAttacks.iceAttackMaxLevel` | Ice Dragon max effective level | `3` |
| `chainLightning.lightningAttackMaxLevel` | Lightning Dragon max effective level | `3` |
| `chainLightning.useVanillaLightning` | Lightning damage mode (`false`=magic / `true`=vanilla) | `false` |
| `chainLightning.centerDamage` | Chain Lightning center target damage | `5.0` |
| `chainLightning.chainDamageBase` | Chain Lightning base chain damage | `4.0` |

### Usage

Get the effect via command, then attack a target:

```
/effect give @s if_dragon_blessings:fire_attack 60 0
/effect give @s if_dragon_blessings:ice_attack 60 0
/effect give @s if_dragon_blessings:lightning_attack 60 0
```

All three effects can be held at once and trigger simultaneously without conflict.

### Planned Features
- Enchantments (maybe)
- Potions
- More trigger methods
- More config options

### FAQ
- Custom weapons? Probably not.
