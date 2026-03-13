# README CI — Minimal release

Goal: This document explains how to use the minimal CI pipeline added to the repository to produce a signed APK named `Applicash.apk` and how to prepare the required GitHub secrets.

Workflow location
- GitHub Actions workflow: `.github/workflows/release.yml`
- The APK produced and renamed by the workflow is placed at `.ci/Applicash.apk` and uploaded as an artifact and attached to Releases when a tag `v*` is pushed.

Required GitHub repository secrets
- `KEYSTORE_BASE64`: the content of the keystore file (.jks) encoded in base64.
- `KEYSTORE_PASSWORD`: the keystore password (storepass).
- `KEY_ALIAS`: the key alias (e.g. `appli_key`).
- `KEY_PASSWORD`: the key password (keypass).
- `GITHUB_TOKEN`: provided automatically by GitHub Actions for uploading assets (you usually don't need to set this manually).

Local prerequisites (for preparing the secrets)
- JDK (for `keytool`) if you need to generate a keystore.
- GitHub CLI (`gh`) if you want to create secrets from the command line.
- Git configured to push to the repository.

Steps — create a keystore and add secrets (PowerShell)
Run these commands from `C:\Projets\AppliCash` in PowerShell.

1) (Optional) Generate a keystore if you don't have one:

```powershell
$keyStorePath = 'C:\Projets\AppliCash\appli-release.jks'
$keyAlias = 'appli_key'
# Generate a random password (example). Store it securely.
$pw = -join ((33..126) | Get-Random -Count 24 | ForEach-Object {[char]$_})
& keytool -genkeypair -v -keystore $keyStorePath -alias $keyAlias -keyalg RSA -keysize 2048 -validity 10000 -storetype JKS -storepass $pw -keypass $pw -dname "CN=Michaël Besily, OU=Dev, O=Appliclash, L=Paris, S=FR, C=FR"
```

Copy the generated password safely if you need it locally.

2) Encode the keystore to base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\Projets\AppliCash\appli-release.jks')) > C:\Projets\AppliCash\keystore_base64.txt
```

3) Add the secrets to the GitHub repository (using `gh`):

```powershell
# Make sure you are in the repo folder and have done 'gh auth login'
Set-Location C:\Projets\AppliCash
# add the base64 content
gh secret set KEYSTORE_BASE64 --body (Get-Content .\keystore_base64.txt -Raw)
# add passwords and alias
gh secret set KEYSTORE_PASSWORD --body "YourKeystorePassword"
gh secret set KEY_ALIAS --body "appli_key"
gh secret set KEY_PASSWORD --body "YourKeyPassword"
```

Alternatively, add secrets via the GitHub web UI (Settings → Secrets and variables → Actions).

Trigger the CI (push or tag)
- To build and obtain the APK as an artifact (push to `main`):

```powershell
git add .
git commit -m "ci: trigger build signed APK"
git push origin main
```

- To create a Release and attach `Applicash.apk`:

```powershell
git tag v1.0.0
git push origin v1.0.0
```

Where to get the APK
- GitHub → Actions → select the run → tab "Artifacts" → download the artifact named `Applicash` (file `Applicash.apk`).
- If you pushed a tag: GitHub → Releases → open the corresponding Release → download `Applicash.apk` attached.

Verify the signature (after downloading locally)

```powershell
# On your local machine with Android build-tools installed
# adapt path to your Android SDK build-tools
$buildTools = "$env:ANDROID_HOME\build-tools\$(Get-ChildItem $env:ANDROID_HOME\build-tools | Sort-Object Name | Select-Object -Last 1).Name"
& "$buildTools\apksigner" verify --print-certs .\Applicash.apk
```
An example of powerscript works
```powershell
# PowerShell script : create keystore, encode base64 and add GitHub secrets
# Exécutez depuis C:\Projets\AppliCash

Set-StrictMode -Version Latest
$RepoPath = 'C:\Projets\AppliCash'
Set-Location $RepoPath

# Vérifier prérequis
if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    Write-Error "keytool introuvable. Installez JDK et assurez-vous que keytool est dans le PATH."
    return
}
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "gh CLI introuvable. Installez GitHub CLI et exécutez 'gh auth login' avant de continuer."
    return
}

# Paramètres
$keyStorePath = Join-Path $RepoPath 'appli-release.jks'
$keyAlias = 'appli_key'
$ownerRepo = ''   # facultatif: 'owner/repo' si vous voulez cibler un repo précis via --repo

