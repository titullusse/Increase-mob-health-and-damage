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
| Côté | Serveur pour les multiplicateurs, client pour l'affichage |

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

Le serveur peut en revanche **imposer** les réglages d'affichage aux joueurs qui ont le mod — voir
[Autorité du serveur](#autorité-du-serveur--mondeserverconfigmobhealthmodifier-servertoml).

---

## Configuration

Trois fichiers, créés au premier lancement :

| Fichier | Emplacement | Portée |
|---------|-------------|--------|
| `mobhealthmodifier-common.toml` | `config/` | Gameplay — multiplicateurs et ciblage |
| `mobhealthmodifier-client.toml` | `config/` | Affichage — préférences du joueur |
| `mobhealthmodifier-server.toml` | `<monde>/serverconfig/` | Affichage imposé par le serveur |

⚠️ Le fichier serveur n'est **pas** dans `config/` mais dans le dossier du monde —
`world/serverconfig/` sur un serveur, `saves/<nom du monde>/serverconfig/` en solo. C'est NeoForge
qui impose cet emplacement : une config `SERVER` est propre à chaque monde.

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
	# Aucun plafond ici, mais Minecraft limite la sante a 1024 points :
	# au-dela le mob restera a 1024 (soit x51 environ pour un zombie).
	# Range: 0.1 ~ 1000000.0
	healthMultiplier = 1.0

	# Active la modification des degats d'attaque des mobs.
	enableDamageModification = true

	# Multiplicateur applique aux degats d'attaque.
	# 1.0 = normal | 2.0 = double | 0.5 = moitie
	# Aucun plafond ici, mais Minecraft limite les degats a 2048.
	# Range: 0.1 ~ 1000000.0
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

### Jusqu'où peut-on monter ?

Il n'y a plus de plafond côté mod : `healthMultiplier = 500.0` est accepté. Mais **Minecraft borne
lui-même ses attributs**, et c'est là que la limite réelle se trouve :

| Attribut | Plafond du jeu |
|----------|----------------|
| Santé maximale | **1024** points de vie |
| Dégâts d'attaque | **2048** |

Ces bornes sont déclarées par Minecraft (`RangedAttribute`) et appliquées avant que la valeur ne
serve. Aucun mod ne peut les dépasser sans réécrire l'attribut lui-même.

Conséquence concrète : le multiplicateur utile dépend de la valeur de départ du mob.

| Mob | Santé de base | Multiplicateur au-delà duquel plus rien ne change |
|-----|---------------|--------------------------------------------------|
| Zombie, creeper, squelette | 20 | × 51.2 |
| Enderman | 40 | × 25.6 |
| Ravageur | 100 | × 10.24 |
| Warden | 500 | × 2.05 |

Autrement dit, `healthMultiplier = 1000.0` sur un zombie donne exactement le même résultat que
`51.2` : 1024 points de vie. Le mod écrit d'ailleurs directement la valeur plafonnée, pour que les
données de l'entité correspondent à ce que le jeu utilise réellement.

Le minimum reste `0.1`. Une valeur hors intervalle est refusée par NeoForge et remplacée par le
défaut, avec un avertissement dans les logs.

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
	# Ne laisse que la barre de vie au-dessus de leur tete.
	# N'affecte ni les mobs nommes, ni la liste des joueurs (touche Tab).
	# Un serveur peut imposer ce reglage et ignorer ce choix.
	hidePlayerNameTags = true

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

`hidePlayerNameTags = true` — le défaut — supprime le pseudo flottant au-dessus des joueurs, ne
laissant que la barre de vie. Passez-le à `false` pour retrouver les pseudos.

Le masquage ne touche ni au chat, ni à la liste des joueurs (touche Tab), ni aux plaques de nom des
mobs nommés : uniquement l'étiquette affichée en jeu au-dessus des joueurs.

### Ce que les barres ne font pas

Elles sont **occultées par les blocs** : pas de vision à travers les murs. C'est délibéré — une
option « voir à travers les murs » sur les barres des joueurs serait un wallhack, et n'a pas sa place
dans un mod qu'on installe sur un serveur.

Les porte-armures n'en reçoivent pas, bien qu'ils soient techniquement des entités vivantes. Les
barres n'affichent pas de valeur chiffrée, ni les cœurs d'absorption ou d'armure.

---

## Autorité du serveur — `<monde>/serverconfig/mobhealthmodifier-server.toml`

Par défaut chaque joueur règle son affichage comme il l'entend. Un serveur peut reprendre la main :

```toml
[display]
	# Imposer les reglages de barres de vie ci-dessous a tous les joueurs.
	enforceHealthBarSettings = false
	healthBarsEnabled = true
	showOnHostileMobs = true
	showOnPassiveMobs = false
	showOnPlayers = true
	maxRenderDistance = 24.0

	# Imposer la visibilite des pseudos ci-dessous a tous les joueurs.
	enforceNameTagSettings = false
	hidePlayerNameTags = true
```

Les deux interrupteurs sont indépendants :

| Interrupteur | Ce qu'il verrouille |
|--------------|--------------------|
| `enforceHealthBarSettings` | Barres actives ou non, sur quelles catégories, à quelle distance |
| `enforceNameTagSettings` | Visibilité des pseudos des joueurs |

Tant qu'un interrupteur reste à `false`, le fichier client du joueur décide. Dès qu'il passe à
`true`, les valeurs du serveur juste en dessous s'appliquent à tout le monde et le choix local est
ignoré pour ce groupe.

**Exemples.** Un serveur PvP qui ne veut pas que la vie des adversaires soit lisible à distance :

```toml
enforceHealthBarSettings = true
showOnPlayers = false        # pas de barre sur les joueurs
showOnHostileMobs = true     # mais on garde celles des mobs
```

Un serveur RP qui impose l'anonymat :

```toml
enforceNameTagSettings = true
hidePlayerNameTags = true
```

### Comment ça marche

Aucun paquet réseau n'a été écrit pour cela. NeoForge synchronise automatiquement toute
configuration de type `SERVER` vers chaque client pendant la phase de connexion, avant l'entrée dans
le monde ; le client lit ensuite ces valeurs comme si elles étaient les siennes.

Deux garde-fous encadrent le basculement :

- **Hors partie**, au menu principal, la config serveur n'est pas chargée : rien n'est imposé.
- **Sur un serveur dépourvu du mod**, NeoForge charge les valeurs par défaut, dans lesquelles les
  deux `enforce*` valent `false`. Les préférences du joueur s'appliquent donc normalement.

Ce que le serveur ne verrouille **jamais** : `barWidth`, `barHeight` et `verticalOffset`. Ces trois
réglages ne procurent aucun avantage, un serveur n'a pas à dicter l'esthétique de chacun.

Le point d'arbitrage tient dans une seule classe, `DisplayPolicy` : le rendu ne consulte qu'elle, et
elle seule décide qui du serveur ou du client l'emporte.

---

## Fonctionnement

Le mod écoute `EntityJoinLevelEvent` et, pour chaque `Mob` arrivant côté serveur, multiplie la
valeur de base des attributs `MAX_HEALTH` et `ATTACK_DAMAGE`.

```
Démarrage
  └─ MobHealthModifier enregistre ses trois configurations
     ├─ COMMON → config/mobhealthmodifier-common.toml
     ├─ SERVER → <monde>/serverconfig/mobhealthmodifier-server.toml
     └─ CLIENT → config/mobhealthmodifier-client.toml

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

Côté client, le rendu se greffe sur deux évènements NeoForge. Chaque décision passe d'abord par
`DisplayPolicy`, qui tranche entre les réglages imposés par le serveur et ceux du joueur :

```
Connexion à un monde
  └─ NeoForge envoie mobhealthmodifier-server.toml au client

Rendu d'une entité vivante
  └─ RenderLivingEvent.Post
     └─ DisplayPolicy : barres actives ? cette catégorie ? à cette distance ?
        └─ HealthBarRenderer : quads orientés face à la caméra, au point
           d'ancrage de la plaque de nom, décalé de verticalOffset

Rendu d'une plaque de nom
  └─ RenderNameTagEvent
     └─ DisplayPolicy : pseudos masqués ?
        └─ NameTagHandler : setCanRender(FALSE) si c'est un joueur
```

La barre est construite en quads jointifs — cadre en quatre bandes, portion pleine, portion vide —
plutôt qu'en rectangles empilés. Aucune surface ne se superpose, ce qui écarte tout risque de
z-fighting sans dépendre de l'ordre de tri des faces translucides.

### Structure

```
src/main/java/com/marc33/mobhealth/
├── MobHealthModifier.java                    # @Mod, enregistre les trois configs
├── config/
│   ├── MobHealthModifierConfig.java          # gameplay          (COMMON)
│   ├── MobHealthModifierServerConfig.java    # affichage imposé  (SERVER, synchronisée)
│   └── MobTarget.java                        # ciblage ALL / HOSTILE / PASSIVE
├── events/MobAttributeHandler.java           # applique les multiplicateurs
└── client/                                   # chargé côté client uniquement
    ├── MobHealthModifierClientConfig.java    # préférences locales (CLIENT)
    ├── DisplayPolicy.java                    # arbitre serveur vs client
    ├── HealthBarRenderer.java                # dessine les barres de vie
    └── NameTagHandler.java                   # masque les pseudos

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
- La santé ne peut pas dépasser 1024 ni les dégâts 2048 : ce sont les bornes de Minecraft, pas
  celles du mod.
- `ATTACK_DAMAGE` n'existe pas sur tous les mobs. Les creepers (explosion) et les mobs à distance
  (squelette, blaze) infligent des dégâts par un autre biais et ne sont pas affectés côté dégâts.
- Les barres de vie exigent le mod côté client ; installé sur le serveur seul, il ne fait que
  renforcer les mobs.

## Dépannage

| Symptôme | Piste |
|----------|-------|
| Le fichier de config n'existe pas | Il est créé au **premier** lancement ; démarrez une fois puis quittez. |
| Les mobs ne changent pas | Vérifiez que ce sont de **nouveaux** spawns, que les `enable*` sont à `true`, et que `affectedMobs` couvre bien le mob testé (par défaut `HOSTILE` : une vache n'est pas affectée). |
| Le mod n'apparaît pas dans la liste | Vérifiez la version de NeoForge (21.1.x) et que le JAR est bien dans `mods/`. |
| Aucune barre de vie en multijoueur | Le rendu est côté client : le JAR doit être dans **votre** dossier `mods/`, pas seulement sur le serveur. |
| Mes réglages d'affichage sont ignorés | Le serveur les impose. Regardez les `enforce*` dans `<monde>/serverconfig/mobhealthmodifier-server.toml`. |
| Je ne trouve pas le fichier serveur | Il est dans le dossier du monde, pas dans `config/`, et il n'apparaît qu'après le premier chargement du monde. |
| Barres invisibles sur les animaux | `showOnPassiveMobs` est à `false` par défaut. |
| Barres qui disparaissent de loin | Augmentez `maxRenderDistance`. |
| Erreur de compilation Java | `java -version` doit indiquer 21 ou plus. |

```bash
# Logs du mod
grep -i "mobhealthmodifier" logs/latest.log
```

---

## Développement

**Ajouter un attribut** (exemple : la vitesse) — trois points :

1. Déclarer la valeur dans `MobHealthModifierConfig.Common` avec `defineInRange(...)`.
2. Ajouter le getter statique correspondant.
3. L'appliquer dans `MobAttributeHandler.applyModifications()` via
   `mob.getAttribute(Attributes.MOVEMENT_SPEED)`.

**Ajouter un réglage d'affichage** — le déclarer dans `MobHealthModifierClientConfig`, puis exposer
le getter correspondant dans `DisplayPolicy`. Le rendu ne doit jamais lire une config directement :
c'est ce qui garantit qu'il n'existe qu'un seul endroit où l'arbitrage serveur/client est décidé. S'il
doit pouvoir être imposé, le déclarer aussi dans `MobHealthModifierServerConfig` et le brancher sur
l'interrupteur `enforce*` adéquat.

Lancer un environnement de test :

```bash
./gradlew runClient   # client de dev
./gradlew runServer   # serveur de dev (nécessite d'accepter l'EULA Mojang)
```

Documents de référence : [`SYNTHESE_PROJET.md`](SYNTHESE_PROJET.md) (architecture détaillée) et
[`CHEAT_SHEET.md`](CHEAT_SHEET.md) (aide-mémoire).

## Licence

MIT
