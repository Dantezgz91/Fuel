# Guía del proyecto FuelTracker

Prototipo Android para seguir precios de gasolineras usando datos oficiales del **MITECO** (Ministerio de Industria).  
Esta guía está pensada si es tu **primera vez con Kotlin**: explica la estructura del repo y por dónde leer el código.

---

## 1. ¿Qué hace la app?

1. **Buscar municipios** y listar gasolineras de ese municipio (API REST).
2. **Seguir** gasolineras que te interesan (se guardan en SQLite con Room).
3. **Guardar historial de precios** (un snapshot por estación/combustible y día).
4. **Pantalla Inicio**: precio más barato entre las seguidas y recomendación por día de la semana.
5. **Análisis**: patrones por día de semana, día del mes, tendencia (sobre precios **diarios**).
6. **Ajustes**: sincronización automática ~1 vez al día (WorkManager).

---

## 2. Estructura de carpetas

```
Fuel/                          ← Proyecto Gradle (nombre: FuelTracker)
├── app/                       ← Módulo Android (toda la app vive aquí)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fueltracker/app/
│       │   ├── MainActivity.kt          ← Entrada de la UI
│       │   ├── FuelTrackerApp.kt        ← Application + Hilt + WorkManager
│       │   ├── data/                    ← API, base de datos, implementaciones
│       │   ├── domain/                  ← Modelos y lógica de negocio “pura”
│       │   ├── di/                      ← Inyección de dependencias (Hilt)
│       │   ├── ui/                      ← Pantallas Compose + ViewModels
│       │   └── worker/                  ← Tareas en segundo plano
│       └── res/                         ← Iconos, strings, temas XML
├── gradle/libs.versions.toml  ← Versiones de librerías
├── build.gradle.kts           ← Config raíz
└── settings.gradle.kts        ← Incluye el módulo ":app"
```

**Regla mental**: `ui` muestra cosas; `domain` decide reglas; `data` habla con internet y disco.

---

## 3. Arquitectura (capas)

```
┌─────────────────────────────────────────────────────────┐
│  UI (Compose)                                           │
│  HomeScreen, StationsScreen, AnalyticsScreen, Settings  │
└───────────────────────────┬─────────────────────────────┘
                            │ lee StateFlow, llama funciones
┌───────────────────────────▼─────────────────────────────┐
│  ViewModel (HomeViewModel, StationsViewModel, …)         │
│  Estado de pantalla + corrutinas                        │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Use cases (opcional, lógica reutilizable)              │
│  AnalyzePatternsUseCase, GetRecommendationUseCase       │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Repositorios (interfaces en domain, impl en data)      │
│  GasStationRepository, PriceRepository                  │
└───────────────┬─────────────────────────┬───────────────┘
                │                         │
     ┌──────────▼──────────┐   ┌──────────▼──────────┐
     │  MitecoApi (Retrofit)│   │  Room (SQLite)      │
     │  MITECO en la red    │   │  DAO + Entity       │
     └─────────────────────┘   └─────────────────────┘
```

**Por qué así**: la UI no llama a Retrofit directamente; si cambias la API o la BD, tocas sobre todo `data/`.

---

## 4. Kotlin mínimo para leer este proyecto

| Concepto | Ejemplo en el proyecto |
|----------|------------------------|
| `data class` | `GasStation`, `HomeUiState` — objeto con datos, `copy()` para actualizar |
| `enum class` | `FuelType` — lista fija de combustibles |
| `sealed class` | `Screen` en navegación — variantes conocidas (`Home`, `Stations`, …) |
| `object` | `DailyPriceAggregator` — utilidad sin instanciar |
| `interface` | `GasStationRepository` — contrato; la impl está en `data/` |
| `?` nullable | `String?` en DTOs de la API cuando el JSON puede venir vacío |
| `suspend fun` | Llamadas async (API, Room) que solo se usan en corrutinas |
| `Flow<T>` | Stream de datos (ej. lista de gasolineras seguidas que se actualiza sola) |
| `StateFlow` | Estado observable para Compose (`uiState`) |
| `Result<T>` | `success` / `failure` al buscar municipio o estaciones |
| `?.` / `?:` | Acceso seguro y valor por defecto: `name.orEmpty()` |
| `@Inject` | Hilt mete la dependencia en el constructor |

