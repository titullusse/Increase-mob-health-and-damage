# 📋 Synthèse — Mob Health Modifier

Mod NeoForge 1.21.1 qui multiplie la **vie** et les **dégâts** des mobs — hostiles par défaut,
passifs ou tous au choix — via un fichier de configuration TOML.

---

## 📁 Structure

```
Increase-mob-health-and-damage/
├── build.gradle              # ModDevGradle 2.0.143
├── settings.gradle
├── gradle.properties         # versions MC / NeoForge / mod
├── gradlew, gradle/wrapper/  # wrapper Gradle 8.14.3
│
├── src/main/java/com/marc33/mobhealth/
│   ├── MobHealthModifier.java
│   ├── config/MobHealthModifierConfig.java
│   ├── config/MobTarget.java
│   └── events/MobAttributeHandler.java
│
└── src/main/resources/
    ├── META-INF/neoforge.mods.toml
    └── pack.mcmeta
```

---

## 🔧 Architecture

### 1. `MobHealthModifier.java` — point d'entrée

```java
@Mod(MobHealthModifier.MOD_ID)
public class MobHealthModifier {
    public static final String MOD_ID = "mobhealthmodifier";

    public MobHealthModifier(IEventBus modEventBus, ModContainer modContainer) {
        MobHealthModifierConfig.register(modContainer);
    }
}
```

En NeoForge 1.21, le constructeur du mod peut demander les services dont il a besoin par injection :
`IEventBus` pour le bus du mod, `ModContainer` pour enregistrer la configuration.

### 2. `MobHealthModifierConfig.java` — configuration

```java
private static final Pair<Common, ModConfigSpec> PAIR =
        new ModConfigSpec.Builder().configure(Common::new);
public static final Common COMMON = PAIR.getLeft();
public static final ModConfigSpec COMMON_SPEC = PAIR.getRight();

public static void register(ModContainer container) {
    container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
}
```

Rôles :
- décrire la structure du fichier `config/mobhealthmodifier-common.toml` ;
- déclarer les bornes (`defineInRange`, 0.1 – 10.0) que NeoForge applique lui-même ;
- exposer des getters qui vérifient `isLoaded()` avant de lire, afin de renvoyer une valeur neutre
  plutôt que de lever une exception si un évènement arrive avant le chargement du fichier.

### 3. `MobTarget.java` — ciblage

```java
public enum MobTarget {
    ALL     { public boolean matches(Mob mob) { return true; } },
    HOSTILE { public boolean matches(Mob mob) { return isHostile(mob); } },
    PASSIVE { public boolean matches(Mob mob) { return !isHostile(mob); } };

    private static boolean isHostile(Mob mob) {
        return mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER;
    }
}
```

Lu depuis le TOML via `builder.defineEnum("affectedMobs", MobTarget.HOSTILE)` ; NeoForge écrit
lui-même la liste des valeurs acceptées en commentaire et refuse toute autre valeur.

Le test d'hostilité combine deux critères :

- **`Enemy`** est le marqueur vanilla de l'hostilité. Il est porté par `Monster`, donc par toute la
  hiérarchie habituelle (zombie, squelette, creeper, araignée, enderman, piglin, wither, warden…),
  mais aussi, hors de cette hiérarchie, par `Slime`, `Ghast`, `Phantom`, `Shulker`, `Hoglin`,
  `Zoglin` et `EnderDragon` — d'où la nécessité de tester l'interface plutôt que la classe
  `Monster`.
- **`MobCategory.MONSTER`** est la catégorie de spawn. Elle sert de filet de sécurité pour les mobs
  ajoutés par d'autres mods, qui déclarent parfois leur hostilité par la catégorie sans implémenter
  `Enemy`.

### 4. `MobAttributeHandler.java` — logique de jeu

```java
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID)
public final class MobAttributeHandler {
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) { ... }
}
```

Le paramètre `bus` de `@EventBusSubscriber` est déprécié en 21.1 : le bus « game » est le défaut, il
ne faut plus le préciser.

Quatre filtres avant modification :

1. **Côté client ignoré** — les attributs sont autoritaires côté serveur puis synchronisés.
2. **Non-`Mob` ignorés** — écarte les joueurs, projectiles, items au sol, etc.
3. **Mobs hors ciblage ignorés** — selon `affectedMobs`. Ce filtre passe **avant** l'écriture du
   marqueur : un mob non ciblé repart totalement intact et sera réexaminé si la configuration
   change plus tard.
4. **Mobs déjà traités ignorés** — voir ci-dessous.

---

## ⚠️ Le piège d'`EntityJoinLevelEvent`

L'évènement ne se déclenche pas seulement au spawn : il se déclenche **à chaque fois qu'une entité
est ajoutée au monde**, donc aussi au rechargement d'un chunk déjà visité.

Une implémentation naïve multiplierait donc la santé d'un même zombie à chaque aller-retour du
joueur : avec `healthMultiplier = 2.0`, 20 → 40 → 80 → 160 HP.

Le mod écrit un marqueur booléen dans `mob.getPersistentData()`, sauvegardé avec l'entité, et sort
immédiatement si ce marqueur est présent. La modification est donc appliquée exactement une fois par
mob, pour toute sa durée de vie.

