package com.satisfy.healthreader

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
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

    private lateinit var status: TextView
    private lateinit var log: TextView

    private var healthStore: HealthDataStore? = null

    private var energyScore: Float? = null
    private var sleepScore: Int? = null

    private var permissionStatus = "NOT TESTED"
    private var energyPermissionStatus = "NOT TESTED"
    private var sleepPermissionStatus = "NOT TESTED"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {

        val density = resources.displayMetrics.density

        fun dp(value: Int): Int {
            return (value * density).toInt()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(24),
                dp(30),
                dp(24),
                dp(30)
            )
        }

        val title = TextView(this).apply {
            text = "SATISFY HEALTH READER"
            textSize = 24f
        }

        val version = TextView(this).apply {
            text = "v0.3.4 REAL DATA READ"
            textSize = 14f

            setPadding(
                0,
                dp(6),
                0,
                dp(24)
            )
        }

        status = TextView(this).apply {
            textSize = 17f

            setPadding(
                0,
                0,
                0,
                dp(20)
            )
        }

        refreshStatus()


        val button1 = Button(this).apply {

            text = "1  测试 APP"

            setOnClickListener {

                safeRun("APP TEST") {

                    appendLog(
                        "APP TEST OK"
                    )

                    refreshStatus()
                }
            }
        }


        val button2 = Button(this).apply {

            text = "2  测试 SAMSUNG HEALTH SDK"

            setOnClickListener {

                safeRun("SDK CLASS TEST") {

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


        val button3 = Button(this).apply {

            text = "3  GET HEALTH DATA STORE"

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

                    healthStore = store

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


        val button4 = Button(this).apply {

            text = "4  CHECK PERMISSIONS"

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

                    } catch (t: Throwable) {

                        showError(
                            "CHECK PERMISSIONS",
                            t
                        )
                    }
                }
            }
        }


        val button5 = Button(this).apply {

            text = "5  REQUEST PERMISSIONS"

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

                        if (missing.isNotEmpty()) {

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

                    } catch (t: Throwable) {

                        showError(
                            "REQUEST PERMISSIONS",
                            t
                        )
                    }
                }
            }
        }


        val button6 = Button(this).apply {

            text = "6  READ ENERGY"

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
                                .setLimit(10)
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

                        for (point in data) {

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
                                    localDate.toString()
                            }
                        }

                        energyScore =
                            selectedScore

                        if (selectedScore != null) {

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

                        permissionStatus = "OK"
                        energyPermissionStatus = "GRANTED"

                        refreshStatus()

                    } catch (t: Throwable) {

                        showError(
                            "READ ENERGY",
                            t
                        )
                    }
                }
            }
        }


        val button7 = Button(this).apply {

            text = "7  READ SLEEP"

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
                            end.minusHours(48)

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
                                .setLimit(20)
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

                        for (point in data) {

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
                                    startTime.toString()
                            }
                        }

                        sleepScore =
                            selectedScore

                        if (selectedScore != null) {

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

                        permissionStatus = "OK"
                        sleepPermissionStatus = "GRANTED"

                        refreshStatus()

                    } catch (t: Throwable) {

                        showError(
                            "READ SLEEP",
                            t
                        )
                    }
                }
            }
        }


        val clear =
            Button(this).apply {

                text = "清空诊断日志"

                setOnClickListener {

                    log.text = ""
                }
            }


        val logTitle =
            TextView(this).apply {

                text = "\nDIAGNOSTIC LOG"

                textSize = 18f
            }


        log =
            TextView(this).apply {

                text =
                    """
v0.3.4 REAL DATA READ

当前阶段：

APP                OK
SDK                OK
STORE              已验证
ENERGY PERMISSION  已验证
SLEEP PERMISSION   已验证

按钮 6：
读取真实 Samsung Health Energy Score

按钮 7：
读取真实 Samsung Health Sleep Score

Energy 查询：
今天 + 昨天

Sleep 查询：
最近 48 小时

无数据：
--

本版本修正：
HealthDataPoint
getStartLocalDateTime()
getEndLocalDateTime()
                    """.trimIndent()

                textSize = 14f

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(50)
                )

                setTextIsSelectable(true)
            }


        root.addView(title)
        root.addView(version)
        root.addView(status)

        root.addView(button1)
        root.addView(button2)
        root.addView(button3)
        root.addView(button4)
        root.addView(button5)
        root.addView(button6)
        root.addView(button7)

        root.addView(clear)

        root.addView(logTitle)
        root.addView(log)


        val scroll =
            ScrollView(this)

        scroll.addView(root)

        setContentView(scroll)
    }


    private suspend fun ensurePermission(
        store: HealthDataStore,
        permission: Permission,
        name: String
    ) {

        val granted =
            store.getGrantedPermissions(
                setOf(permission)
            )

        if (!granted.contains(permission)) {

            throw IllegalStateException(
                "$name READ permission is not granted"
            )
        }
    }


    private fun getOrCreateStore():
        HealthDataStore {

        val existing =
            healthStore

        if (existing != null) {
            return existing
        }

        appendLog(
            "Creating HealthDataStore..."
        )

        val store =
            HealthDataService.getStore(
                applicationContext
            )

        healthStore = store

        appendLog(
            "HealthDataStore created successfully."
        )

        refreshStatus()

        return store
    }


    private fun refreshStatus() {

        val energyText =
            energyScore
                ?.let {
                    String.format(
                        "%.0f",
                        it
                    )
                }
                ?: "--"

        val sleepText =
            sleepScore
                ?.toString()
                ?: "--"

        status.text =
            """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      ${if (healthStore != null) "OK" else "NOT TESTED"}

PERMISSION             $permissionStatus

ENERGY PERMISSION      $energyPermissionStatus
SLEEP PERMISSION       $sleepPermissionStatus

ENERGY SCORE           $energyText
SLEEP SCORE            $sleepText
            """.trimIndent()
    }


    private fun safeRun(
        name: String,
        action: () -> Unit
    ) {

        try {

            action()

        } catch (t: Throwable) {

            showError(
                name,
                t
            )
        }
    }


    private fun showError(
        name: String,
        throwable: Throwable
    ) {

        val root =
            rootCause(
                throwable
            )

        appendLog(
            """
==============================

$name ERROR

Exception:
${throwable.javaClass.name}

Message:
${throwable.message ?: "(no message)"}

ROOT CAUSE:
${root.javaClass.name}

ROOT MESSAGE:
${root.message ?: "(no message)"}

==============================
            """.trimIndent()
        )

        status.text =
            """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      ${if (healthStore != null) "OK" else "NOT TESTED"}

$name
ERROR

${root.javaClass.simpleName}

ENERGY SCORE           ${energyScore ?: "--"}
SLEEP SCORE            ${sleepScore ?: "--"}
            """.trimIndent()
    }


    private fun rootCause(
        throwable: Throwable
    ): Throwable {

        var current =
            throwable

        while (
            current.cause != null &&
            current.cause !== current
        ) {

            current =
                current.cause!!
        }

        return current
    }


    private fun appendLog(
        message: String
    ) {

        log.append(
            "\n\n$message\n"
        )
    }
}
