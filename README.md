# BeatFlow

Aplicación Android nativa en Kotlin para medir y analizar la **Variabilidad de la Frecuencia Cardíaca (HRV)** utilizando un sensor **Polar H10** (o dispositivos Polar compatibles) mediante Bluetooth Low Energy (BLE).

## Características

- **Conexión BLE** con dispositivos Polar H10 — escaneo, conexión y streaming de HR/RR/ECG
- **Grabación en tiempo real** con gráfico de frecuencia cardíaca y cronómetro
- **Captura de datos del paciente** (nombre, edad, sexo)
- **Cálculo completo de métricas HRV:**
  - Dominio del tiempo: Mean HR, SDNN, RMSSD, pNN50, Max HR, Min HR
  - Dominio de la frecuencia: LF, HF, LF/HF (FFT personalizado)
  - No lineales: SD1, SD2 (Poincaré plot)
- **Exportación** a archivo `.hrv` (ZIP conteniendo CSV + JSON)
- **100% offline** — sin conexión a internet, sin servidores, sin telemetría

## Arquitectura

Clean Architecture + MVVM:

```
Presentation (Compose UI + ViewModels)
    |
Domain (modelos + HrvCalculator)
    |
Data (Room DB + DAOs, PolarManager BLE)
    |
External (Polar SDK, File I/O)
```

- **Hilt** para inyección de dependencias
- **Jetpack Compose** + **Material 3** para la UI
- **Room** para persistencia local de sesiones
- **Polar BLE SDK** v6.16.1 para comunicación con el sensor
- **MPAndroidChart** para gráficos en tiempo real

## Tecnologías

| Categoría | Librería |
|---|---|
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Dagger Hilt 2.52 |
| BLE | Polar BLE SDK 6.16.1 |
| Persistencia | Room 2.6.1 |
| Gráficos | MPAndroidChart 3.1.0 |
| Async | Coroutines 1.9.0 + RxJava 3.1.8 |
| Serialización | kotlinx-serialization-json 1.7.3 |
| Min SDK | 26 |
| Target SDK | 35 |

## Requisitos

- Dispositivo Android 8.0+ (API 26)
- Sensor Polar H10 (o dispositivo Polar compatible)
- Bluetooth 4.0+ (BLE)

## Permisos

- **Android 12+:** `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- **Android < 12:** `ACCESS_FINE_LOCATION`

## Compilación

```bash
./gradlew assembleDebug
```

El proyecto usa Gradle 8.9 y el plugin de Android Gradle 8.5.2.

## Estructura del proyecto

```
app/
├── src/main/kotlin/com/beatflow/app/
│   ├── bluetooth/PolarManager.kt       # SDK Polar BLE
│   ├── data/
│   │   ├── local/                       # Room DB, DAOs, entidades
│   │   └── repository/                  # SessionRepository
│   ├── domain/
│   │   ├── model/                       # HrvSession, HrvMetrics, PatientData, RawRecord
│   │   └── HrvCalculator.kt            # Cálculos HRV con FFT
│   ├── export/FileExporter.kt          # Exportación a .hrv (CSV+JSON+ZIP)
│   ├── presentation/
│   │   ├── main/                        # Pantalla principal (BLE)
│   │   ├── measurement/                 # Pantalla de medición
│   │   ├── patient/                     # Formulario de paciente
│   │   ├── report/                      # Reporte de métricas HRV
│   │   ├── navigation/NavGraph.kt      # Navegación
│   │   └── theme/                       # Tema Material 3
│   ├── MainActivity.kt
│   └── BeatFlowApplication.kt
├── build.gradle.kts
└── proguard-rules.pro
```

## Formato de exportación (.hrv)

Archivo ZIP que contiene:

### `raw_data.csv`

Todas las muestras de sensores en formato CSV con las siguientes columnas:

| Columna | Descripción | Ejemplo |
|---|---|---|
| `timestamp` | Unix timestamp en milisegundos (epoch) | `1717200000000` |
| `hr` | Frecuencia cardíaca instantánea en BPM | `72` |
| `rr_ms` | Intervalo RR en milisegundos | `833.0` |
| `ecg_signal` | Muestra de ECG en microvoltios (μV) | `-0.245` |

```
timestamp,hr,rr_ms,ecg_signal
1717200000000,72,833.0,-0.245
1717200000000,72,,0.112
1717200000000,,,0.087
```

- Las filas con HR llevan también RR (un valor por latido)
- Las filas con ECG pueden tener múltiples muestras por latido (130 Hz)
- Las celdas vacías indican que ese sensor no generó dato en esa muestra

### `session.json`

Datos del paciente, metadatos de sesión y métricas HRV en formato JSON.

## Privacidad

BeatFlow no requiere conexión a internet. Todos los datos se almacenan localmente en el dispositivo y en el almacenamiento externo. No se recopila ni transmite información alguna.

## Licencia

Uso educativo y clínico. Basado en el [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).
