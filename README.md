# Increase-mob-health-and-damage

**Mob Health Modifier** — mod NeoForge pour Minecraft 1.21.1 qui multiplie la **santé** et les
**dégâts** des mobs, et affiche une **barre de vie** au-dessus des mobs et des joueurs. Tout se règle
dans des fichiers de configuration TOML.

| | |
|---|---|
| Version du mod | 1.0.0 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.0+ (compilé avec 21.1.248) |
| Java | 21+ |
| Côté | Serveur pour les multiplicateurs, client pour les barres de vie |

---

## Compilation

```bash
./gradlew build
```

Le premier build télécharge et décompile Minecraft : comptez 5 à 15 minutes. Les suivants prennent
quelques secondes.

JAR produit : `build/libs/mobhealthmodifier-1.0.0.jar`

## Installation

### Client (solo)

```bash
cp build/libs/mobhealthmodifier-1.0.0.jar ~/.minecraft/mods/
```

Lancez Minecraft avec le profil NeoForge 21.1, puis redémarrez une fois : le fichier de config est
créé au premier démarrage.

### Serveur

```bash
cp build/libs/mobhealthmodifier-1.0.0.jar /chemin/vers/serveur/mods/
```

Le mod se sépare en deux moitiés indépendantes :

| Fonction | Où elle s'exécute | Le client doit-il l'installer ? |
|----------|-------------------|--------------------------------|
| Multiplicateurs santé / dégâts | Serveur | Non — les valeurs sont synchronisées |
| Barres de vie, masquage des pseudos | Client | **Oui** |

Autrement dit : installez-le côté serveur seul, et tout le monde affronte des mobs renforcés sans
rien installer. Pour voir les barres de vie, chaque joueur doit avoir le JAR dans son propre dossier
`mods/`. Aucune des deux moitiés n'a besoin de l'autre pour fonctionner.

---

## Configuration

Deux fichiers, créés au premier lancement :

| Fichier | Portée |
|---------|--------|
| `config/mobhealthmodifier-common.toml` | Gameplay — multiplicateurs et ciblage |
| `config/mobhealthmodifier-client.toml` | Affichage — barres de vie et pseudos |

### Gameplay — `mobhealthmodifier-common.toml`

```toml
[general]
	# Quels mobs sont affectes par les multiplicateurs.
	# HOSTILE = zombies, squelettes, creepers, blazes, boss...
	# PASSIVE = vaches, moutons, villageois, golems...
	# ALL     = tous les mobs
	# Allowed Values: ALL, HOSTILE, PASSIVE
	affectedMobs = "HOSTILE"

	# Active la modification de la sante maximale des mobs.
	enableHealthModification = true

	# Multiplicateur applique a la sante maximale.
	# 1.0 = normal | 2.0 = double | 0.5 = moitie
	# Range: 0.1 ~ 10.0
	healthMultiplier = 1.0

	# Active la modification des degats d'attaque des mobs.
	enableDamageModification = true

	# Multiplicateur applique aux degats d'attaque.
	# 1.0 = normal | 2.0 = double | 0.5 = moitie
	# Range: 0.1 ~ 10.0
	damageMultiplier = 1.0
```

Modifiez les valeurs, puis **redémarrez** le jeu ou le serveur.

### Ciblage des mobs

`affectedMobs` choisit qui reçoit les multiplicateurs :

| Valeur | Effet |
|--------|-------|
| `HOSTILE` *(défaut)* | Uniquement les mobs hostiles |
| `PASSIVE` | Uniquement les mobs non hostiles |
| `ALL` | Tous les mobs |

Un mob est considéré comme hostile s'il porte le marqueur vanilla `Enemy` **ou** si sa catégorie de
spawn est `MONSTER`. Cela couvre toute la hiérarchie `Monster` (zombie, squelette, creeper, araignée,
enderman, piglin, wither, warden…) et les hostiles qui vivent hors de cette hiérarchie (slime, ghast,
phantom, shulker, hoglin, zoglin, ender dragon). Le second critère sert de filet pour les mobs
ajoutés par d'autres mods, qui déclarent parfois leur hostilité par la catégorie seule.

Les mobs hors ciblage ne sont pas touchés du tout : ils ne reçoivent aucun marqueur et seront
réexaminés si vous changez `affectedMobs` plus tard.

### Presets

