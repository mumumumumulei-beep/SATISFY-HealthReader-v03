package com.satisfy.healthreader

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

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
import kotlinx.coroutines.tasks.await

import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DATA_PATH =
            "/satisfy/health_scores"
    }

    private lateinit var status: TextView
    private lateinit var log: TextView

    private var healthStore: HealthDataStore? = null

    /*
     * REAL Samsung Health values.
     *
     * Do not hard-code Energy / Sleep.
     */
    private var energyScore: Float? = null
    private var sleepScore: Int? = null

    private var energyDate: String? = null
    private var sleepDate: String? = null

    private var watchConnectionStatus =
        "NOT TESTED"

    private var watchSyncStatus =
        "NOT SENT"

    private var permissionStatus =
        "NOT TESTED"

    private var energyPermissionStatus =
        "NOT TESTED"

    private var sleepPermissionStatus =
        "NOT TESTED"


    private val energyPermission by lazy {

        Permission.of(
            DataTypes.ENERGY_SCORE,
            AccessType.READ
        )
    }


    private val sleepPermission by lazy {

        Permission.of(
            DataTypes.SLEEP,
            AccessType.READ
        )
    }


    private val requiredPermissions by lazy {

        setOf(
            energyPermission,
            sleepPermission
        )
    }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        buildUi()
    }


    private fun buildUi() {

        val density =
            resources.displayMetrics.density

        fun dp(value: Int): Int {

            return (
                value * density
            ).toInt()
        }


        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(24),
                    dp(30),
                    dp(24),
                    dp(30)
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "SATISFY HEALTH BRIDGE"

                textSize =
                    24f
            }


        val version =
            TextView(this).apply {

                text =
                    "v0.4.0 DATA LAYER DIAGNOSTIC"

                textSize =
                    14f

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(24)
                )
            }


        status =
            TextView(this).apply {

                textSize =
                    17f

                setPadding(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }


        refreshStatus()


        /*
         * BUTTON 1
         */

        val button1 =
            Button(this).apply {

                text =
                    "1  测试 APP"

                setOnClickListener {

                    safeRun(
                        "APP TEST"
                    ) {

                        appendLog(
                            "APP TEST OK"
                        )

                        refreshStatus()
                    }
                }
            }


        /*
         * BUTTON 2
         */

        val button2 =
            Button(this).apply {

                text =
                    "2  测试 SAMSUNG HEALTH SDK"

                setOnClickListener {

                    safeRun(
                        "SDK CLASS TEST"
                    ) {

                        val clazz =
                            Class.forName(
                                "com.samsung.android.sdk.health.data.HealthDataService"
                            )

                        appendLog(
                            """
SDK CLASS FOUND

${clazz.name}
                            """.trimIndent()
                        )

                        refreshStatus()
                    }
                }
            }


        /*
         * BUTTON 3
         */

        val button3 =
            Button(this).apply {

                text =
                    "3  GET HEALTH DATA STORE"

                setOnClickListener {

                    safeRun(
                        "GET HEALTH DATA STORE"
                    ) {

                        appendLog(
                            "Calling HealthDataService.getStore()..."
                        )

                        val store =
                            HealthDataService.getStore(
                                applicationContext
                            )

                        healthStore =
                            store

                        appendLog(
                            """
GET STORE OK

Store class:
${store.javaClass.name}
                            """.trimIndent()
                        )

                        refreshStatus()
                    }
                }
            }


        /*
         * BUTTON 4
         */

        val button4 =
            Button(this).apply {

                text =
                    "4  CHECK PERMISSIONS"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            appendLog(
                                """
CHECK PERMISSIONS START

Checking:
ENERGY_SCORE READ
SLEEP READ
                                """.trimIndent()
                            )

                            val store =
                                getOrCreateStore()

                            val granted =
                                store.getGrantedPermissions(
                                    requiredPermissions
                                )

                            val energyGranted =
                                granted.contains(
                                    energyPermission
                                )

                            val sleepGranted =
                                granted.contains(
                                    sleepPermission
                                )

                            val allGranted =
                                granted.containsAll(
                                    requiredPermissions
                                )

                            permissionStatus =
                                if (allGranted) {
                                    "OK"
                                } else {
                                    "MISSING"
                                }

                            energyPermissionStatus =
                                if (energyGranted) {
                                    "GRANTED"
                                } else {
                                    "MISSING"
                                }

                            sleepPermissionStatus =
                                if (sleepGranted) {
                                    "GRANTED"
                                } else {
                                    "MISSING"
                                }

                            appendLog(
                                """
CHECK PERMISSIONS OK

ENERGY_SCORE READ:
$energyGranted

SLEEP READ:
$sleepGranted

ALL REQUIRED:
$allGranted
                                """.trimIndent()
                            )

                            refreshStatus()

                        } catch (
                            t: Throwable
                        ) {

                            showError(
                                "CHECK PERMISSIONS",
                                t
                            )
                        }
                    }
                }
            }


        /*
         * BUTTON 5
         */

        val button5 =
            Button(this).apply {

                text =
                    "5  REQUEST PERMISSIONS"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            val store =
                                getOrCreateStore()

                            appendLog(
                                """
REQUEST PERMISSIONS START

Requesting:
ENERGY_SCORE READ
SLEEP READ
                                """.trimIndent()
                            )

                            val before =
                                store.getGrantedPermissions(
                                    requiredPermissions
                                )

                            val missing =
                                requiredPermissions
                                    .filterNot {
                                        before.contains(it)
                                    }
                                    .toSet()

                            if (
                                missing.isNotEmpty()
                            ) {

                                appendLog(
                                    """
Opening Samsung Health permission UI...

Missing count:
${missing.size}
                                    """.trimIndent()
                                )

                                store.requestPermissions(
                                    missing,
                                    this@MainActivity
                                )
                            }


                            val after =
                                store.getGrantedPermissions(
                                    requiredPermissions
                                )

                            val energyGranted =
                                after.contains(
                                    energyPermission
                                )

                            val sleepGranted =
                                after.contains(
                                    sleepPermission
                                )

                            val allGranted =
                                after.containsAll(
                                    requiredPermissions
                                )

                            permissionStatus =
                                if (allGranted) {
                                    "OK"
                                } else {
                                    "MISSING"
                                }

                            energyPermissionStatus =
                                if (energyGranted) {
                                    "GRANTED"
                                } else {
                                    "MISSING"
                                }

                            sleepPermissionStatus =
                                if (sleepGranted) {
                                    "GRANTED"
                                } else {
                                    "MISSING"
                                }

                            appendLog(
                                """
PERMISSION RESULT

ENERGY_SCORE:
$energyGranted

SLEEP:
$sleepGranted

ALL REQUIRED:
$allGranted
                                """.trimIndent()
                            )

                            refreshStatus()

                        } catch (
                            t: Throwable
                        ) {

                            showError(
                                "REQUEST PERMISSIONS",
                                t
                            )
                        }
                    }
                }
            }


        /*
         * BUTTON 6
         *
         * REAL ENERGY SCORE
         */

        val button6 =
            Button(this).apply {

                text =
                    "6  READ ENERGY"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            val store =
                                getOrCreateStore()

                            ensurePermission(
                                store,
                                energyPermission,
                                "ENERGY_SCORE"
                            )


                            val today =
                                LocalDate.now()

                            val yesterday =
                                today.minusDays(1)


                            appendLog(
                                """
ENERGY READ START

Date range:
$yesterday
to
$today
                                """.trimIndent()
                            )


                            val request =
                                DataTypes
                                    .ENERGY_SCORE
                                    .readDataRequestBuilder
                                    .setLocalDateFilter(
                                        LocalDateFilter.of(
                                            yesterday,
                                            today
                                        )
                                    )
                                    .setOrdering(
                                        Ordering.DESC
                                    )
                                    .setLimit(
                                        10
                                    )
                                    .build()


                            val response =
                                store.readData(
                                    request
                                )


                            val data =
                                response.dataList


                            appendLog(
                                """
ENERGY RESPONSE OK

Record count:
${data.size}
                                """.trimIndent()
                            )


                            var selectedScore:
                                Float? = null

                            var selectedDate:
                                String? = null


                            for (
                                point in data
                            ) {

                                val score =
                                    point.getValue(
                                        DataType
                                            .EnergyScoreType
                                            .ENERGY_SCORE
                                    )


                                val localDate =
                                    point
                                        .getStartLocalDateTime()
                                        .toLocalDate()


                                appendLog(
                                    """
ENERGY RECORD

Date:
$localDate

Score:
${score ?: "null"}

Update:
${point.updateTime}
                                    """.trimIndent()
                                )


                                if (
                                    score != null &&
                                    selectedScore == null
                                ) {

                                    selectedScore =
                                        score

                                    selectedDate =
                                        localDate
                                            .toString()
                                }
                            }


                            energyScore =
                                selectedScore

                            energyDate =
                                selectedDate


                            if (
                                selectedScore != null
                            ) {

                                appendLog(
                                    """
ENERGY SELECTED

Score:
$selectedScore

Date:
$selectedDate
                                    """.trimIndent()
                                )

                            } else {

                                appendLog(
                                    """
ENERGY NO DATA

No non-null Energy Score
was found for today/yesterday.
                                    """.trimIndent()
                                )
                            }


                            permissionStatus =
                                "OK"

                            energyPermissionStatus =
                                "GRANTED"

                            refreshStatus()

                        } catch (
                            t: Throwable
                        ) {

                            showError(
                                "READ ENERGY",
                                t
                            )
                        }
                    }
                }
            }


        /*
         * BUTTON 7
         *
         * REAL SLEEP SCORE
         */

        val button7 =
            Button(this).apply {

                text =
                    "7  READ SLEEP"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            val store =
                                getOrCreateStore()

                            ensurePermission(
                                store,
                                sleepPermission,
                                "SLEEP"
                            )


                            val end =
                                LocalDateTime.now()

                            val start =
                                end.minusHours(
                                    48
                                )


                            appendLog(
                                """
SLEEP READ START

Time range:
$start
to
$end
                                """.trimIndent()
                            )


                            val request =
                                DataTypes
                                    .SLEEP
                                    .readDataRequestBuilder
                                    .setLocalTimeFilter(
                                        LocalTimeFilter.of(
                                            start,
                                            end
                                        )
                                    )
                                    .setOrdering(
                                        Ordering.DESC
                                    )
                                    .setLimit(
                                        20
                                    )
                                    .build()


                            val response =
                                store.readData(
                                    request
                                )


                            val data =
                                response.dataList


                            appendLog(
                                """
SLEEP RESPONSE OK

Record count:
${data.size}
                                """.trimIndent()
                            )


                            var selectedScore:
                                Int? = null

                            var selectedTime:
                                String? = null


                            for (
                                point in data
                            ) {

                                val score =
                                    point.getValue(
                                        DataType
                                            .SleepType
                                            .SLEEP_SCORE
                                    )


                                val startTime =
                                    point
                                        .getStartLocalDateTime()


                                val endTime =
                                    point
                                        .getEndLocalDateTime()


                                appendLog(
                                    """
SLEEP RECORD

Start:
$startTime

End:
${endTime ?: "null"}

Sleep Score:
${score ?: "null"}

Update:
${point.updateTime}
                                    """.trimIndent()
                                )


                                if (
                                    score != null &&
                                    selectedScore == null
                                ) {

                                    selectedScore =
                                        score

                                    selectedTime =
                                        startTime
                                            .toString()
                                }
                            }


                            sleepScore =
                                selectedScore

                            sleepDate =
                                selectedTime


                            if (
                                selectedScore != null
                            ) {

                                appendLog(
                                    """
SLEEP SELECTED

Score:
$selectedScore

Start:
$selectedTime
                                    """.trimIndent()
                                )

                            } else {

                                appendLog(
                                    """
SLEEP NO DATA

No non-null Sleep Score
was found in the last 48 hours.
                                    """.trimIndent()
                                )
                            }


                            permissionStatus =
                                "OK"

                            sleepPermissionStatus =
                                "GRANTED"

                            refreshStatus()

                        } catch (
                            t: Throwable
                        ) {

                            showError(
                                "READ SLEEP",
                                t
                            )
                        }
                    }
                }
            }


        /*
         * BUTTON 8
         *
         * CHECK CONNECTED WEAR NODES
         */

        val button8 =
            Button(this).apply {

                text =
                    "8  CHECK WATCH CONNECTION"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            appendLog(
                                """
WATCH NODE CHECK START

Searching connected Wear OS nodes...
                                """.trimIndent()
                            )


                            val nodes =
                                Wearable
                                    .getNodeClient(
                                        this@MainActivity
                                    )
                                    .connectedNodes
                                    .await()


                            if (
                                nodes.isEmpty()
                            ) {

                                watchConnectionStatus =
                                    "NO NODE"

                                appendLog(
                                    """
WATCH NODE RESULT

NO CONNECTED WEAR NODE FOUND

Check:
- Bluetooth
- Galaxy Wearable connection
- Google Play Services
- Same paired watch
                                    """.trimIndent()
                                )

                            } else {

                                watchConnectionStatus =
                                    "CONNECTED"

                                appendLog(
                                    """
WATCH NODE RESULT

Connected node count:
${nodes.size}
                                    """.trimIndent()
                                )


                                nodes.forEach {

                                    appendLog(
                                        """
WATCH NODE

Name:
${it.displayName}

ID:
${it.id}

Nearby:
${it.isNearby}
                                        """.trimIndent()
                                    )
                                }
                            }


                            refreshStatus()

                        } catch (
                            t: Throwable
                        ) {

                            watchConnectionStatus =
                                "ERROR"

                            showError(
                                "CHECK WATCH CONNECTION",
                                t
                            )
                        }
                    }
                }
            }


        /*
         * BUTTON 9
         *
         * SEND REAL SCORES TO WATCH
         */

        val button9 =
            Button(this).apply {

                text =
                    "9  SEND SCORES TO WATCH"

                setOnClickListener {

                    lifecycleScope.launch {

                        try {

                            val nodes =
                                Wearable
                                    .getNodeClient(
                                        this@MainActivity
                                    )
                                    .connectedNodes
                                    .await()


                            if (
                                nodes.isEmpty()
                            ) {

                                watchConnectionStatus =
                                    "NO NODE"

                                watchSyncStatus =
                                    "NOT SENT"

                                refreshStatus()

                                appendLog(
                                    """
WATCH SYNC BLOCKED

No connected Wear OS node.

Run button 8 first.
                                    """.trimIndent()
                                )

                                return@launch
                            }


                            watchConnectionStatus =
                                "CONNECTED"


                            val energyToSend =
                                energyScore
                                    ?: -1f

                            val sleepToSend =
                                sleepScore
                                    ?: -1

                            val energyDateToSend =
                                energyDate
                                    ?: ""

                            val sleepDateToSend =
                                sleepDate
                                    ?: ""

                            val updatedAt =
                                System.currentTimeMillis()


                            appendLog(
                                """
WATCH SYNC START

Path:
$DATA_PATH

Energy:
$energyToSend

Energy date:
${energyDateToSend.ifEmpty { "--" }}

Sleep:
$sleepToSend

Sleep date:
${sleepDateToSend.ifEmpty { "--" }}

Updated at:
$updatedAt
                                """.trimIndent()
                            )


                            val putDataMapRequest =
                                PutDataMapRequest
                                    .create(
                                        DATA_PATH
                                    )


                            putDataMapRequest
                                .dataMap
                                .putFloat(
                                    "energy_score",
                                    energyToSend
                                )


                            putDataMapRequest
                                .dataMap
                                .putString(
                                   