**Compose** (UI declarativa): las pantallas son funciones `@Composable` que describen interfaz; cuando cambia `uiState`, se redibuja.

---

## 5. Arranque de la app (orden de lectura recomendado)

1. `AndroidManifest.xml` — permisos, `MainActivity`, `FuelTrackerApp`.
2. `FuelTrackerApp.kt` — `@HiltAndroidApp`, configura WorkManager.
3. `MainActivity.kt` — tema, barra inferior, `NavHost` con 4 rutas.
4. `ui/navigation/AppNavigation.kt` — rutas `home`, `stations`, `analytics`, `settings`.
5. Elige una pantalla, por ejemplo **Gasolineras**:
   - `StationsScreen.kt` — UI (búsqueda municipio, lista, filtro).
   - `StationsViewModel.kt` — estado y llamadas al repositorio.

---

## 6. Pantallas y archivos clave

| Pestaña | Screen | ViewModel | Qué hace |
|---------|--------|-----------|----------|
| Inicio | `HomeScreen.kt` | `HomeViewModel.kt` | Seguidas, precio mínimo, recomendación |
| Gasolineras | `StationsScreen.kt` | `StationsViewModel.kt` | Municipios, buscar, seguir/dejar de seguir |
| Análisis | `AnalyticsScreen.kt` | `AnalyticsViewModel.kt` | Gráficos y patrones de precio |
| Ajustes | `SettingsScreen.kt` | `SettingsViewModel.kt` | Sync diaria, retención de datos |

**Tema visual**: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`.

---

## 7. Flujo de datos: ejemplo “Buscar Zaragoza y seguir una gasolinera”

```
Usuario escribe "Zaragoza" en StationsScreen
    → StationsViewModel.onMunicipalityQueryChange()
    → filtra lista en memoria (municipios ya cargados del repo)

Usuario elige municipio
    → StationsViewModel.selectMunicipality()
    → GasStationRepository.searchStationsByMunicipality(id)
         → MitecoApi.getStationsByMunicipality()  [Retrofit]
         → guarda estaciones en Room (GasStationDao)
         → guarda precios del día (PriceRecordDao, sin duplicar mismo día)
    → UI muestra searchResults

Usuario pulsa "Seguir"
    → trackStation(id)
    → GasStationDao marca isTracked = true

HomeScreen (otra pestaña)
    → getTrackedStations() como Flow
    → HomeViewModel combina con precios y AnalyzePatternsUseCase
