# ✅ Logique d'authentification granulaire implémentée

## 🎯 Règles de navigation

### ✅ CONSULTATION (Lecture) → Aucune authentification requise
- **HomeScreen** → voir la liste des groupes
- **Cliquer sur un groupe** → voir les dépenses (ExpenseScreen)
- **BalanceScreen** → voir le remboursement
- **ProfileScreen** → formulaire vide si pas connecté

### ❌ CRÉATION/MODIFICATION (Écriture) → Authentification requise
- **Ajouter un groupe** (+) → vérifie connexion
  - ✅ Connecté → GroupScreen (création)
  - ❌ Pas connecté → LoginScreen
- **Modifier le profil** → nécessiterait connexion (à implémenter)

## 📋 Implémentation

### MainActivity.kt

#### onAddGroupClick (Création = authentification requise)
```kotlin
onAddGroupClick = {
    if (!isNavigating) {
        isNavigating = true
        scope.launch {
            try {
                val loggedIn = profileUseCase.observeProfile().first() != null
                if (loggedIn) {
                    navController.navigate(Group)      // → GroupScreen
                } else {
                    navController.navigate(Login)      // → LoginScreen
                }
            } catch (e: Exception) {
                navController.navigate(Login)
            } finally {
                isNavigating = false
            }
        }
    }
}
```

#### onGroupClick (Consultation = pas d'authentification)
```kotlin
onGroupClick = { groupData ->
    expenseViewModel.loadGroup(groupData.id)
    navController.navigate(ExpenseRoute)  // Direct, pas de vérification
}
```

#### onProfileClick (Affichage du profil)
```kotlin
onProfileClick = {
    navController.navigate(ProfilRoute)  // Direct, écran vide si pas connecté
}
```

## 🔄 Flux utilisateur

### Scénario 1: Utilisateur NON connecté
```
1. Lancer l'app
2. Voir HomeScreen (groupes vides ou existants)
3. Cliquer sur "+" (ajouter groupe)
   → Vérification: pas connecté
   → Navigation vers LoginScreen
4. Se connecter
5. Retour à HomeScreen
6. Cliquer sur "+" (ajouter groupe)
   → Vérification: connecté ✅
   → Navigation vers GroupScreen
```

### Scénario 2: Consultation sans connexion
```
1. Lancer l'app
2. Voir la liste des groupes (HomeScreen)
3. Cliquer sur un groupe
   → Navigation vers ExpenseScreen (pas de vérification)
4. Voir les dépenses du groupe ✅
5. Consulter les remboursements (BalanceScreen) ✅
```

### Scénario 3: Profil sans connexion
```
1. Lancer l'app
2. Cliquer sur l'icône profil (👤)
   → Navigation vers ProfileScreen (pas de vérification)
3. Voir un formulaire vide (pas connecté) ✅
```

## 📊 Tableau de synthèse

| Action | Type | Auth? | Comportement |
|--------|------|-------|-------------|
| Voir groupes | Lecture | ❌ | Affiche la liste |
| Cliquer groupe | Lecture | ❌ | Navigue vers ExpenseScreen |
| Voir dépenses | Lecture | ❌ | Affiche les dépenses |
| Voir remboursements | Lecture | ❌ | Affiche BalanceScreen |
| **Ajouter groupe** | **Écriture** | **✅** | **Vérifie connexion** |
| **Modifier groupe** | **Écriture** | **✅** | **À implémenter** |
| **Voir profil** | **Lecture** | ❌ | Formulaire vide si pas connecté |
| **Modifier profil** | **Écriture** | **✅** | **À implémenter** |

## 🚀 Comment tester

### Test 1: Ajouter groupe sans connexion
1. Lancer l'app
2. Cliquer sur "+" (ajouter groupe)
3. ✅ Doit naviguer vers **LoginScreen**
4. Se connecter
5. Relancer l'app
6. Cliquer sur "+"
7. ✅ Doit naviguer vers **GroupScreen**

### Test 2: Consulter groupe sans connexion
1. Lancer l'app
2. Cliquer sur un groupe existant
3. ✅ Doit naviguer vers **ExpenseScreen** (pas de login requis)

### Test 3: Voir profil sans connexion
1. Lancer l'app
2. Cliquer sur l'icône profil (👤)
3. ✅ Doit afficher **ProfileScreen** vide

## 📝 Code modifié

### Fichier: MainActivity.kt
- ✏️ Restauré imports (profileUseCase, rememberCoroutineScope, etc.)
- ✏️ Restauré variables (profileUseCase, scope, isNavigating)
- ✏️ Ajouté vérification d'authentification dans `onAddGroupClick`
- ✏️ Gardé navigation directe pour `onGroupClick` (consultation)
- ✏️ Gardé navigation directe pour `onProfileClick` (affichage vide)

## ✅ Améliorations futures

- [ ] Ajouter vérification d'authentification pour modification de groupe
- [ ] Ajouter vérification d'authentification pour modification de profil
- [ ] Ajouter message "Connectez-vous pour créer un groupe" dans HomeScreen
- [ ] Désactiver le bouton "+" si pas connecté (optionnel)
- [ ] Implémenter "Créer groupe anonymement" (optionnel)

---

**Status:** ✅ Logique d'authentification implémentée
**Prochaine étape:** Tester les scénarios ci-dessus

