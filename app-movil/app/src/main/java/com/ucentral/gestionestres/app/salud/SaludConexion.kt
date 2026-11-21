package com.ucentral.gestionestres.app.salud

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * Puente entre Java y Health Connect (HU002, HU026).
 *
 * La API de Health Connect usa funciones "suspend" de Kotlin, que no se pueden
 * llamar directamente desde Java. Este objeto las envuelve en CompletableFuture
 * para que el resto de la app quede en Java. Es el único archivo Kotlin del proyecto.
 */
object SaludConexion {

    const val PAQUETE_HEALTH_CONNECT = "com.google.android.apps.healthdata"

    @JvmField
    val PERMISO_FC: String = HealthPermission.getReadPermission(HeartRateRecord::class)

    @JvmField
    val PERMISO_HRV: String = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    /** HU026: permite leer cada 15 minutos con la app cerrada. */
    const val PERMISO_SEGUNDO_PLANO = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Promedios de una ventana de 15 minutos: es lo único que sale del teléfono (no la señal cruda). */
    data class Ventana(
        val inicio: Instant,
        val fin: Instant,
        val fcPromedio: Double,
        val hrvPromedio: Double?,
        val numLecturas: Int
    )

    @JvmStatic
    fun disponible(contexto: Context): Boolean =
        HealthConnectClient.getSdkStatus(contexto, PAQUETE_HEALTH_CONNECT) == HealthConnectClient.SDK_AVAILABLE

    /** Solo lectura de FC y HRV, más el permiso de segundo plano (HU002). */
    @JvmStatic
    fun permisosSolicitados(): Set<String> = setOf(PERMISO_FC, PERMISO_HRV, PERMISO_SEGUNDO_PLANO)

    /** Contrato para abrir la pantalla de permisos de Health Connect con registerForActivityResult. */
    @JvmStatic
    fun contratoPermisos(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    @JvmStatic
    fun permisosConcedidos(contexto: Context): CompletableFuture<Set<String>> = alcance.future {
        HealthConnectClient.getOrCreate(contexto).permissionController.getGrantedPermissions()
    }

    /** HU002: al desvincular, la app renuncia a todos sus permisos de Health Connect. */
    @JvmStatic
    fun revocarPermisos(contexto: Context): CompletableFuture<Boolean> = alcance.future {
        HealthConnectClient.getOrCreate(contexto).permissionController.revokeAllPermissions()
        true
    }

    /**
     * Lee de Health Connect la FC y la HRV entre [inicio] y [fin] y devuelve sus promedios.
     * Devuelve null si no hay lecturas de FC en ese periodo (HU026: no se envía ventana).
     */
    @JvmStatic
    fun leerVentana(contexto: Context, inicio: Instant, fin: Instant): CompletableFuture<Ventana?> =
        alcance.future {
            val cliente = HealthConnectClient.getOrCreate(contexto)
            val rango = TimeRangeFilter.between(inicio, fin)

            val fc = cliente.readRecords(ReadRecordsRequest(HeartRateRecord::class, rango))
                .records
                .flatMap { it.samples }
                .filter { !it.time.isBefore(inicio) && it.time.isBefore(fin) }
                .map { it.beatsPerMinute.toDouble() }

            if (fc.isEmpty()) {
                return@future null
            }

            // Si el usuario no concedió la HRV o el wearable no la entrega, la ventana va sin HRV.
            val hrv = try {
                cliente.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, rango))
                    .records
                    .map { it.heartRateVariabilityMillis }
            } catch (e: SecurityException) {
                emptyList()
            }

            Ventana(inicio, fin, fc.average(), if (hrv.isEmpty()) null else hrv.average(), fc.size)
        }
}
