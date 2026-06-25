package com.beatflow.app.export

import android.content.Context
import android.net.Uri
import com.beatflow.app.domain.model.HrvSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun buildFilename(session: HrvSession): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date(session.startTime))
        val nombre = session.patientData.nombre
        val apellidos = session.patientData.apellidos
        return "${timestamp}_${nombre}_${apellidos}.hrv"
    }

    fun exportSessionToUri(session: HrvSession, uri: Uri): Result<Uri> = runCatching {
        val bytes = buildZipBytes(session)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
        } ?: throw IllegalStateException("No se pudo abrir el archivo")
        uri
    }

    private fun buildZipBytes(session: HrvSession): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("hr_data.csv"))
            zos.write(buildHrCsv(session).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("rr_data.txt"))
            zos.write(buildRrTxt(session).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("ecg_data.csv"))
            zos.write(buildEcgCsv(session).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("session.json"))
            zos.write(buildJson(session).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun buildHrCsv(session: HrvSession): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,hr")
        session.records.forEach { record ->
            record.hr?.let { sb.appendLine("${record.timestamp},$it") }
        }
        return sb.toString()
    }

    private fun buildRrTxt(session: HrvSession): String {
        val sb = StringBuilder()
        session.records.forEach { record ->
            record.rr?.let { sb.appendLine("%.0f".format(it)) }
        }
        return sb.toString()
    }

    private fun buildEcgCsv(session: HrvSession): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,ecg_signal")
        session.records.forEach { record ->
            record.ecgSignal?.let { sb.appendLine("${record.timestamp},$it") }
        }
        return sb.toString()
    }

    private fun buildJson(session: HrvSession): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startFormatted = dateFormat.format(Date(session.startTime))
        val endFormatted = dateFormat.format(Date(session.endTime))

        val data = buildJsonObject {
            putJsonObject("patient") {
                put("nombre", session.patientData.nombre)
                put("apellidos", session.patientData.apellidos)
                put("edad", session.patientData.edad)
                put("sexo", session.patientData.sexo)
            }
            putJsonObject("session") {
                put("startTime", startFormatted)
                put("endTime", endFormatted)
                put("durationMs", session.durationMs)
                put("totalRecords", session.records.size)
            }
            session.protocolConfig?.let { config ->
                putJsonObject("protocol") {
                    put("type", config.type)
                    put("totalTimeSecs", config.totalTimeSecs)
                    config.inspirationSecs?.let { put("inspirationSecs", it) }
                    config.expirationSecs?.let { put("expirationSecs", it) }
                    config.standUpSecs?.let { put("standUpSecs", it) }
                }
            }
            if (session.metrics != null) {
                putJsonObject("metrics") {
                    putJsonObject("timeDomain") {
                        put("meanHr", session.metrics.meanHr)
                        put("sdnn", session.metrics.sdnn)
                        put("rmssd", session.metrics.rmssd)
                        put("pnn50", session.metrics.pnn50)
                        put("pnn20", session.metrics.pnn20)
                        put("nn50", session.metrics.nn50)
                        put("nn20", session.metrics.nn20)
                        put("maxHr", session.metrics.maxHr)
                        put("minHr", session.metrics.minHr)
                    }
                    putJsonObject("frequencyDomain") {
                        put("vlf", session.metrics.vlf)
                        put("lf", session.metrics.lf)
                        put("hf", session.metrics.hf)
                        put("totalPower", session.metrics.totalPower)
                        put("lfHfRatio", session.metrics.lfHfRatio)
                        put("lfNu", session.metrics.lfNu)
                        put("hfNu", session.metrics.hfNu)
                    }
                    putJsonObject("nonLinear") {
                        put("sd1", session.metrics.sd1)
                        put("sd2", session.metrics.sd2)
                        put("sd1Sd2Ratio", session.metrics.sd1Sd2Ratio)
                    }
                }
            }
            putJsonObject("metadata") {
                put("app", "BeatFlow")
                put("version", "1.0.0")
                put("device", "Polar H10")
            }
        }

        return json.encodeToString(data)
    }
}
