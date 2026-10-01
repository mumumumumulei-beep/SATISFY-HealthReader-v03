package com.satisfy.healthreader

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {
    private lateinit var store: HealthDataStore
    private lateinit var status: TextView
    private lateinit var energy: TextView
    private lateinit var sleep: TextView

    private val permissions by lazy {
        setOf(
            Permission.of(DataTypes.ENERGY_SCORE, AccessType.READ),
            Permission.of(DataTypes.SLEEP, AccessType.READ)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = HealthDataService.getStore(this)
        setContentView(buildUi())
        checkPermissionsAndRead(false)
    }

    private fun buildUi(): LinearLayout {
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text = text
            textSize = size
            setPadding(0, 18, 0, 8)
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 80, 48, 48)

            addView(label("SATISFY HEALTH READER", 24f))
            status = label("Samsung Health: checking…", 16f); addView(status)
            addView(label("ENERGY SCORE", 16f))
            energy = label("--", 44f); addView(energy)
            addView(label("SLEEP SCORE", 16f))
            sleep = label("--", 44f); addView(sleep)

            addView(Button(this@MainActivity).apply {
                text = "授权 Samsung Health"
                setOnClickListener { checkPermissionsAndRead(true) }
            })
            addView(Button(this@MainActivity).apply {
                text = "刷新数据"
                setOnClickListener { checkPermissionsAndRead(false) }
            })
        }
    }

    private fun checkPermissionsAndRead(requestIfMissing: Boolean) {
        lifecycleScope.launch {
            try {
                status.text = "Samsung Health: checking permissions…"
                var granted = store.getGrantedPermissions(permissions)
                if (!granted.containsAll(permissions) && requestIfMissing) {
                    granted = store.requestPermissions(permissions, this@MainActivity)
                }
                if (!granted.containsAll(permissions)) {
                    status.text = "Samsung Health: permission required"
                    energy.text = "--"; sleep.text = "--"
                    return@launch
                }
                status.text = "Samsung Health: connected"
                readScores()
            } catch (e: Exception) {
                status.text = "ERROR: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
            }
        }
    }

    private suspend fun readScores() {
        energy.text = "…"; sleep.text = "…"

        val today = LocalDate.now()
        val energyRequest = DataTypes.ENERGY_SCORE.readDataRequestBuilder
            .setLocalDateFilter(LocalDateFilter.of(today.minusDays(1), today))
            .setOrdering(Ordering.DESC)
            .setLimit(2)
            .build()
        val energyResult = store.readData(energyRequest).dataList
        val e = energyResult.firstOrNull()?.getValue(DataType.EnergyScoreType.ENERGY_SCORE)
        energy.text = e?.let { Math.round(it).toString() } ?: "--"

        val now = LocalDateTime.now()
        val sleepRequest = DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(now.minusHours(48), now))
            .setOrdering(Ordering.DESC)
            .setLimit(10)
            .build()
        val sleepResult = store.readData(sleepRequest).dataList
        val s = sleepResult.firstNotNullOfOrNull {
            it.getValue(DataType.SleepType.SLEEP_SCORE)
        }
        sleep.text = s?.toString() ?: "--"
        status.text = "Samsung Health: connected • refreshed"
    }
}
