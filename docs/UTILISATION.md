# Guide d'Utilisation - ELIXIR-REIGN

Bienvenue dans **ELIXIR-REIGN**, un jeu de stratégie isométrique où vous devrez construire votre village et défendre vos ressources contre vos adversaires !

## 📱 Plateformes Disponibles

ELIXIR-REIGN est jouable sur **plusieurs plateformes** :

- **Android** : Téléchargez et lancez l'application sur votre téléphone ou tablette
- **PC (Windows/Linux/Mac)** : Lancez le jeu directement depuis votre ordinateur
- **Cross-Platforme** : Les joueurs Android et PC peuvent jouer ensemble sur le même serveur !

---

## 🎮 Commencer une Partie

### Modes Multijoueur

ELIXIR-REIGN propose deux modes de jeu multijoueur :

- **Mode 1v1** : Affrontement entre 2 joueurs
- **Mode 4 joueurs** : Bataille royale avec 4 joueurs aux 4 coins de la carte

### Configuration de Départ

- Chaque joueur démarre avec **1 Hôtel de Ville**
- Vous devez construire et améliorer vos bâtiments pour progresser
- Les joueurs commencent aux 4 coins de la carte (mode 4 joueurs)

## 🎯 Objectif

**Détruire tous les Hôtels de Ville des adversaires** pour remporter la victoire !

## 💰 Ressources Disponibles

| Ressource | Usage |
|-----------|-------|
| **Or** | Construire et améliorer vos bâtiments |
| **Élixir** | Entraîner les troupes offensives |
| **Élixir Noir** | Améliorer vos troupes et défenses |

## 🏗️ Bâtiments

### Bâtiments de Ressources

- **Hôtel de Ville** : Centre du village (à protéger !)
- **Mine d'Or** : Génère de l'or (améliorable)
- **Pompe à Élixir** : Génère de l'élixir (améliorable)
- **Pompe à Élixir Noir** : Génère de l'élixir noir (améliorable)

### Bâtiments Militaires

- **Caserne** :
    - Entraîne des troupes (Barbares, Archers, Géants)
    - Limite le nombre total de troupes

- **Canon** : Défense contre les troupes ennemies

- **Tour d'Archers** : Défense contre les troupes ennemies

## ⚔️ Troupes

Vous pouvez entraîner 3 types de troupes via la Caserne :

- **Barbares** : Troupes de mêlée puissantes et rapides
- **Archers** : Troupes à distance
- **Géants** : Troupes très résistantes et puissantes

## 🎮 Comment Jouer - Contrôles et Interface

### Sur PC (LWJGL3)

| Action | Contrôle |
|--------|----------|
| **Déplacer la vue** | Clic droit + mouvement souris OU touches fléchées |
| **Zoomer** | Molette souris (haut/bas) |
| **Sélectionner un bâtiment** | Clic gauche sur le bâtiment |
| **Construire/Placer** | Clic gauche sur l'emplacement souhaité |
| **Améliorer un bâtiment** | Clic gauche sur le bâtiment + option d'amélioration |
| **Entraîner une troupe** | Clic gauche sur la Caserne + sélectionner le type |
| **Attaquer** | Clic gauche sur une troupe, puis clic sur la cible |
| **Menu/Paramètres** | Clic sur l'icône menu en haut à gauche |

### Sur Android

| Action | Contrôle |
|--------|----------|
| **Déplacer la vue** | Glisser votre doigt sur l'écran |
| **Zoomer** | Pincer/écarter deux doigts |
| **Sélectionner un bâtiment** | Appuyez sur le bâtiment |
| **Construire/Placer** | Appuyez sur l'emplacement souhaité |
| **Améliorer un bâtiment** | Appuyez sur le bâtiment + option d'amélioration |
| **Entraîner une troupe** | Appuyez sur la Caserne + sélectionner le type |
| **Attaquer** | Appuyez sur une troupe, puis sur la cible |
| **Menu/Paramètres** | Appuyez sur l'icône menu (en haut) |

---

## 🧪 Tester le Jeu Seul

Vous pouvez **lancer plusieurs instances du jeu** pour tester les fonctionnalités multijoueur seul(e) :

- Lancez le jeu avec un compte/joueur 1
- Lancez une deuxième instance du jeu avec un compte/joueur 2
- Les deux instances se connectent au **même serveur** et peuvent interagir
- Idéal pour tester les attaques, les défenses et les stratégies !

### Pour PC :
- Lancez le jeu plusieurs fois depuis votre ordinateur

### Pour Android :
- Créez plusieurs profils utilisateurs ou utilisez des appareils/émulateurs différents


---

## 📋 Conseils de Jeu

✅ **Développement** : Améliorez régulièrement vos bâtiments

✅ **Défense** : Disposez vos défenses stratégiquement autour de votre Hôtel de Ville

✅ **Attaque** : Entraînez une armée équilibrée avant d'attaquer

✅ **Ressources** : Gérez vos ressources avec parcimonie

✅ **Cross-Plateforme** : Vous pouvez affronter des joueurs PC en étant sur Android et vice-versa !

---

## 🌐 Architecture du Jeu

ELIXIR-REIGN utilise une architecture **client-serveur** :

- **Serveur central** : Gère l'état du jeu, les joueurs et les interactions
- **Clients multiples** : Peuvent être sur Android, PC ou toute autre plateforme supportée
- **Communication en temps réel** : Via TCP/UDP pour une expérience fluide
- **Multi-instance** : Vous pouvez avoir plusieurs clients connectés au même serveur pour tester ou jouer

---

**Bonne chance et que le meilleur stratège gagne !** 🏆

