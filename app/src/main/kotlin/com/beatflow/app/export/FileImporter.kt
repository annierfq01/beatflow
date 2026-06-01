package com.beatflow.app.export

import android.content.Context
import android.net.Uri
import com.beatflow.app.domain.model.HrvMetrics
import com.beatflow.app.domain.model.PatientData
import com.beatflow.app.domain.model.RawRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedSession(
    val patientData: PatientData,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val metrics: HrvMetrics?,
    val records: List<RawRecord>
)

@Singleton
class FileImporter @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun importSession(context: Context, uri: Uri): Result<ImportedSession> = runCatching {
        val zipBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer el archivo")

        var csvContent: String? = null
        var jsonContent: String? = null

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "raw_data.csv" -> csvContent = zis.bufferedReader().use { it.readText() }
                    "session.json" -> jsonContent = zis.bufferedReader().use { it.readText() }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val csv = csvContent ?: throw IllegalStateException("No se encontró raw_data.csv en el archivo")
        val sesJson = jsonContent ?: throw IllegalStateException("No se encontró session.json en el archivo")

        parseSession(csv, sesJson)
    }

    private fun parseSession(csvContent: String, jsonContent: String): ImportedSession {
        val root = json.parseToJsonElement(jsonContent).jsonObject

        val patientObj = root["patient"]?.jsonObject
            ?: throw IllegalStateException("Datos de paciente no encontrados")
        val sessionObj = root["session"]?.jsonObject
            ?: throw IllegalStateException("Datos de sesión no encontrados")
        val metricsObj = root["metrics"]?.jsonObject

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val patientData = PatientData(
            nombre = patientObj["nombre"]?.jsonPrimitive?.content ?: "",
            apellidos = patientObj["apellidos"]?.jsonPrimitive?.content ?: "",
            edad = patientObj["edad"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            sexo = patientObj["sexo"]?.jsonPrimitive?.content ?: ""
        )

        val startTime = dateFormat.parse(
            sessionObj["startTime"]?.jsonPrimitive?.content ?: ""
        )?.time ?: 0L
        val endTime = dateFormat.parse(
            sessionObj["endTime"]?.jsonPrimitive?.content ?: ""
        )?.time ?: 0L
        val durationMs = sessionObj["durationMs"]?.jsonPrimitive?.long ?: 0L

        val metrics = metricsObj?.let { parseMetrics(it) }

        val records = parseCsv(csvContent)

        return ImportedSession(
            patientData = patientData,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs,
            metrics = metrics,
            records = records
        )
    }

    private fun parseMetrics(obj: JsonObject): HrvMetrics {
        val td = obj["timeDomain"]?.jsonObject ?: return defaultMetrics()
        val fd = obj["frequencyDomain"]?.jsonObject
        val nl = obj["nonLinear"]?.jsonObject

        return HrvMetrics(
            meanHr = td["meanHr"]?.jsonPrimitive?.double ?: 0.0,
            sdnn = td["sdnn"]?.jsonPrimitive?.double ?: 0.0,
            rmssd = td["rmssd"]?.jsonPrimitive?.double ?: 0.0,
            pnn50 = td["pnn50"]?.jsonPrimitive?.double ?: 0.0,
            pnn20 = td["pnn20"]?.jsonPrimitive?.double ?: 0.0,
            nn50 = td["nn50"]?.jsonPrimitive?.double ?: 0.0,
            nn20 = td["nn20"]?.jsonPrimitive?.double ?: 0.0,
            maxHr = td["maxHr"]?.jsonPrimitive?.double ?: 0.0,
            minHr = td["minHr"]?.jsonPrimitive?.double ?: 0.0,
            vlf = fd?.get("vlf")?.jsonPrimitive?.double ?: 0.0,
            lf = fd?.get("lf")?.jsonPrimitive?.double ?: 0.0,
            hf = fd?.get("hf")?.jsonPrimitive?.double ?: 0.0,
            totalPower = fd?.get("totalPower")?.jsonPrimitive?.double ?: 0.0,
            lfHfRatio = fd?.get("lfHfRatio")?.jsonPrimitive?.double ?: 0.0,
            lfNu = fd?.get("lfNu")?.jsonPrimitive?.double ?: 0.0,
            hfNu = fd?.get("hfNu")?.jsonPrimitive?.double ?: 0.0,
            sd1 = nl?.get("sd1")?.jsonPrimitive?.double ?: 0.0,
            sd2 = nl?.get("sd2")?.jsonPrimitive?.double ?: 0.0,
            sd1Sd2Ratio = nl?.get("sd1Sd2Ratio")?.jsonPrimitive?.double ?: 0.0
        )
    }

    private fun defaultMetrics() = HrvMetrics(
        meanHr = 0.0, sdnn = 0.0, rmssd = 0.0,
        pnn50 = 0.0, pnn20 = 0.0, nn50 = 0.0, nn20 = 0.0,
        maxHr = 0.0, minHr = 0.0,
        vlf = 0.0, lf = 0.0, hf = 0.0, totalPower = 0.0,
        lfHfRatio = 0.0, lfNu = 0.0, hfNu = 0.0,
        sd1 = 0.0, sd2 = 0.0, sd1Sd2Ratio = 0.0
    )

    private fun parseCsv(csv: String): List<RawRecord> {
        val lines = csv.lines().drop(1).filter { it.isNotBlank() }
        return lines.map { line ->
            val parts = line.split(",")
            RawRecord(
                timestamp = parts.getOrNull(0)?.toLongOrNull() ?: 0L,
                hr = parts.getOrNull(1)?.toDoubleOrNull()?.toInt(),
                rr = parts.getOrNull(2)?.toDoubleOrNull(),
                ecgSignal = parts.getOrNull(3)?.toDoubleOrNull()
            )
        }
    }
}
