# 📋 Synthèse — Mob Health Modifier

Mod NeoForge 1.21.1 en deux moitiés : un volet **serveur** qui multiplie la vie et les dégâts des
mobs, et un volet **client** qui affiche une barre de vie au-dessus des mobs et des joueurs. Chacune
fonctionne sans l'autre — mais le serveur peut imposer ses réglages d'affichage aux clients.

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
│   ├── config/MobHealthModifierServerConfig.java
│   ├── events/MobAttributeHandler.java
│   └── client/
│       ├── MobHealthModifierClientConfig.java
│       ├── DisplayPolicy.java
│       ├── HealthBarRenderer.java
│       └── NameTagHandler.java
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
        MobHealthModifierConfig.register(modContainer);        // ModConfig.Type.COMMON
        MobHealthModifierServerConfig.register(modContainer);  // ModConfig.Type.SERVER
        MobHealthModifierClientConfig.register(modContainer);  // ModConfig.Type.CLIENT
    }
}
```

En NeoForge 1.21, le constructeur du mod peut demander les services dont il a besoin par injection :
`IEventBus` pour le bus du mod, `ModContainer` pour enregistrer la configuration.

Une config de type `CLIENT` n'est chargée que sur un client : l'enregistrer inconditionnellement
depuis le constructeur commun est sans effet sur un serveur dédié. Seules les classes de rendu, qui
touchent réellement à des classes client, sont marquées `@EventBusSubscriber(value = Dist.CLIENT)`
pour ne jamais être chargées côté serveur.

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

### 5. `client/HealthBarRenderer.java` — barres de vie

```java
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID, value = Dist.CLIENT)
public final class HealthBarRenderer {
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) { ... }
}
```

`RenderLivingEvent.Post` est émis par `LivingEntityRenderer.render` **après** son `popPose()` : la
pile de matrices est donc revenue à l'origine de l'entité, exactement dans l'état où le jeu s'apprête
à dessiner la plaque de nom. C'est le point d'accroche idéal, et il vaut aussi pour les joueurs —
`PlayerRenderer.render` délègue à `super.render`, qui émet l'évènement.

Le billboard reprend la recette de `EntityRenderer#renderNameTag` :

```java
poseStack.translate(anchor.x, anchor.y + 0.5D + offset, anchor.z);
poseStack.mulPose(dispatcher.cameraOrientation());
poseStack.scale(0.025F, -0.025F, 0.025F);
```

Le `-0.025F` inverse l'axe Y : dans le repère local qui suit, **les valeurs négatives montent**. La
barre occupe donc `y ∈ [-hauteur, 0]`.

Deux choix méritent d'être expliqués :

- **`RenderType.debugQuads()`** — malgré son nom, c'est le type public qui correspond exactement au
  besoin : `POSITION_COLOR`, quads translucides, **sans face culling** (le sens d'enroulement des
  sommets n'a donc pas d'importance) et sans lightmap, si bien que la barre reste lisible en pleine
  nuit. Ce format n'accepte pas `setLight()` : chaque sommet se résume à
  `addVertex(matrix, x, y, z).setColor(argb)`.
- **Des quads jointifs plutôt qu'empilés** — le cadre est dessiné en quatre bandes autour de la
  barre, et le fond ne couvre que la portion vide. Aucune surface ne se superpose, ce qui supprime le
  risque de z-fighting entre faces coplanaires sans dépendre de l'ordre de tri des translucides.

La couleur suit la teinte du cercle chromatique, de 0 (rouge) à 1/3 (vert) :
`Mth.hsvToArgb(fraction / 3.0F, 1.0F, 1.0F, 255)`.

### 6. `client/NameTagHandler.java` — masquage des pseudos

```java
@SubscribeEvent
public static void onRenderNameTag(RenderNameTagEvent event) {
    if (!hidePlayerNameTags()) return;
    if (event.getEntity() instanceof Player) event.setCanRender(TriState.FALSE);
}
```

`RenderNameTagEvent` est émis juste avant le rendu de la plaque, et `setCanRender(TriState.FALSE)`
l'annule proprement — sans mixin, sans annuler le reste du rendu de l'entité. Seul l'affichage dans
le monde est concerné : la liste des joueurs, le chat et les mobs nommés ne bougent pas.

### 7. `config/MobHealthModifierServerConfig.java` et `client/DisplayPolicy.java` — autorité du serveur

Un serveur doit pouvoir décider si les barres de vie et les pseudos s'affichent, sans quoi le mod
serait inutilisable sur un serveur PvP ou RP. Ce besoin est couvert **sans écrire un seul paquet
réseau** : NeoForge synchronise automatiquement toute configuration de type `SERVER` vers chaque
client pendant la phase de connexion, avant l'entrée dans le monde. Le client lit ensuite ces valeurs
comme si elles étaient locales.

```java
public static void register(ModContainer container) {
    container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
}
```

Le fichier vit dans `<monde>/serverconfig/mobhealthmodifier-server.toml` : une config `SERVER` est
propre à chaque monde, pas globale comme celles de `config/`.

