# FirstParkour

Plugin de parkour infini pour le serveur **FirstSky** — compatible **Folia 26.2**.  
Regroupe le parkour bloc, les modes avancés (IPPlus) et le parkour élytra en un seul plugin unifié.

---

## Fonctionnalités

- **Parkour bloc infini** — génération procédurale de blocs, scores et classements
- **Modes avancés** — Practice, Speed, SuperJump, TimeTrial et plus via le menu en jeu
- **Parkour élytra** — circuits en vol libre avec obstacles via `/eparkour`
- **Hologrammes** — classements en temps réel dans le monde
- **PlaceholderAPI** — intégration complète pour afficher les stats partout
- **Compatibilité Folia** — RegionScheduler, GlobalRegionScheduler et EntityScheduler sur tous les chemins critiques
- **Scoreboard TAB** — scoreboard interne désactivé, intégration via PlaceholderAPI (`%witp_score%`, `%witp_time%`, etc.)
- **Langue forcée** — langue serveur (français) appliquée à tous les joueurs, indépendamment des préférences sauvegardées

---

## Commandes

| Commande | Alias | Description |
|---|---|---|
| `/parkour` | `/witp`, `/ip` | Commande principale du parkour bloc |
| `/eparkour` | `/iep` | Parkour élytra |

> Les modes avancés (Practice, Duels, Speed, etc.) sont accessibles uniquement via le menu en jeu — la commande `/ipp` a été supprimée.

---

## Compatibilité serveur

| Plateforme | Version | Java |
|---|---|---|
| Folia | 26.2 | 21+ |
| Paper | 1.21.11 | 21 |

---

## Installation

1. Arrêter le serveur
2. Supprimer les anciens JARs IP, IPPlus et IEP si présents
3. Placer `FirstParkour.jar` dans le dossier `plugins/`
4. Démarrer le serveur

Les fichiers de config des anciens plugins (`plugins/IPPlus`, `plugins/IEP`) sont automatiquement copiés dans `plugins/IP/plus` et `plugins/IP/elytra` au premier démarrage.

---

## Build

```bash
./mvnw clean package
```

Le JAR compilé se trouve dans `target/IP-6.0.0-SNAPSHOT.jar`.

---

## Licence

GPL-3.0 — basé sur [Infinite Parkour](https://github.com/Efnilite/Walk-in-the-Park) par Efnilite. Voir [NOTICE.md](NOTICE.md).