| Mode | `healthMultiplier` | `damageMultiplier` |
|------|-----|-----|
| Facile | 0.5 | 0.5 |
| Normal | 1.0 | 1.0 |
| Difficile | 2.0 | 2.0 |
| Hardcore | 3.0 | 3.0 |

Les bornes 0.1 – 10.0 sont appliquées par NeoForge : une valeur hors intervalle est refusée et
remplacée par la valeur par défaut, avec un avertissement dans les logs.

### Exemple de résultat

Avec `healthMultiplier = 2.0` et `damageMultiplier = 1.5` :

| Mob | Santé | Dégâts |
|-----|-------|--------|
| Zombie | 20 → 40 | 3 → 4.5 |
| Squelette | 20 → 40 | *(dégâts portés par la flèche, non modifiés)* |
| Creeper | 20 → 40 | *(explosion, non modifiée)* |
| Enderman | 40 → 80 | 7 → 10.5 |

---

## Barres de vie — `mobhealthmodifier-client.toml`

Une barre de progression flotte au-dessus de chaque entité concernée, orientée face à la caméra. Elle
passe du vert au rouge en traversant le jaune et l'orange à mesure que la santé descend.

```toml
[display]
	# Interrupteur general des barres de vie.
	enableHealthBars = true

	# Afficher une barre au-dessus des mobs hostiles.
	showOnHostileMobs = true

	# Afficher une barre au-dessus des mobs passifs.
	showOnPassiveMobs = false

	# Afficher une barre au-dessus des joueurs.
	# Vous compris, si vous passez en vue a la troisieme personne.
	showOnPlayers = true

	# Masquer le pseudo affiche au-dessus des joueurs.
	# Pratique pour ne garder que la barre de vie.
	# N'affecte ni les mobs nommes, ni la liste des joueurs (touche Tab).
	hidePlayerNameTags = false

	# Distance maximale d'affichage d'une barre, en blocs.
	# Range: 4.0 ~ 64.0
	maxRenderDistance = 24.0

	# Largeur de la barre, en pixels (40 = un bloc de large).
	# Range: 8 ~ 120
	barWidth = 40

	# Hauteur de la barre, en pixels.
	# Range: 1 ~ 20
	barHeight = 5

	# Decalage vertical de la barre, en blocs. Negatif = plus bas.
	# Range: -2.0 ~ 2.0
	verticalOffset = 0.0
```

### Masquer les pseudos

`hidePlayerNameTags = true` supprime le pseudo flottant au-dessus des joueurs, ne laissant que la
barre de vie. C'est un réglage **local** : il ne change rien pour les autres joueurs, et il ne touche
ni au chat, ni à la liste des joueurs (touche Tab), ni aux plaques de nom des mobs nommés.

### Ce que les barres ne font pas

Elles sont **occultées par les blocs** : pas de vision à travers les murs. C'est délibéré — une
option « voir à travers les murs » sur les barres des joueurs serait un wallhack, et n'a pas sa place
dans un mod qu'on installe sur un serveur.

Les porte-armures n'en reçoivent pas, bien qu'ils soient techniquement des entités vivantes.

---

## Fonctionnement

Le mod écoute `EntityJoinLevelEvent` et, pour chaque `Mob` arrivant côté serveur, multiplie la
valeur de base des attributs `MAX_HEALTH` et `ATTACK_DAMAGE`.

```
Démarrage
  └─ MobHealthModifier → MobHealthModifierConfig.register()
     └─ NeoForge génère config/mobhealthmodifier-common.toml

Un mob apparaît
  └─ EntityJoinLevelEvent
     └─ MobAttributeHandler.onEntityJoinWorld()
        ├─ ignore le côté client et les non-mobs
        ├─ ignore les mobs hors ciblage (affectedMobs)
        ├─ ignore les mobs déjà traités (marqueur NBT persistant)
        ├─ MAX_HEALTH    ×= healthMultiplier, puis soin complet
        └─ ATTACK_DAMAGE ×= damageMultiplier
```

**À propos du marqueur NBT :** `EntityJoinLevelEvent` se déclenche aussi au rechargement d'un chunk,
pas seulement au spawn. Sans garde-fou, un zombie verrait sa santé doublée à chaque fois que le
joueur revient dans la zone. Le mod écrit donc un marqueur dans les données persistantes de
l'entité, ce qui garantit une modification unique par mob.

Côté client, le rendu se greffe sur deux évènements NeoForge :