```

---

## 8. Capa `data/` (detalle)

### Red — `data/remote/`

- **`MitecoApi.kt`**: interfaz Retrofit (GET municipios, GET estaciones por municipio).
- **`dto/`**: clases que coinciden con el JSON (`@SerializedName` de Gson).
- **`NetworkModule.kt`**: crea OkHttp + Retrofit (base URL MITECO, header `Accept: application/json`).

### Base de datos — `data/local/`

- **`entity/`**: tablas Room (`GasStationEntity`, `PriceRecordEntity`).
- **`dao/`**: consultas SQL (`GasStationDao`, `PriceRecordDao`).
- **`AppDatabase.kt`**: une entidades y DAOs.

### Repositorios — `data/repository/`

- **`GasStationRepositoryImpl`**: orquesta API + DAO (búsqueda, track, refresh precios, dedup diario).
- **`PriceRepositoryImpl`**: historial de precios y limpieza de datos viejos.

---

## 9. Capa `domain/`

- **`model/`**: tipos de negocio (`GasStation`, `PriceRecord`, `PriceAnalysis`, …).
- **`repository/`**: interfaces que usa la UI (sin saber de Retrofit ni Room).
- **`usecase/`**:
  - `DailyPriceAggregator` — agrupa precios por día natural (MITECO publica ~1 vez/día).
  - `AnalyzePatternsUseCase` — medias por lunes/martes…, tendencia.
  - `GetRecommendationUseCase` — mejor día para repostar.

La UI y los ViewModels **no deberían** importar `data.remote` ni `data.local` directamente.

---

## 10. Inyección de dependencias (Hilt)

Carpeta **`di/`**:

| Módulo | Función |
|--------|---------|
| `DatabaseModule` | Crea `AppDatabase` y DAOs |
| `NetworkModule` | Crea `MitecoApi` |
| `RepositoryModule` | Enlaza interfaces → implementaciones |

En ViewModels: `@HiltViewModel` + `@Inject constructor(...)`.  
En `MainActivity`: `@AndroidEntryPoint`.  
En tests o manual: no hace falta `new`; Hilt lo resuelve.

---

## 11. Segundo plano: `PriceSyncWorker`

- Clase en `worker/PriceSyncWorker.kt`.
- WorkManager la ejecuta según intervalo configurado en `SettingsViewModel` (24 h).
- Llama a `refreshPricesForTrackedStations()` para actualizar precios de municipios donde hay seguidas.

---

## 12. Gradle y dependencias principales

- **`settings.gradle.kts`**: solo módulo `:app`.
- **`app/build.gradle.kts`**: SDK 37, Compose, Room, Retrofit, Hilt, WorkManager, Vico (gráficos).
- **`gradle/libs.versions.toml`**: versiones centralizadas (AGP, Kotlin, Room, etc.).

Tareas útiles en Android Studio: **Build → Make Project**; ejecutar con configuración **app**.

---

## 13. API MITECO (referencia)

Base (en `NetworkModule`):

`https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/`

| Endpoint | Uso |
|----------|-----|
| `Listados/Municipios/` | Lista de municipios |
| `EstacionesTerrestres/FiltroMunicipio/{id}` | Gasolineras de un municipio |

Los precios vienen como **strings** con coma decimal; en código se normalizan a `Double`.

---

## 14. Decisiones de diseño del prototipo

- **Un precio por día** por estación/combustible: el análisis y la sync asumen publicación diaria oficial.
- **Claves únicas en listas Compose**: `tracked-…` / `search-…` + índice (evita crash por IDs duplicados).
- **Capitales primero** al buscar municipio (menos de 3 letras = lista corta de capitales).
- **Preservar `isTracked`** al volver a insertar estaciones del mismo municipio.

---

## 15. Ruta de aprendizaje sugerida (3–5 sesiones)

| Sesión | Lee / toca |
|--------|------------|
| 1 | `MainActivity`, navegación, `StationsScreen` + `StationsViewModel` |
| 2 | `GasStationRepositoryImpl`, `MitecoApi`, DTOs |
| 3 | Room: `AppDatabase`, entidades, DAOs |
| 4 | `HomeViewModel`, `AnalyzePatternsUseCase`, `DailyPriceAggregator` |
| 5 | `SettingsViewModel`, `PriceSyncWorker` |

**Experimento**: pon un breakpoint en `selectMunicipality()` y ejecuta en debug; mira la pila cuando llega la respuesta de la API.

---

## 16. Tests

En `app/src/test/` hay tests unitarios de agregación diaria y tendencia (`DailyPriceAggregatorTest`, `AnalyzePatternsTrendTest`).  
Ejecutar: clic derecho en la carpeta `test` → Run tests, o tarea Gradle `testDebugUnitTest`.

---

## 17. Glosario rápido

| Término | Significado |
|---------|-------------|
| Compose | Toolkit UI moderno de Android (Kotlin, no XML de layouts) |
| ViewModel | Sobrevive a rotaciones; guarda estado de pantalla |
| Room | ORM sobre SQLite |
| Retrofit | Cliente HTTP tipado |
| Corrutina | Concurrencia ligera (`launch`, `suspend`) |
| Flow | Secuencia asíncrona de valores en el tiempo |
| Hilt | Inyección de dependencias (DI) sobre Dagger |
| DTO | Objeto solo para transporte JSON |
| Entity | Fila de base de datos |

---

Si algo no cuadra con el código actual, busca el símbolo con **Navigate → Symbol** (`Ctrl+Alt+Shift+N` en Windows) en Android Studio: es la forma más rápida de no perderse en el proyecto.
