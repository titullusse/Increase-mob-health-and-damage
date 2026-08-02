# 🚀 Cheat Sheet — Mob Health Modifier

Aide-mémoire du code et des commandes.

---

## 📦 Quick facts

| | |
|---|---|
| Mod | Mob Health Modifier 1.0.0 |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.0+ |
| Java | 21+ |
| Build | ModDevGradle 2.0.143 |
| Fichiers Java | 9 (5 communs + 4 client) |
| Premier build | 5–15 min |

---

## 🔧 Commandes

```bash
./gradlew build         # compile le mod
./gradlew clean build   # rebuild complet
./gradlew runClient     # client de dev
./gradlew runServer     # serveur de dev (EULA requis)
```

JAR : `build/libs/mobhealthmodifier-1.0.0.jar`

---

## 📁 Structure

```
src/main/java/com/marc33/mobhealth/
├── MobHealthModifier.java
├── config/MobHealthModifierConfig.java
├── config/MobTarget.java
├── config/MobHealthModifierServerConfig.java   # SERVER, synchronisée au client
├── events/MobAttributeHandler.java
└── client/                          # @EventBusSubscriber(value = Dist.CLIENT)
    ├── MobHealthModifierClientConfig.java
    ├── DisplayPolicy.java           # arbitre serveur vs client
    ├── HealthBarRenderer.java
    └── NameTagHandler.java

src/main/resources/
├── META-INF/neoforge.mods.toml   # NeoForge 1.21+ : PAS mods.toml
└── pack.mcmeta                    # pack_format 34
```

---

## ⚙️ Config

`config/mobhealthmodifier-common.toml`

```toml
[general]
	affectedMobs = "HOSTILE"   # ALL | HOSTILE | PASSIVE
	enableHealthModification = true
	healthMultiplier = 1.0     # 0.1 – 10.0
	enableDamageModification = true
	damageMultiplier = 1.0     # 0.1 – 10.0
```

Presets : facile `0.5/0.5` · normal `1.0/1.0` · difficile `2.0/2.0` · hardcore `3.0/3.0`

Hostile = `mob instanceof Enemy || getType().getCategory() == MobCategory.MONSTER`

`config/mobhealthmodifier-client.toml` (affichage, local)

```toml
[display]
	enableHealthBars = true
	showOnHostileMobs = true
	showOnPassiveMobs = false
	showOnPlayers = true
	hidePlayerNameTags = true    # masque les pseudos
	maxRenderDistance = 24.0     # 4 – 64 blocs
	barWidth = 40                # 8 – 120 px
	barHeight = 5                # 1 – 20 px
	verticalOffset = 0.0         # -2.0 – 2.0 blocs
```

`<monde>/serverconfig/mobhealthmodifier-server.toml` — ⚠️ pas dans `config/` !

```toml
[display]
	enforceHealthBarSettings = false   # true → le serveur décide des barres
	healthBarsEnabled = true
	showOnHostileMobs = true
	showOnPassiveMobs = false
	showOnPlayers = true
	maxRenderDistance = 24.0

	enforceNameTagSettings = false     # true → le serveur décide des pseudos
	hidePlayerNameTags = true
```

NeoForge synchronise seul toute config `SERVER` vers les clients à la connexion.
Jamais imposés : `barWidth`, `barHeight`, `verticalOffset`.

---

## 🎯 Les classes

### MobHealthModifier
```java
@Mod(MobHealthModifier.MOD_ID)
public MobHealthModifier(IEventBus modEventBus, ModContainer modContainer) {
    MobHealthModifierConfig.register(modContainer);        // COMMON
    MobHealthModifierServerConfig.register(modContainer);  // SERVER, synchronisée au client
    MobHealthModifierClientConfig.register(modContainer);  // CLIENT, ignoré sur serveur dédié
}
```
Point d'entrée. Le constructeur reçoit le `ModContainer`, indispensable pour enregistrer la config.

### MobHealthModifierConfig
```java
private static final Pair<Common, ModConfigSpec> PAIR =
        new ModConfigSpec.Builder().configure(Common::new);

container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
```
Structure TOML, bornes, getters. Les getters testent `COMMON_SPEC.isLoaded()` pour ne pas lever
d'exception si un évènement arrive avant le chargement du fichier.

### MobAttributeHandler
```java
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID)   // bus GAME par défaut en 21.1
@SubscribeEvent
public static void onEntityJoinWorld(EntityJoinLevelEvent event)
```
Applique les multiplicateurs. Garde-fous : côté client ignoré, non-`Mob` ignorés, mobs hors ciblage
ignorés, mobs déjà traités ignorés via un marqueur NBT persistant.

