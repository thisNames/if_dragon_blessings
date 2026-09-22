# If Dragon Blessings（龙之祝福）

一个 Minecraft Forge 1.20.1 模组，为玩家提供三种「龙的祝福」攻击方式。

攻击方式与物品无关：持有对应祝福效果（buff）后，攻击命中目标即可触发对应的龙之攻击特效。

## 功能

### 火龙（Fire Dragon）

- **祝福效果**：`fire_attack`（火龙的祝福）
- **攻击特效**：点燃目标 5 秒 + 烈焰标记 5 秒 + 击退

### 冰龙（Ice Dragon）

- **祝福效果**：`ice_attack`（冰龙的恩惠）
- **攻击特效**：冰封 + 缓慢 III + 挖掘疲劳 III（10 秒）+ 冰块装饰渲染 + 消失时碎冰音效与粒子

### 电龙（Lightning Dragon）

- **祝福效果**：`lightning_attack`（电龙的庇护）
- **攻击特效**：闪电链（多目标伤害传导）+ 感电定身 3 秒

## 效果分类

| 类型 | 效果 | 说明 |
|---|---|---|
| 攻击 buff（正面） | `fire_attack` / `ice_attack` / `lightning_attack` | 施加给攻击者，攻击时触发对应攻击方式 |
| 状态效果（负面） | `frozen`（冰封）/ `blaze`（烈焰）/ `shocked`（感电） | 施加给被击中的目标 |

## 环境要求

- Minecraft `1.20.1`
- Forge `47.4.10+`
- **零第三方依赖**（仅依赖 Forge 与原版）

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/` 目录。

## 使用

通过命令给自己附加祝福效果（创造模式下），然后攻击目标即可：

```
/effect give @s if_dragon_blessings:fire_attack 60 0
/effect give @s if_dragon_blessings:ice_attack 60 0
/effect give @s if_dragon_blessings:lightning_attack 60 0
```

三种祝福可同时持有，攻击时会同时触发对应特效，互不冲突。

## 许可

LGPL
