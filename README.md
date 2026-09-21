# 🟠 FirstParkour

Plugin Minecraft **Paper 1.21 / Folia** — Parkour infini per-joueur avec duels, thèmes de blocs et classements.

---

## ✨ Fonctionnalités

- **Parkour infini** — génération procédurale de blocs adaptée à la difficulté choisie
- **Monde dédié** — le parkour se joue dans un monde séparé configurable
- **3 difficultés** — Facile, Normal, Difficile (distance, hauteur et angle configurables)
- **8 thèmes de blocs** — Défaut, Nether, The End, Océan, Hiver, Désert, Nuages, Jungle
- **Système de duels** — inviter un ami, compte à rebours commun, le premier qui tombe perd
- **Classements top 10** per difficulté avec PlaceholderAPI
- **Menu GUI** avec support **Nexo** (textures custom sur tous les boutons)
- **MySQL + HikariCP** — persistance des records, total de sauts et thème choisi
- **Folia-compatible** — schedulers régionaux pour les blocs, entity pour l'UI, async pour la DB

---

## 📋 Prérequis

| Dépendance | Version | Obligatoire |
|---|---|---|
| Paper / Folia | 1.21.x | ✅ |
| MySQL | 5.7+ / 8.x | ✅ |
| PlaceholderAPI | 2.11+ | ⬜ optionnel |
| Nexo | 0.9+ | ⬜ optionnel |
| TAB | toute version PAPI-compatible | ⬜ optionnel |

---

## ⚙️ Configuration

```yaml
# config.yml

parkour:
  world: 'FirstParkour'   # Monde dédié (laisser vide = monde actuel du joueur)
  spawn:
    x: 0.0
    y: 100.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0

mysql:
  host: localhost
  port: 3306
  database: firstparkour
  username: root
  password: ''
```

Le spawn se configure en jeu avec `/parkour definirespawn`.

### Monde dédié

Créez un monde vide avec le plugin de votre choix (Multiverse, MyWorlds, etc.) puis renseignez son nom dans `parkour.world`. Les joueurs y sont téléportés au démarrage et ramenés à leur position d'origine à la fin.

### Thèmes Nexo

Renseignez les IDs de vos items Nexo dans `gui.nexo-items.*` (laisser vide `''` = fallback vanilla) :

```yaml
gui:
  nexo-items:
    filler: 'parkour_fond'
    easy: 'parkour_facile'
    medium: 'parkour_normal'
    hard: 'parkour_difficile'
    themes: 'parkour_themes'
    # ...
    theme-nether: 'parkour_theme_nether'
    theme-end: 'parkour_theme_end'
    # etc.
```

Pour un fond full-screen dans le titre du menu, utilisez le glyph Unicode de votre texture Nexo :
```yaml
gui:
  title: '&6FirstParkour'
```

---

## 🎮 Commandes

| Commande | Description | Permission |
|---|---|---|
| `/parkour` | Ouvre le menu | `firstparkour.use` |
| `/parkour arreter` | Arrête la session en cours | `firstparkour.use` |
| `/parkour stats` | Voir ses records et statistiques | `firstparkour.use` |
| `/parkour classement [facile\|normal\|difficile]` | Classement top 10 | `firstparkour.use` |
| `/parkour duel <joueur> [facile\|normal\|difficile]` | Inviter en duel | `firstparkour.use` |
| `/parkour duel accepter` | Accepter une invitation | `firstparkour.use` |
| `/parkour duel refuser` | Refuser une invitation | `firstparkour.use` |
| `/parkour themes` | Ouvre le menu de thèmes | `firstparkour.use` |
| `/parkour definirespawn` | Définir le spawn du parkour | `firstparkour.admin` |
| `/parkour recharger` | Recharger la configuration | `firstparkour.admin` |

Alias principal : `/pk`

---

## 📊 PlaceholderAPI

| Placeholder | Description |
|---|---|
| `%firstparkour_score%` | Score actuel (session en cours) |
| `%firstparkour_playing%` | `true` / `false` |
| `%firstparkour_difficulty%` | Difficulté en cours |
| `%firstparkour_best_easy%` | Record personnel Facile |
| `%firstparkour_best_medium%` | Record personnel Normal |
| `%firstparkour_best_hard%` | Record personnel Difficile |
| `%firstparkour_total_jumps%` | Total de sauts |
| `%firstparkour_top_easy_1_name%` | Nom du 1er Facile |
| `%firstparkour_top_easy_1_score%` | Score du 1er Facile |
| `%firstparkour_top_medium_3_name%` | Nom du 3ème Normal |
| *(rang 1-10, difficultés easy/medium/hard)* | |

---

## ⚔️ Système de Duels

1. `/parkour duel <joueur> [difficulté]` — l'adversaire reçoit un message cliquable
2. Il accepte avec `/parkour duel accepter` (ou clique sur le message)
3. Compte à rebours 3-2-1, les deux joueurs démarrent **simultanément**
4. L'action bar affiche le score de l'adversaire en temps réel
5. Le premier qui tombe perd — titre `VICTOIRE` / `DÉFAITE` + scores comparés
6. Une déconnexion compte comme une défaite

---

## 🏗️ Architecture technique

```
fr.firstsky.firstparkour/
├── FirstParkour.java           — Plugin principal
├── command/ParkourCommand.java — Commandes + tabcomplete filtré
├── database/DatabaseManager.java — MySQL + HikariCP + migration auto
├── gui/
│   ├── ParkourMenu.java        — Menu principal (Nexo-aware)
│   └── ThemeMenu.java          — Menu de sélection de thème
├── listener/ParkourListener.java — Move/Join/Quit/Damage
├── manager/
│   ├── ParkourManager.java     — Sessions, téléportation, génération
│   ├── DuelManager.java        — Invitations, duels, résolution
│   └── LeaderboardManager.java — Cache top10, refresh async
├── model/
│   ├── Difficulty.java         — Enum (easy/medium/hard, FR+EN)
│   ├── BlockTheme.java         — Enum des 8 thèmes
│   ├── ParkourSession.java     — État d'une session (blocs, score, angle)
│   ├── PlayerData.java         — Données persistées
│   ├── DuelInvite.java         — Invitation en attente
│   └── ActiveDuel.java         — Duel en cours
├── placeholder/ParkourExpansion.java — Extension PAPI
└── util/
    ├── BlockGenerator.java     — Génération procédurale des blocs
    ├── FoliaUtil.java          — Wrappers RegionScheduler/EntityScheduler/AsyncScheduler
    ├── MessageUtil.java        — Couleurs, titre, action bar
    └── NexoUtil.java           — Chargement sécurisé items Nexo
```

**Folia** : toutes les opérations sur les blocs passent par `RegionScheduler`, les actions joueur par `EntityScheduler`, la base de données par `AsyncScheduler`.

---

## 🔨 Build

```bash
# JDK 26 requis
mvn clean package
# JAR dans target/FirstParkour-1.0.0.jar
```

---

*Développé pour [FirstSky](https://github.com/FirstSky-Omega)*
