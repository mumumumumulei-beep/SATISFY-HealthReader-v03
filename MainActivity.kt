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
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var log: TextView

    private var healthStore: HealthDataStore? = null

    private val requiredPermissions by lazy {
        setOf(
            Permission.of(
                DataTypes.ENERGY_SCORE,
                AccessType.READ
            ),
            Permission.of(
                DataTypes.SLEEP,
                AccessType.READ
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动时仍然不主动访问 Samsung Health
        buildUi()
    }

    private fun buildUi() {

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(30))
        }

        val title = TextView(this).apply {
            text = "SATISFY HEALTH READER"
            textSize = 24f
        }

        val version = TextView(this).apply {
            text = "v0.3.3 PERMISSION DIAGNOSTIC"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(24))
        }

        status = TextView(this).apply {
            text = """
APP START              OK
SDK AAR                NOT TESTED
SDK CLASS              NOT TESTED
HEALTH DATA STORE      NOT TESTED

PERMISSION             NOT TESTED
ENERGY SCORE           --
SLEEP SCORE            --
            """.trimIndent()

            textSize = 17f
            setPadding(0, 0, 0, dp(20))
        }

        val button1 = Button(this).apply {
            text = "1  测试 APP"

            setOnClickListener {

                safeRun("APP TEST") {

                    appendLog("APP TEST OK")

                    status.text = """
APP START              OK
APP TEST               OK

SDK                     NOT TESTED
HEALTH DATA STORE       NOT TESTED

PERMISSION              NOT TESTED
ENERGY SCORE            --
SLEEP SCORE             --
                    """.trimIndent()
                }
            }
        }

        val button2 = Button(this).apply {
            text = "2  测试 SAMSUNG HEALTH SDK"

            setOnClickListener {

                safeRun("SDK CLASS TEST") {

                    val clazz = Class.forName(
                        "com.samsung.android.sdk.health.data.HealthDataService"
                    )

                    appendLog(
                        """
SDK CLASS FOUND
${clazz.name}
                        """.trimIndent()
                    )

                    status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK

HEALTH DATA STORE      NOT TESTED
PERMISSION             NOT TESTED

ENERGY SCORE           --
SLEEP SCORE            --
                    """.trimIndent()
                }
            }
        }

        val button3 = Button(this).apply {
            text = "3  GET HEALTH DATA STORE"

            setOnClickListener {

                safeRun("GET HEALTH DATA STORE") {

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

                    status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      OK

PERMISSION             NOT TESTED

ENERGY SCORE           --
SLEEP SCORE            --
                    """.trimIndent()
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

                        val store = getOrCreateStore()

                        val granted =
                            store.getGrantedPermissions(
                                requiredPermissions
                            )

                        val energyPermission =
                            Permission.of(
                                DataTypes.ENERGY_SCORE,
                                AccessType.READ
                            )

                        val sleepPermission =
                            Permission.of(
                                DataTypes.SLEEP,
                                AccessType.READ
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

                        appendLog(
                            """
CHECK PERMISSIONS OK

ENERGY_SCORE READ:
$energyGranted

SLEEP READ:
$sleepGranted

ALL REQUIRED:
$allGranted

Granted count:
${granted.size}
                            """.trimIndent()
                        )

                        status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      OK

PERMISSION             ${if (allGranted) "OK" else "MISSING"}

ENERGY PERMISSION      ${if (energyGranted) "GRANTED" else "MISSING"}
SLEEP PERMISSION       ${if (sleepGranted) "GRANTED" else "MISSING"}

ENERGY SCORE           --
SLEEP SCORE            --
                        """.trimIndent()

                    } catch (t: Throwable) {

                        showAsyncError(
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

                        appendLog(
                            """
REQUEST PERMISSIONS START

Requesting:
ENERGY_SCORE READ
SLEEP READ
                            """.trimIndent()
                        )

                        val store = getOrCreateStore()

                        val before =
                            store.getGrantedPermissions(
                                requiredPermissions
                            )

                        if (
                            before.containsAll(
                                requiredPermissions
                            )
                        ) {

                            appendLog(
                                """
PERMISSIONS ALREADY GRANTED

No permission popup required.
                                """.trimIndent()
                            )

                            status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      OK

PERMISSION             OK

ENERGY PERMISSION      GRANTED
SLEEP PERMISSION       GRANTED

ENERGY SCORE           --
SLEEP SCORE            --
                            """.trimIndent()

                            return@launch
                        }

                        val missing =
                            requiredPermissions
                                .toMutableSet()
                                .apply {
                                    removeAll(before)
                                }

                        appendLog(
                            """
Missing permission count:
${missing.size}

Opening Samsung Health permission UI...
                            """.trimIndent()
                        )

                        val result =
                            store.requestPermissions(
                                missing,
                                this@MainActivity
                            )

                        val after =
                            store.getGrantedPermissions(
                                requiredPermissions
                            )

                        val energyPermission =
                            Permission.of(
                                DataTypes.ENERGY_SCORE,
                                AccessType.READ
                            )

                        val sleepPermission =
                            Permission.of(
                                DataTypes.SLEEP,
                                AccessType.READ
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

                        appendLog(
                            """
REQUEST PERMISSIONS RETURNED

Returned count:
${result.size}

ENERGY_SCORE READ:
$energyGranted

SLEEP READ:
$sleepGranted

ALL REQUIRED:
$allGranted
                            """.trimIndent()
                        )

                        status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK
HEALTH DATA STORE      OK

PERMISSION             ${if (allGranted) "OK" else "MISSING"}

ENERGY PERMISSION      ${if (energyGranted) "GRANTED" else "MISSING"}
SLEEP PERMISSION       ${if (sleepGranted) "GRANTED" else "MISSING"}

ENERGY SCORE           --
SLEEP SCORE            --
                        """.trimIndent()

                    } catch (t: Throwable) {

                        showAsyncError(
                            "REQUEST PERMISSIONS",
                            t
                        )
                    }
                }
            }
        }

        val button6 = Button(this).apply {
            text = "6  READ ENERGY（下一步）"

            setOnClickListener {

                appendLog(
                    """
ENERGY READ BLOCKED

先完成权限测试。
                    """.trimIndent()
                )
            }
        }

        val button7 = Button(this).apply {
            text = "7  READ SLEEP（下一步）"

            setOnClickListener {

                appendLog(
                    """
SLEEP READ BLOCKED

先完成权限测试。
                    """.trimIndent()
                )
            }
        }

        val clear = Button(this).apply {
            text = "清空诊断日志"

            setOnClickListener {
                log.text = ""
            }
        }

        val logTitle = TextView(this).apply {
            text = "\nDIAGNOSTIC LOG"
            textSize = 18f
        }

        log = TextView(this).apply {

            text = """
v0.3.3 启动成功。

本轮目标：

1  测试 APP
2  测试 SAMSUNG HEALTH SDK
3  GET HEALTH DATA STORE
4  CHECK PERMISSIONS
5  REQUEST PERMISSIONS

只测试：

ENERGY_SCORE READ
SLEEP READ

步骤 6 / 7 仍然锁定。
            """.trimIndent()

            textSize = 14f
            setPadding(0, dp(10), 0, dp(50))

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

        val scroll = ScrollView(this)
        scroll.addView(root)

        setContentView(scroll)
    }

    private fun getOrCreateStore(): HealthDataStore {

        val existing = healthStore

        if (existing != null) {
            return existing
        }

        appendLog(
            "HealthDataStore was null. Creating store..."
        )

        val store =
            HealthDataService.getStore(
                applicationContext
            )

        healthStore = store

        appendLog(
            "HealthDataStore created successfully."
        )

        return store
    }

    private fun safeRun(
        name: String,
        action: () -> Unit
    ) {

        try {

            action()

        } catch (t: Throwable) {

            showAsyncError(
                name,
                t
            )
        }
    }

    private fun showAsyncError(
        name: String,
        throwable: Throwable
    ) {

        val root = rootCause(
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

        status.text = """
APP START              OK

$name
ERROR

${root.javaClass.simpleName}

PERMISSION             ERROR
ENERGY SCORE           --
SLEEP SCORE            --
        """.trimIndent()
    }

    private fun rootCause(
        throwable: Throwable
    ): Throwable {

        var current = throwable

        while (
            current.cause != null &&
            current.cause !== current
        ) {

            current = current.cause!!
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
