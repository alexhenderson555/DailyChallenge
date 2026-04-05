# Вход через Google Play и сохранение прогресса

В приложении уже подключены **Play Games Services v2**: зависимость, инициализация в `Application`, модуль синхронизации и экран настроек (блок «Google Play»).

Чтобы всё заработало, нужно настроить проект в консоли и подставить свой APP ID.

---

## 0. APP ID в приложении

В **`app/src/main/res/values/strings.xml`** замени значение `games_app_id` на числовой идентификатор приложения из Play Console (Play Games Services → твой проект → Application ID):

```xml
<string name="games_app_id" translatable="false">ВАШ_ЧИСЛОВОЙ_ID</string>
```

Пока там стоит `0` — вход и облако работать не будут.

---

## 1. Настройка в Google Play Console

1. Открой [Google Play Console](https://play.google.com/console) → ваше приложение (или создай).
2. **Игра или приложение:**  
   Play Games Services доступны и для «не игровых» приложений — можно подключить к любому приложению.
3. **Play Games Services** → «Настроить» / «Setup»:
   - Создай или выбери проект в [Google Cloud Console](https://console.cloud.google.com/).
   - Включи **Play Games Services API** для этого проекта.
   - В разделе Play Games Services создай **OAuth 2.0 Client ID** для Android (указать package name и SHA-1 отпечаток signing-ключа).
4. В Play Console в настройках Play Games Services добавь своё приложение (package name, например `com.dailychallenge.app`).
5. Сохрани **идентификатор приложения** (Application ID / Project number) — он понадобится в коде и в `strings.xml`.

Документация: [Play Games Services — настройка](https://developer.android.com/games/pgs/quickstart).

---

## 2. Зависимость и инициализация в приложении

В `app/build.gradle.kts` добавь:

```kotlin
implementation("com.google.android.gms:play-services-games-v2:2.0.0")
```

В `Application` (например `DailyChallengeApp`) в `onCreate()`:

```kotlin
PlayGamesSdk.initialize(applicationContext)
```

В `AndroidManifest.xml` внутри `<application>` добавь мета-данные с ID проекта (числовой ID из Play Console):

```xml
<meta-data
    android:name="com.google.android.gms.games.UNITY_PLAY_GAMES_SDK_VERSION"
    android:value="0.12.01" />
```

Идентификатор приложения Play Games задаётся при первой инициализации через конфиг. Для v2 часто используют строковый ресурс, например в `res/values/strings.xml`:

```xml
<string name="app_id" translatable="false">ВАШ_ЧИСЛОВОЙ_ID_ИЗ_PLAY_CONSOLE</string>
```

и передают его в SDK при инициализации (см. актуальную документацию по Play Games v2 для твоего варианта инициализации).

---

## 3. Вход (Sign-in)

- При старте приложения можно вызвать **тихий вход** (без UI).
- Если не получилось — показать кнопку «Войти с Google Play» (например в Настройках).

Пример получения клиента и проверки входа:

```kotlin
val signInClient = PlayGames.getGamesSignInClient(activity)
signInClient.isAuthenticated
    .addOnCompleteListener { task ->
        if (task.isSuccessful && task.result?.isAuthenticated == true) {
            // Пользователь вошёл — можно грузить/сохранять снимки
        } else {
            // Показать кнопку «Войти с Google Play»
        }
    }
```

Ручной вход по нажатию кнопки:

```kotlin
signInClient.signIn()
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Вход выполнен
        }
    }
```

---

## 4. Сохранение и загрузка прогресса (Saved Games / Snapshots)

- **Прогресс** — это то, что у тебя сейчас в DataStore/файлах: записи дней (`DayRecord`), настройки (категории, тема, напоминание и т.д.).
- Нужно один раз придумать формат (например JSON) и:
  - при **сохранении**: собрать все нужные данные в один JSON, превратить в `ByteArray`, отправить в облако через **SnapshotsClient** (commit snapshot);
  - при **загрузке**: открыть последний снимок, прочитать `ByteArray`, распарсить JSON и применить к локальным DataStore/репозиторию (с возможностью «последний выигрыш» или merge по дате).

Документация по API снимков: [Saved games (Android)](https://developer.android.com/games/pgs/savedgames).

Клиент снимков получаешь так (после успешного входа):

```kotlin
val snapshotsClient = PlayGames.getSnapshotsClient(activity)
// Открыть снимок по имени (например "progress"), прочитать данные или создать новый и commit
```

---

## 5. Что сделать в коде приложения

1. Добавить зависимость `play-services-games-v2` и инициализацию `PlayGamesSdk` в `Application`.
2. В настройках добавить пункт «Войти с Google Play» и при успешном входе помечать, что пользователь «привязан» (например флаг в DataStore).
3. Реализовать модуль/хелпер, который:
   - сериализует прогресс (записи + настройки) в JSON;
   - по кнопке «Сохранить в облако» или после важных действий вызывает commit snapshot;
   - при первом входе или по кнопке «Загрузить прогресс» открывает снимок, парсит JSON и записывает в DataStore/репозиторий.
4. Решить политику конфликтов: если на другом устройстве уже есть более новый снимок — перезаписать локальные данные облаком или показать диалог «локально / из облака».

После этого вход будет через Google Play, а прогресс — сохраняться в облако и подтягиваться на других устройствах при входе тем же аккаунтом.
