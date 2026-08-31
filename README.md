# Cambe Flow 💱

<p align="center">
  <img src="assets/banner.png" alt="Cambe Flow Banner" width="100%" />
</p>

<p align="center">
  <b>Application Android moderne de conversion de devises et d'analyse des marchés financiers en temps réel</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-brightgreen.svg?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/Room-Database-orange.svg" alt="Room" />
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Coroutines-purple.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
</p>

---

## 📱 Aperçu & Captures d'écran

<p align="center">
  <img src="assets/screenshot_converter.png" width="23%" alt="Convertisseur" />
  <img src="assets/screenshot_markets.png" width="23%" alt="Marchés" />
  <img src="assets/screenshot_trends.png" width="23%" alt="Tendances & Graphiques" />
  <img src="assets/screenshot_alerts.png" width="23%" alt="Alertes de Prix" />
</p>

> 💡 *Déposez vos captures d'écran dans le dossier `assets/` (`banner.png`, `screenshot_converter.png`, `screenshot_markets.png`, `screenshot_trends.png`, `screenshot_alerts.png`).*

---

## ✨ Fonctionnalités Principales

- **Convertisseur Instantané & Pavé Numérique Dédié** :
  - Conversion instantanée entre plus de 160 devises mondiales.
  - Pavé tactile intégré pour une saisie rapide et ergonomique.
  - Inversion instantanée des devises source et cible.
  - Mode hors-ligne avec basculement automatique sur les derniers taux mis en cache.

- **Marchés & Cotations en Direct** :
  - Vue d'ensemble des paires de devises majeures (EUR/USD, GBP/USD, USD/JPY, etc.).
  - Flux asynchrone continu avec calcul de la latence (ms) et indicateur de ticks en temps réel.
  - Recherche instantanée avec filtre réactif (`debounce`).

- **Tendances & Analyse Graphique** :
  - Visualisation des cours sur plusieurs horizons temporels : **1H, 1D, 1W, 1M, 1Y**.
  - Deux modes d'affichage graphique : **Courbe linéaire lissée** et **Chandelier japonais (Candlestick)**.
  - Statistiques de marché détaillées : Plus Haut 24h, Plus Bas 24h, Cours d'ouverture et Indice de volatilité.

- **Alertes de Prix & Surveillance Réactive** :
  - Définition de seuils personnalisés (franchissement à la hausse ou à la baisse).
  - Sentinelle d'arrière-plan analysant les variations en continu via Coroutines.
  - Notifications contextuelles intégrées dès qu'un seuil est atteint.

- **Historique & Journalisation Locale** :
  - Enregistrement automatique de chaque conversion effectuée.
  - Filtrage par période (7 derniers jours, dernier mois, 3 derniers mois).
  - Gestion et suppression des entrées avec synchronisation en base de données locale.

- **Moteur Asynchrone & Paramètres Avancés** :
  - Personnalisation de l'intervalle de synchronisation API (3s, 5s, 15s, 30s ou manuel).
  - Diagnostic en direct des performances de flux (latence, ticks générés).

---

## 🛠️ Architecture & Stack Technique

- **Langage** : Kotlin (100%)
- **Interface Utilisateur** : Jetpack Compose & Material Design 3 (M3)
- **Architecture** : MVVM (Model-View-ViewModel) réactif et Unidirectional Data Flow (UDF)
- **Gestion de l'État & Asynchronisme** :
  - Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Default`, `Dispatchers.Main`)
  - Kotlin StateFlow & SharedFlow
- **Persistance Locale** : Room Database (SQLite avec TypeConverters et Flow réactifs)
- **Réseau** : Retrofit 2 / OkHttp avec intégration de l'API de taux de change en direct
- **Design Visuel & Icônes** : Thème sombre moderne, typographie soignée et icône applicative adaptative

---

## 📁 Structure du Projet

```text
app/src/main/java/com/example/
├── MainActivity.kt                 # Point d'entrée principal & Navigation
├── data/
│   ├── local/                     # Base de données Room (Entities, DAOs, Database)
│   │   ├── AlertDao.kt
│   │   ├── HistoryDao.kt
│   │   ├── Converters.kt
│   │   └── TradeFlowDatabase.kt
│   ├── remote/                    # Client API & Modèles réseau
│   │   └── ExchangeRateApiService.kt
│   └── repository/                # Gestion unifiée des données & flux de ticks
│       └── CurrencyRepository.kt
├── model/                         # Modèles de données métier (Currency, Pair, Alert, etc.)
│   ├── Currency.kt
│   ├── MarketPair.kt
│   ├── PriceAlert.kt
│   └── ConversionRecord.kt
├── ui/
│   ├── components/                # Composants Compose réutilisables (TopBar, BottomNav, Modales)
│   ├── screens/                   # Écrans de l'application
│   │   ├── ConverterScreen.kt
│   │   ├── MarketsScreen.kt
│   │   ├── TrendsScreen.kt
│   │   ├── AlertsScreen.kt
│   │   ├── HistoryScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/                     # Définition des couleurs, typographies et formes M3
└── viewmodel/
    └── TradeFlowViewModel.kt      # ViewModel central & Moteur asynchrone
```

---

## 🚀 Compilation & Exécution

### Prérequis
- Android Studio Ladybug / Meerkat ou environnement cloud compatible
- JDK 17+
- Android SDK (API 34 recommandé, minSdk 24)

### Commandes de Build
```bash
# Vérifier et compiler l'application
gradle assembleDebug

# Lancer les tests unitaires et Robolectric
gradle :app:testDebugUnitTest
```

---

## 📄 Licence

Ce projet est distribué sous licence **Apache 2.0**. Consultez le fichier [`LICENSE`](LICENSE) pour plus d'informations.
