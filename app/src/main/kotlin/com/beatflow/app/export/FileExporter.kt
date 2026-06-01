package com.beatflow.app.export

import android.content.Context
import android.net.Uri
import com.beatflow.app.domain.model.HrvSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            val csvContent = buildCsv(session)
            zos.putNextEntry(ZipEntry("raw_data.csv"))
            zos.write(csvContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val jsonContent = buildJson(session)
            zos.putNextEntry(ZipEntry("session.json"))
            zos.write(jsonContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun buildCsv(session: HrvSession): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,hr,rr_ms,ecg_signal")
        session.records.forEach { record ->
            sb.appendLine(
                "${record.timestamp},${record.hr ?: ""},${record.rr ?: ""},${record.ecgSignal ?: ""}"
            )
        }
        return sb.toString()
    }

    private fun buildJson(session: HrvSession): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startFormatted = dateFormat.format(Date(session.startTime))
        val endFormatted = dateFormat.format(Date(session.endTime))

        val data = mapOf(
            "patient" to mapOf(
                "nombre" to session.patientData.nombre,
                "apellidos" to session.patientData.apellidos,
                "edad" to session.patientData.edad,
                "sexo" to session.patientData.sexo
            ),
            "session" to mapOf(
                "startTime" to startFormatted,
                "endTime" to endFormatted,
                "durationMs" to session.durationMs,
                "totalRecords" to session.records.size
            ),
            "metrics" to session.metrics?.let { metrics ->
                mapOf(
                    "timeDomain" to mapOf(
                        "meanHr" to metrics.meanHr,
                        "sdnn" to metrics.sdnn,
                        "rmssd" to metrics.rmssd,
                        "pnn50" to metrics.pnn50,
                        "pnn20" to metrics.pnn20,
                        "nn50" to metrics.nn50,
                        "nn20" to metrics.nn20,
                        "maxHr" to metrics.maxHr,
                        "minHr" to metrics.minHr
                    ),
                    "frequencyDomain" to mapOf(
                        "vlf" to metrics.vlf,
                        "lf" to metrics.lf,
                        "hf" to metrics.hf,
                        "totalPower" to metrics.totalPower,
                        "lfHfRatio" to metrics.lfHfRatio,
                        "lfNu" to metrics.lfNu,
                        "hfNu" to metrics.hfNu
                    ),
                    "nonLinear" to mapOf(
                        "sd1" to metrics.sd1,
                        "sd2" to metrics.sd2,
                        "sd1Sd2Ratio" to metrics.sd1Sd2Ratio
                    )
                )
            },
            "metadata" to mapOf(
                "app" to "BeatFlow",
                "version" to "1.0.0",
                "device" to "Polar H10"
            )
        )

        return json.encodeToString(data)
    }
}
