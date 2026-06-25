# BeatFlow

Aplicación Android nativa en Kotlin para medir y analizar la **Variabilidad de la Frecuencia Cardíaca (HRV)** utilizando un sensor **Polar H10** (o dispositivos Polar compatibles) mediante Bluetooth Low Energy (BLE).

## Características

- **Conexión BLE** con dispositivos Polar H10 — escaneo, conexión y streaming de HR/RR/ECG
- **Tres protocolos de medición:**
  - **Reposo/Basal**: Medición en reposo con cuenta regresiva
  - **Test de Respiración**: Respiración guiada con círculo animado (inspiración/espiración)
  - **Test Ortostático**: Registro continuo con cambio postural, círculo con tiempo para ponerse de pie
- **Captura de datos del paciente** (nombre, edad, sexo)
- **Cálculo completo de métricas HRV:**
  - Dominio del tiempo: Mean HR, SDNN, RMSSD, pNN50, Max HR, Min HR
  - Dominio de la frecuencia: LF, HF, LF/HF (FFT personalizado)
  - No lineales: SD1, SD2 (Poincaré plot)
- **Exportación** a archivo `.hrv` (ZIP conteniendo CSV + TXT + JSON)
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
│   ├── export/
│   │   ├── FileExporter.kt              # Exportación a .hrv (CSV+JSON+ZIP)
│   │   └── FileImporter.kt              # Importación de .hrv
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

El archivo `.hrv` es un **ZIP** que contiene cuatro archivos: CSVs para datos de HR y ECG, un TXT para intervalos RR, y un JSON con metadatos, configuración del protocolo y métricas.

### Estructura del ZIP

```
archivo.hrv
├── hr_data.csv        # Datos de frecuencia cardíaca
├── rr_data.txt        # Intervalos RR (texto plano, un valor por línea)
├── ecg_data.csv       # Señal de ECG
└── session.json       # Metadatos, paciente, protocolo y métricas HRV
```

### `hr_data.csv`

Frecuencia cardíaca instantánea en BPM por cada latido detectado.

| Columna | Descripción | Ejemplo |
|---|---|---|
| `timestamp` | Unix timestamp en milisegundos (epoch) | `1717200000000` |
| `hr` | Frecuencia cardíaca en BPM | `72` |

```
timestamp,hr
1717200000000,72
1717200000500,73
1717200001000,71
```

### `rr_data.txt`

Intervalo RR (tiempo entre latidos consecutivos) en milisegundos. Formato de texto plano, un valor por línea (compatible con BeatFlowPC).

```
833.0
820.5
845.2
812.0
```

### `ecg_data.csv`

Señal de ECG cruda en microvoltios (μV) muestreada a 130 Hz.

| Columna | Descripción | Ejemplo |
|---|---|---|
| `timestamp` | Unix timestamp en milisegundos (epoch) | `1717200000000` |
| `ecg_signal` | Tensión en microvoltios (μV) | `-0.245` |

```
timestamp,ecg_signal
1717200000000,-0.245
1717200000007,0.112
1717200000015,0.087
```

### `session.json`

Objeto JSON con los siguientes campos:

#### Ejemplo: Protocolo Basal (Reposo)

```json
{
  "patient": {
    "nombre": "Juan",
    "apellidos": "Pérez García",
    "edad": 35,
    "sexo": "Masculino"
  },
  "session": {
    "startTime": "2025-06-01 10:30:00",
    "endTime": "2025-06-01 10:35:00",
    "durationMs": 300000,
    "totalRecords": 1250
  },
  "protocol": {
    "type": "basal",
    "totalTimeSecs": 300
  },
  "metrics": {
    "timeDomain": {
      "meanHr": 72.5,
      "sdnn": 48.3,
      "rmssd": 32.1,
      "pnn50": 12.5,
      "pnn20": 35.2,
      "nn50": 8.0,
      "nn20": 22.0,
      "maxHr": 145.0,
      "minHr": 55.0
    },
    "frequencyDomain": {
      "vlf": 1200.5,
      "lf": 850.3,
      "hf": 420.1,
      "totalPower": 2470.9,
      "lfHfRatio": 2.02,
      "lfNu": 66.9,
      "hfNu": 33.1
    },
    "nonLinear": {
      "sd1": 22.7,
      "sd2": 62.5,
      "sd1Sd2Ratio": 0.36
    }
  },
  "metadata": {
    "app": "BeatFlow",
    "version": "1.0.0",
    "device": "Polar H10"
  }
}
```

#### Ejemplo: Protocolo Test Respiración

```json
{
  "protocol": {
    "type": "respiracion",
    "totalTimeSecs": 300,
    "inspirationSecs": 5,
    "expirationSecs": 5
  }
}
```

#### Ejemplo: Protocolo Test Ortostático

```json
{
  "protocol": {
    "type": "ortostatico",
    "totalTimeSecs": 480,
    "standUpSecs": 120
  }
}
```

#### Campos completos

| Campo | Tipo | Descripción |
|---|---|---|
| `patient.nombre` | string | Nombre del paciente |
| `patient.apellidos` | string | Apellidos del paciente |
| `patient.edad` | int | Edad en años |
| `patient.sexo` | string | Sexo del paciente |
| `session.startTime` | string | Inicio de la sesión (yyyy-MM-dd HH:mm:ss) |
| `session.endTime` | string | Fin de la sesión (yyyy-MM-dd HH:mm:ss) |
| `session.durationMs` | long | Duración total en milisegundos |
| `session.totalRecords` | int | Número total de registros (HR+RR+ECG combinados) |
| `protocol.type` | string | Tipo de protocolo: `"basal"`, `"respiracion"` u `"ortostatico"` |
| `protocol.totalTimeSecs` | int | Duración total configurada en segundos |
| `protocol.inspirationSecs` | int | (Solo respiración) Segundos de inspiración |
| `protocol.expirationSecs` | int | (Solo respiración) Segundos de espiración |
| `protocol.standUpSecs` | int | (Solo ortostático) Segundo en que el sujeto se pone de pie |
| `metrics.timeDomain.*` | double | Métricas de dominio temporal |
| `metrics.frequencyDomain.*` | double | Métricas de dominio frecuencial (FFT) |
| `metrics.nonLinear.*` | double | Métricas no lineales (Poincaré) |
| `metadata.*` | string | Metadatos de la aplicación |

El campo `protocol` está presente solo cuando se usó un protocolo de medición.
El campo `metrics` es opcional: puede estar ausente si no hay suficientes datos de RR (< 30 latidos) para el cálculo HRV.

## Privacidad

BeatFlow no requiere conexión a internet. Todos los datos se almacenan localmente en el dispositivo y en el almacenamiento externo. No se recopila ni transmite información alguna.

## Licencia

Uso educativo y clínico. Basado en el [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).