### MobTarget
```java
public enum MobTarget { ALL, HOSTILE, PASSIVE }   // abstract boolean matches(Mob)
```
Lu depuis le TOML avec `builder.defineEnum("affectedMobs", MobTarget.HOSTILE)`.

### HealthBarRenderer (client)
```java
@SubscribeEvent
public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event)
```
Post est émis après `popPose()` : la pile est à l'origine de l'entité, comme pour la plaque de nom.
Billboard : `translate(anchor)` → `mulPose(cameraOrientation())` → `scale(0.025F, -0.025F, 0.025F)`.
⚠️ L'axe Y est inversé par ce scale : les valeurs négatives montent.

Quads via `RenderType.debugQuads()` — POSITION_COLOR, translucide, **NO_CULL**, sans lightmap.
Pas de `setLight()` avec ce format, seulement `addVertex(matrix, x, y, z).setColor(argb)`.

### DisplayPolicy (client)
```java
return ServerConfig.enforcesNameTagSettings()
        ? ServerConfig.hidePlayerNameTags()
        : ClientConfig.hidePlayerNameTags();
```
Seul point d'arbitrage serveur/client. Le rendu ne lit **que** cette classe, jamais les configs
directement.

### NameTagHandler (client)
```java
@SubscribeEvent
public static void onRenderNameTag(RenderNameTagEvent event) {
    if (event.getEntity() instanceof Player) event.setCanRender(TriState.FALSE);
}
```

---

## 🔄 Flow

```
Startup → register() ×3 → les trois TOML sont générés

SERVEUR
Mob spawn → EntityJoinLevelEvent → applyModifications(mob)
          → affectedMobs.matches(mob) ? sinon on sort sans marquer
          → MAX_HEALTH ×= healthMultiplier (+ setHealth(getMaxHealth()))
          → ATTACK_DAMAGE ×= damageMultiplier

CLIENT
Connexion     → NeoForge synchronise mobhealthmodifier-server.toml
Rendu entité  → RenderLivingEvent.Post → DisplayPolicy → HealthBarRenderer → quads billboardés
Rendu pseudo  → RenderNameTagEvent     → DisplayPolicy → NameTagHandler   → setCanRender(FALSE)
```

---

## 📝 Imports essentiels

```java
// Config
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

// Events
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

// Entités
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

// Mod
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

// Rendu (client)
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityAttachment;
import org.joml.Matrix4f;
```

⚠️ En 1.21.1 les `Attributes.X` sont des `Holder<Attribute>`, pas des `Attribute`.

---

## ✏️ Ajouter un attribut (ex. vitesse)

**1. `MobHealthModifierConfig.Common`**
```java
public final ModConfigSpec.DoubleValue speedMultiplier;

speedMultiplier = builder
        .comment("Multiplicateur de vitesse de deplacement.")
        .defineInRange("speedMultiplier", 1.0D, MIN_MULTIPLIER, MAX_MULTIPLIER);
```

**2. Getter**
```java
public static double getSpeedMultiplier() {
    return isLoaded() ? COMMON.speedMultiplier.get() : 1.0D;
}
```

**3. `MobAttributeHandler.applyModifications()`**
```java
AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
if (speed != null) {
    speed.setBaseValue(speed.getBaseValue() * MobHealthModifierConfig.getSpeedMultiplier());
}
```

---

## ❌ Limitations

- Mobs déjà présents non modifiés (nouveaux spawns uniquement)
- Un mob garde à vie les valeurs reçues à son apparition
- Redémarrage requis après édition du TOML
- Pas de commande ni de GUI
- Ciblage par catégorie uniquement, pas par type précis de mob
- Barres de vie = côté client obligatoire (le serveur seul ne suffit pas)
- Barres occultées par les blocs, sans valeur chiffrée
- Config SERVER par monde : le fichier vit dans `<monde>/serverconfig/`, pas dans `config/`
- `ATTACK_DAMAGE` absent chez les creepers et les mobs à distance

---

## 🔍 Debug

```bash
grep -i "mobhealthmodifier" logs/latest.log
cat config/mobhealthmodifier-common.toml
```

---

## ✅ Checklist déploiement

- [ ] `./gradlew build` OK
- [ ] JAR dans `build/libs/`
- [ ] JAR copié dans `mods/`
- [ ] Jeu/serveur redémarré une fois → TOML créé
- [ ] Config éditée, redémarrage
- [ ] Nouveau mob spawné et testé
- [ ] Barre de vie visible (JAR aussi côté client !)
- [ ] Logs sans erreur
