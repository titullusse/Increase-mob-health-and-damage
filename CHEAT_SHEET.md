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
| Fichiers Java | 3 |
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
└── events/MobAttributeHandler.java

src/main/resources/
├── META-INF/neoforge.mods.toml   # NeoForge 1.21+ : PAS mods.toml
└── pack.mcmeta                    # pack_format 34
```

---

## ⚙️ Config

`config/mobhealthmodifier-common.toml`

```toml
[general]
	enableHealthModification = true
	healthMultiplier = 1.0     # 0.1 – 10.0
	enableDamageModification = true
	damageMultiplier = 1.0     # 0.1 – 10.0
```

Presets : facile `0.5/0.5` · normal `1.0/1.0` · difficile `2.0/2.0` · hardcore `3.0/3.0`

---

## 🎯 Les 3 classes

### MobHealthModifier
```java
@Mod(MobHealthModifier.MOD_ID)
public MobHealthModifier(IEventBus modEventBus, ModContainer modContainer) {
    MobHealthModifierConfig.register(modContainer);
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
Applique les multiplicateurs. Garde-fous : côté client ignoré, non-`Mob` ignorés, mobs déjà traités
ignorés via un marqueur NBT persistant.

---

## 🔄 Flow

```
Startup → register() → TOML généré
Mob spawn → EntityJoinLevelEvent → applyModifications(mob)
          → MAX_HEALTH ×= healthMultiplier (+ setHealth(getMaxHealth()))
          → ATTACK_DAMAGE ×= damageMultiplier
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

// Mod
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
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
- Aucune distinction par type de mob
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
- [ ] Logs sans erreur
