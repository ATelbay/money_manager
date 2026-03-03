# CI/CD Setup Guide

## GitHub Repository Secrets

Для работы пайплайна нужно настроить следующие secrets в GitHub:
**Settings → Secrets and variables → Actions**

### Common CI / Debug Build

| Secret | Описание |
|--------|----------|
| `GOOGLE_SERVICES_JSON` | `app/google-services.json` в base64 для CI, `QA Build` и debug-сборок |

**Подготовка:**
1. Возьми локальный `app/google-services.json`
2. Закодируй файл в base64
3. Сохрани результат в secret `GOOGLE_SERVICES_JSON`

### Signing (для Release сборок)

| Secret | Описание |
|--------|----------|
| `KEYSTORE_BASE64` | Keystore в base64: `base64 -i keystore.jks` |
| `SIGNING_KEY_ALIAS` | Alias ключа |
| `SIGNING_KEY_PASSWORD` | Пароль ключа |
| `SIGNING_STORE_PASSWORD` | Пароль keystore |

### Firebase App Distribution

| Secret | Описание |
|--------|----------|
| `FIREBASE_APP_ID` | App ID из Firebase Console |
| `FIREBASE_SERVICE_CREDENTIALS` | JSON service account (весь файл) |

**Получение credentials:**
1. Firebase Console → Project Settings → Service accounts
2. Generate new private key
3. Скопировать содержимое JSON файла

### Google Play Store

| Secret | Описание |
|--------|----------|
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON service account для Play Console |

**Получение credentials:**
1. Google Cloud Console → IAM → Service Accounts
2. Создать account с ролью "Service Account User"
3. Play Console → Setup → API access → Link service account
4. Выдать права "Release to production"

## Создание Keystore

```bash
keytool -genkey -v -keystore money_manager-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias money_manager
```

**Важно:** сохрани keystore и пароли в надёжном месте! Без них не сможешь обновлять приложение в Play Store.

## Структура веток

```
main          ← основная и protected ветка
  ↑
feature/*     ← обычные ручные feature branches
ralph/*       ← ветки автономных Ralph-run'ов
```

## Workflow

| Событие | Что происходит |
|---------|----------------|
| PR → main | `Compile Debug Kotlin` + `Lint & Analysis` + `Unit Tests` |
| Manual `QA Build` | Debug APK build + artifact upload |
| Manual `Build Debug APK` | Debug APK build + artifact upload |
| Tag `v*` | Release pipeline: release artifacts + optional Firebase / Play / GitHub Release |

## Branch Protection (recommended)

Для `main` стоит включить branch protection и сделать required checks:
1. `Compile Debug Kotlin`
2. `Lint & Analysis`
3. `Unit Tests`

Это особенно важно для `ralph/*` PR: `--remote-run` считает GitHub CI основным gate после каждой итерации.

## Продвижение в Play Store

После деплоя в internal track:
1. Play Console → Testing → Internal testing
2. Promote to Closed/Open testing
3. После тестирования → Promote to Production

## Первый релиз

Перед первым деплоем через CI нужно:
1. Создать приложение в Play Console вручную
2. Загрузить первый AAB вручную
3. Заполнить Store listing, Content rating и т.д.
4. После этого CI сможет загружать обновления

## Локальная проверка workflow

```bash
# Установить act (https://github.com/nektos/act)
brew install act

# Прогнать compile gate локально
act pull_request --job compile
```

## Полезные команды

```bash
# Конвертировать keystore в base64
base64 -i money_manager-release.jks | pbcopy  # macOS
base64 -w 0 money_manager         # Linux

# Проверить keystore
keytool -list -v -keystore money_manager-release.jks

# Получить SHA-1 для Firebase
keytool -list -v -keystore money_manager-release.jks | grep SHA1
```