# Générer mot de passe aléatoire (24 caractères imprimables)
$pw = -join ((33..126) | Get-Random -Count 24 | ForEach-Object {[char]$_})
Write-Host "Mot de passe généré (copiez-le quelque part si vous en avez besoin) :" -ForegroundColor Yellow
Write-Host $pw -ForegroundColor Cyan

# Supprimer keystore existant si présent (confirm)
if (Test-Path $keyStorePath) {
    Write-Host "Le keystore existe déjà à $keyStorePath. Il sera supprimé et recréé." -ForegroundColor Yellow
    Remove-Item $keyStorePath -Force
}

# Créer le keystore
$dn = 'CN=Michaël Besily, OU=Dev, O=Appliclash, L=Paris, S=FR, C=FR'
$keytoolArgs = @(
    '-genkeypair', '-v',
    '-keystore', $keyStorePath,
    '-alias', $keyAlias,
    '-keyalg', 'RSA',
    '-keysize', '2048',
    '-validity', '10000',
    '-storetype', 'JKS',
    '-storepass', $pw,
    '-keypass', $pw,
    '-dname', $dn
)
Write-Host "Création du keystore..." -ForegroundColor Green
& keytool @keytoolArgs
if (-not (Test-Path $keyStorePath)) {
    Write-Error "Échec de la création du keystore."
    return
}
Write-Host "Keystore créé : $keyStorePath" -ForegroundColor Green

# Encoder en base64
$kb64File = Join-Path $RepoPath 'keystore_base64.txt'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keyStorePath)) | Out-File -Encoding ascii $kb64File
Write-Host "Keystore encodé en base64 -> $kb64File" -ForegroundColor Green

# Lire la base64 en mémoire
$keystore_base64 = Get-Content $kb64File -Raw

# Ajouter secrets GitHub via gh (repo par défaut, exécuter dans le repo)
Write-Host "Ajout des secrets GitHub (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) via gh..." -ForegroundColor Green
if ([string]::IsNullOrEmpty($ownerRepo)) {
    gh secret set KEYSTORE_BASE64 --body $keystore_base64
    gh secret set KEYSTORE_PASSWORD --body $pw
    gh secret set KEY_ALIAS --body $keyAlias
    gh secret set KEY_PASSWORD --body $pw
} else {
    gh secret set KEYSTORE_BASE64 --repo $ownerRepo --body $keystore_base64
    gh secret set KEYSTORE_PASSWORD --repo $ownerRepo --body $pw
    gh secret set KEY_ALIAS --repo $ownerRepo --body $keyAlias
    gh secret set KEY_PASSWORD --repo $ownerRepo --body $pw
}

Write-Host "Secrets ajoutés. (GH CLI --> vérifier via 'gh secret list')" -ForegroundColor Green

# Option : supprimer le keystore local si vous ne voulez plus le conserver
$removeLocal = Read-Host "Supprimer le keystore local (appli-release.jks) ? (y/N)"
if ($removeLocal -match '^[yY](es)?$') {
    Remove-Item $keyStorePath -Force
    Write-Host "Keystore local supprimé." -ForegroundColor Yellow
} else {
    Write-Host "Keystore local conservé : $keyStorePath" -ForegroundColor Yellow
}

# Instructions pour déclencher le workflow
Write-Host ""
Write-Host "Pour déclencher la CI (push sur main) :" -ForegroundColor Cyan
Write-Host "git add . ; git commit -m 'prepare ci' ; git push origin main" -ForegroundColor White
Write-Host ""
Write-Host "Pour créer une Release (upload APK) : (tag -> push)" -ForegroundColor Cyan
Write-Host "git tag v1.0.0 ; git push origin v1.0.0" -ForegroundColor White

Write-Host ""
Write-Host "Vérifier les secrets ajoutés :" -ForegroundColor Cyan
Write-Host "gh secret list" -ForegroundColor White
```

Security notes
- Never commit the `.jks` file to the repository.
- GitHub secrets are masked in logs. Do not print these values in scripts.
- If you delete the local keystore after adding `KEYSTORE_BASE64`, you can restore it inside CI by decoding the secret, but keep a secure backup if you need it locally.

Quick troubleshooting
- No APK found: ensure `./gradlew assembleRelease` produces an APK under `app/build/outputs/apk/release/`.
- Keystore not found: ensure `KEYSTORE_BASE64` is set and the workflow has access to it.
- Alias or password errors: verify `KEY_ALIAS`, `KEYSTORE_PASSWORD`, and `KEY_PASSWORD` values.