```
Rendu d'une entité vivante
  └─ RenderLivingEvent.Post
     └─ HealthBarRenderer : quads orientés face à la caméra, au point d'ancrage
        de la plaque de nom, décalé de verticalOffset

Rendu d'une plaque de nom
  └─ RenderNameTagEvent
     └─ NameTagHandler : setCanRender(FALSE) si c'est un joueur
        et que hidePlayerNameTags est actif
```

La barre est construite en quads jointifs — cadre en quatre bandes, portion pleine, portion vide —
plutôt qu'en rectangles empilés. Aucune surface ne se superpose, ce qui écarte tout risque de
z-fighting sans dépendre de l'ordre de tri des faces translucides.

### Structure

```
src/main/java/com/marc33/mobhealth/
├── MobHealthModifier.java              # @Mod, enregistre les deux configs
├── config/MobHealthModifierConfig.java # config gameplay (COMMON)
├── config/MobTarget.java               # ciblage ALL / HOSTILE / PASSIVE
├── events/MobAttributeHandler.java     # applique les multiplicateurs
└── client/                             # côté client uniquement
    ├── MobHealthModifierClientConfig.java # config affichage (CLIENT)
    ├── HealthBarRenderer.java             # dessine les barres de vie
    └── NameTagHandler.java                # masque les pseudos

src/main/resources/
├── META-INF/neoforge.mods.toml         # métadonnées du mod
└── pack.mcmeta
```

---

## Limitations connues

- Les mobs **déjà présents** dans le monde avant l'installation ne sont pas modifiés ; seuls les
  nouveaux spawns le sont.
- Changer la configuration n'affecte pas les mobs déjà modifiés — un mob garde à vie les valeurs
  reçues à son apparition.
- Pas de rechargement à chaud : un redémarrage est nécessaire après édition du TOML.
- Pas de commande in-game ni de GUI.
- Le ciblage se fait par catégorie (hostiles / passifs / tous), pas par type précis : impossible de
  donner un multiplicateur différent aux zombies et aux creepers.
- `ATTACK_DAMAGE` n'existe pas sur tous les mobs. Les creepers (explosion) et les mobs à distance
  (squelette, blaze) infligent des dégâts par un autre biais et ne sont pas affectés côté dégâts.
- Les barres de vie exigent le mod côté client ; installé sur le serveur seul, il ne fait que
  renforcer les mobs.
- Les barres n'affichent pas de valeur chiffrée, ni les cœurs d'absorption ou d'armure.

## Dépannage

| Symptôme | Piste |
|----------|-------|
| Le fichier de config n'existe pas | Il est créé au **premier** lancement ; démarrez une fois puis quittez. |
| Les mobs ne changent pas | Vérifiez que ce sont de **nouveaux** spawns, que les `enable*` sont à `true`, et que `affectedMobs` couvre bien le mob testé (par défaut `HOSTILE` : une vache n'est pas affectée). |
| Le mod n'apparaît pas dans la liste | Vérifiez la version de NeoForge (21.1.x) et que le JAR est bien dans `mods/`. |
| Aucune barre de vie en multijoueur | Le rendu est côté client : le JAR doit être dans **votre** dossier `mods/`, pas seulement sur le serveur. |
| Barres invisibles sur les animaux | `showOnPassiveMobs` est à `false` par défaut. |
| Barres qui disparaissent de loin | Augmentez `maxRenderDistance`. |
| Erreur de compilation Java | `java -version` doit indiquer 21 ou plus. |

```bash
# Logs du mod
grep -i "mobhealthmodifier" logs/latest.log
```

---

## Développement

Ajouter un nouvel attribut (exemple : la vitesse) se fait en trois points :

1. Déclarer la valeur dans `MobHealthModifierConfig.Common` avec `defineInRange(...)`.
2. Ajouter le getter statique correspondant.
3. L'appliquer dans `MobAttributeHandler.applyModifications()` via
   `mob.getAttribute(Attributes.MOVEMENT_SPEED)`.

Lancer un environnement de test :

```bash
./gradlew runClient   # client de dev
./gradlew runServer   # serveur de dev (nécessite d'accepter l'EULA Mojang)
```

Documents de référence : [`SYNTHESE_PROJET.md`](SYNTHESE_PROJET.md) (architecture détaillée) et
[`CHEAT_SHEET.md`](CHEAT_SHEET.md) (aide-mémoire).

## Licence

MIT