Deux interrupteurs indépendants — `enforceHealthBarSettings` et `enforceNameTagSettings` — décident
si les valeurs du serveur remplacent celles du joueur. `DisplayPolicy` est le seul point d'arbitrage,
et le rendu ne consulte qu'elle :

```java
public static boolean hidePlayerNameTags() {
    return MobHealthModifierServerConfig.enforcesNameTagSettings()
            ? MobHealthModifierServerConfig.hidePlayerNameTags()
            : MobHealthModifierClientConfig.hidePlayerNameTags();
}
```

Deux situations auraient pu poser problème, et se règlent d'elles-mêmes parce que les deux
`enforce*` valent `false` par défaut :

- **Au menu principal**, aucune config `SERVER` n'est chargée. `isLoaded()` renvoie `false`, les
  méthodes `enforces*()` aussi, et les préférences locales s'appliquent.
- **Sur un serveur dépourvu du mod**, NeoForge charge les valeurs par défaut côté client
  (`loadDefaultServerConfigs`). Les `enforce*` sont donc à `false` et rien n'est imposé.

Trois réglages restent délibérément hors de portée du serveur : `barWidth`, `barHeight` et
`verticalOffset`. Ils ne procurent aucun avantage de jeu — imposer l'esthétique de chacun n'aurait
pas de sens.

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

Deux fichiers, deux portées.

`config/mobhealthmodifier-common.toml` — gameplay, imposé par le serveur

```toml
[general]
	affectedMobs = "HOSTILE"      # ALL | HOSTILE | PASSIVE
	enableHealthModification = true
	healthMultiplier = 1.0        # 0.1 – 10.0
	enableDamageModification = true
	damageMultiplier = 1.0        # 0.1 – 10.0
```

`config/mobhealthmodifier-client.toml` — préférences du joueur

```toml
[display]
	enableHealthBars = true
	showOnHostileMobs = true
	showOnPassiveMobs = false
	showOnPlayers = true
	hidePlayerNameTags = false
	maxRenderDistance = 24.0      # 4 – 64 blocs
	barWidth = 40                 # 8 – 120 px
	barHeight = 5                 # 1 – 20 px
	verticalOffset = 0.0          # -2.0 – 2.0 blocs
```

`<monde>/serverconfig/mobhealthmodifier-server.toml` — affichage imposé, synchronisé aux clients

```toml
[display]
	enforceHealthBarSettings = false
	healthBarsEnabled = true
	showOnHostileMobs = true
	showOnPassiveMobs = false
	showOnPlayers = true
	maxRenderDistance = 24.0

	enforceNameTagSettings = false
	hidePlayerNameTags = true
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

5. CONNEXION D'UN CLIENT
   NeoForge envoie mobhealthmodifier-server.toml au client
   └─ DisplayPolicy saura si le serveur impose ses réglages

6. RENDU, À CHAQUE IMAGE, CÔTÉ CLIENT
   RenderLivingEvent.Post
   └─ vivant ? visible ? à portée ? catégorie cochée (via DisplayPolicy) ?
      └─ quads billboardés au point d'ancrage de la plaque de nom
   RenderNameTagEvent
   └─ joueur ? DisplayPolicy.hidePlayerNameTags() ? → setCanRender(FALSE)
```

`setBaseValue` ne soigne pas l'entité : sans le `setHealth` qui suit, un zombie passé de 20 à 40 HP
apparaîtrait à moitié blessé.

---

## 🎮 Comportement

**Fait :**
- multiplie santé et dégâts des mobs qui apparaissent, hostiles par défaut ;
- cible les hostiles, les passifs ou tous, au choix (`affectedMobs`) ;
- chaque modificateur s'active/se désactive indépendamment ;
- affiche une barre de vie au-dessus des mobs et des joueurs (côté client) ;
- masque les pseudos des joueurs par défaut, réversible côté client ;
- permet à un serveur d'imposer barres et pseudos à tous ses joueurs ;
- côté serveur seul, renforce les mobs sans que personne n'installe quoi que ce soit.

**Ne fait pas :**
- modifier les mobs déjà présents avant l'installation ;
- appliquer une nouvelle config aux mobs existants ;
- recharger la config à chaud, ni fournir commande ou GUI ;
- afficher les barres sans le mod côté client — le rendu est local par nature ;
- montrer les barres à travers les murs : ce serait un wallhack sur les joueurs ;
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
| Classes | 12 (Main, 3 configs + leurs 3 classes internes, MobTarget, Handler, DisplayPolicy, HealthBarRenderer, NameTagHandler) |
| Évènements écoutés | 3 (`EntityJoinLevelEvent`, `RenderLivingEvent.Post`, `RenderNameTagEvent`) |
| Fichiers de config | 3 (COMMON, SERVER synchronisée, CLIENT) |
| Attributs modifiés | 2 (`MAX_HEALTH`, `ATTACK_DAMAGE`) |
| Paramètres de config | 22 — 5 gameplay + 9 affichage local + 8 affichage imposé |

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