Conséquence assumée : changer la configuration n'affecte pas les mobs déjà marqués.

---

## ⚙️ Configuration

`config/mobhealthmodifier-common.toml`

```toml
[general]
	affectedMobs = "HOSTILE"      # ALL | HOSTILE | PASSIVE
	enableHealthModification = true
	healthMultiplier = 1.0        # 0.1 – 10.0
	enableDamageModification = true
	damageMultiplier = 1.0        # 0.1 – 10.0
```

---

## 🔄 Flow d'exécution

```
1. DÉMARRAGE
   MobHealthModifier(IEventBus, ModContainer)
   └─ MobHealthModifierConfig.register(container)
      └─ NeoForge crée/charge config/mobhealthmodifier-common.toml

2. CONFIGURATION
   L'utilisateur édite le TOML, puis redémarre

3. UN MOB ARRIVE DANS LE MONDE
   EntityJoinLevelEvent
   └─ côté serveur ? instanceof Mob ? cible de affectedMobs ? pas déjà marqué ?
      └─ applyModifications(mob)

4. MODIFICATION
   MAX_HEALTH    : base ×= healthMultiplier, puis setHealth(getMaxHealth())
   ATTACK_DAMAGE : base ×= damageMultiplier  (ignoré si l'attribut est absent)
```

`setBaseValue` ne soigne pas l'entité : sans le `setHealth` qui suit, un zombie passé de 20 à 40 HP
apparaîtrait à moitié blessé.

---

## 🎮 Comportement

**Fait :**
- multiplie santé et dégâts des mobs qui apparaissent, hostiles par défaut ;
- cible les hostiles, les passifs ou tous, au choix (`affectedMobs`) ;
- chaque modificateur s'active/se désactive indépendamment ;
- fonctionne en solo comme en serveur, sans mod côté client.

**Ne fait pas :**
- modifier les mobs déjà présents avant l'installation ;
- appliquer une nouvelle config aux mobs existants ;
- recharger la config à chaud, ni fournir commande ou GUI ;
- distinguer les types précis de mobs : le ciblage se fait par catégorie, pas zombie par zombie.

**Exemple** — `healthMultiplier = 2.0`, `damageMultiplier = 1.5` :

| Mob | Santé | Dégâts |
|-----|-------|--------|
| Zombie | 20 → 40 | 3 → 4.5 |
| Enderman | 40 → 80 | 7 → 10.5 |
| Creeper | 20 → 40 | explosion, non modifiée |
| Squelette | 20 → 40 | flèche, non modifiée |

Les creepers et les mobs à distance ne possèdent pas l'attribut `ATTACK_DAMAGE` : leurs dégâts sont
portés par l'explosion ou le projectile. `getAttribute` renvoie `null` et l'attribut est ignoré.

---

## 📦 Dépendances

- Minecraft 1.21.1 · NeoForge 21.1.0+ (compilé avec 21.1.248) · Java 21+
- Build : plugin `net.neoforged.moddev` 2.0.143 (ModDevGradle), Gradle 8.14.3

```gradle
plugins {
    id 'net.neoforged.moddev' version '2.0.143'
}

neoForge {
    version = project.neo_version
}
```

Les versions vivent dans `gradle.properties` et sont injectées dans `neoforge.mods.toml` par la
tâche `processResources`.

---

## 📊 Statistiques

| Élément | Détail |
|---------|--------|
| Fichiers Java | 3 |
| Classes | 5 (Main, Config, Config.Common, MobTarget, Handler) |
| Évènements écoutés | 1 (`EntityJoinLevelEvent`) |
| Attributs modifiés | 2 (`MAX_HEALTH`, `ATTACK_DAMAGE`) |
| Paramètres de config | 5 (1 enum, 2 booléens, 2 doubles) |

---

## ❓ FAQ

**Où modifier la logique ?** Dans `MobAttributeHandler.applyModifications()`.

**Comment ajouter un attribut ?** Voir la section dédiée de [`CHEAT_SHEET.md`](CHEAT_SHEET.md).

**Comment appliquer la config sans redémarrage ?** Écouter `ModConfigEvent.Reloading`, et parcourir
les entités chargées de chaque `ServerLevel` en réinitialisant leurs attributs — le marqueur NBT
devrait alors stocker le multiplicateur appliqué plutôt qu'un simple booléen, afin de savoir quoi
recalculer.

**Peut-on toucher aussi aux joueurs et aux animaux ?** Remplacer `instanceof Mob` par
`instanceof LivingEntity`. Attention : cela inclut les joueurs.

**Pourquoi `neoforge.mods.toml` et non `mods.toml` ?** Depuis Minecraft 1.20.5, NeoForge lit
`META-INF/neoforge.mods.toml`. L'ancien nom n'est plus reconnu.

---

## 🎯 Pistes d'évolution

1. Multiplicateurs par type précis de mob (`zombieHealthMultiplier`, …)
2. Autres attributs : vitesse, portée d'attaque, résistance au recul
3. Commande `/mobmodify health 2.0` avec rechargement à chaud
4. Écran de configuration in-game
