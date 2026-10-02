package ru.finswimmer.steppy.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectRepository(private val context: Context) {
    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun availability(): Int = HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE)

    fun clientOrNull(): HealthConnectClient? =
        if (availability() == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(): Boolean = clientOrNull()?.permissionController
        ?.getGrantedPermissions()?.containsAll(permissions) == true

    suspend fun readTodaySteps(): Long {
        val client = clientOrNull() ?: return 0
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val end = Instant.now()
        return client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0
    }

    companion object { const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata" }
}
