---
description: "Firebase Auth + CredentialManager в Money Manager: Google Sign-In, 4-модульная структура (core:auth, domain:auth, data:auth, presentation:auth), Coil 3 для фото профиля"
---

# Firebase Auth & Google Sign-In

## Context

Опциональная аутентификация через Google Sign-In реализована с помощью Firebase Auth и Android CredentialManager API. Приложение работает полностью без входа — авторизация только добавляет возможности (синхронизация, профиль).

**4 модуля:**
- `core:auth` — CredentialManager wrapper, GoogleSignInHelper
- `domain:auth` — AuthRepository interface + UseCases
- `data:auth` — FirebaseAuthRepositoryImpl + Hilt DI
- `presentation:auth` — SignInRoute, SignInScreen, SignInViewModel, SignInState

**Ключевые файлы:**
- `core/auth/src/.../CredentialManagerHelper.kt` — обёртка CredentialManager
- `domain/auth/src/.../repository/AuthRepository.kt` — интерфейс репозитория
- `data/auth/src/.../repository/FirebaseAuthRepositoryImpl.kt` — реализация
- `presentation/auth/src/.../ui/SignInViewModel.kt` — ViewModel
- `presentation/auth/src/.../ui/SignInScreen.kt` — UI

## Firebase Auth API (BOM 34+)

В Firebase BOM 34+ все `ktx`-артефакты объединены с основными. Используй без суффикса `ktx`:

```kotlin
// ПРАВИЛЬНО — merged API (BOM 34+)
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.GoogleAuthProvider

val auth = Firebase.auth
val currentUser = auth.currentUser  // FirebaseUser?

// Вход с Google credential
val authCredential = GoogleAuthProvider.getCredential(idToken, null)
Firebase.auth.signInWithCredential(authCredential).await()

// Выход
Firebase.auth.signOut()
```

## CredentialManager API

Используй `CredentialManager` (НЕ устаревший `GoogleSignInClient`).

**Зависимости (в `libs.versions.toml`):**
```toml
credentials = "1.3.0"
googleid = "1.1.1"
```

**Получение Google ID Token:**
```kotlin
suspend fun signInWithGoogle(context: Context): String {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)  // показывать все аккаунты
        .setServerClientId(getWebClientId(context))
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val result = credentialManager.getCredential(context, request)
    val credential = result.credential as? GoogleIdTokenCredential
    return credential?.idToken ?: throw IllegalStateException("No ID token")
}
```

**Выход (очистка credential state):**
```kotlin
suspend fun signOut(context: Context) {
    val credentialManager = CredentialManager.create(context)
    credentialManager.clearCredentialState(ClearCredentialStateRequest())
    Firebase.auth.signOut()
}
```

## Web Client ID — runtime resolution

НЕ хардкодь Web Client ID в коде. Он генерируется Firebase и доступен как строковый ресурс:

```kotlin
fun getWebClientId(context: Context): String {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName,
    )
    return context.getString(resId)
}
```

## AuthRepository

```kotlin
// domain/auth/.../repository/AuthRepository.kt
interface AuthRepository {
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun signInWithGoogle(context: Context): Result<AuthUser>
    suspend fun signOut(context: Context)
}

// Модель пользователя (в core:model)
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)
```

## UseCases

```kotlin
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.observeAuthState()
}

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(context: Context): Result<AuthUser> =
        repository.signInWithGoogle(context)
}

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(context: Context) = repository.signOut(context)
}
```

## Hilt DI (data:auth)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepositoryImpl): AuthRepository
}
```

## ViewModel + State

```kotlin
data class SignInState(
    val currentUser: AuthUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        observeAuthStateUseCase()
            .onEach { user -> _state.update { it.copy(currentUser = user) } }
            .launchIn(viewModelScope)
    }

    fun signIn(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithGoogleUseCase(context)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch { signOutUseCase(context) }
    }
}
```

## Coil 3 для фото профиля

Группа изменилась с `io.coil-kt` на `io.coil-kt.coil3`. Используй правильные артефакты:

```toml
# libs.versions.toml
coil3 = "3.3.0"

[libraries]
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil3" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil3" }
```

```kotlin
// Отображение фото профиля
AsyncImage(
    model = state.currentUser?.photoUrl,
    contentDescription = "Фото профиля",
    modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .testTag("signIn:userAvatar"),
)
```

## Navigation Integration

```kotlin
// Destinations.kt
@Serializable data object SignIn

// MoneyManagerNavHost.kt
composable<SignIn> {
    SignInRoute(onNavigateBack = { navController.popBackStack() })
}

// Точка входа: Settings → "Аккаунт" row → navigate(SignIn)
```

## testTag naming

- `signIn:googleButton` — кнопка "Войти через Google"
- `signIn:signOutButton` — кнопка "Выйти"
- `signIn:userAvatar` — фото профиля
- `signIn:userName` — имя пользователя
- `signIn:loadingIndicator` — индикатор загрузки

## Quality Bar

- Наблюдай за состоянием аутентификации через `Flow` — не разовый запрос
- Обрабатывай все состояния: loading, signed-in, signed-out, error
- Auth опциональна — приложение должно работать без входа без крашей
- Очищай credential state при выходе через `clearCredentialState()`
- `currentUser` = `null` не является ошибкой — это анонимное использование

## Anti-patterns

- НЕ используй `ktx` суффиксы для Firebase — в BOM 34+ они объединены
- НЕ используй deprecated `GoogleSignInClient` — используй `CredentialManager`
- НЕ хардкодь Web Client ID — резолви через `getIdentifier()` в runtime
- НЕ используй старый Coil 2 (`io.coil-kt:coil-compose`) — группа изменилась на `io.coil-kt.coil3`
- НЕ блокируй функционал приложения если пользователь не вошёл
- НЕ храни ID-токен в памяти дольше чем нужно для одной операции
