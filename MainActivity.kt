package com.satisfy.healthreader

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var log: TextView

    private var healthStore: HealthDataStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动时仍然不碰 Samsung Health
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
            text = "v0.3.2 HEALTH STORE DIAGNOSTIC"
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
            text = "4  CHECK PERMISSIONS（下一步）"

            setOnClickListener {

                appendLog(
                    """
CHECK PERMISSIONS BLOCKED

先确认步骤 3：
HealthDataService.getStore()
是否成功。
                    """.trimIndent()
                )
            }
        }

        val button5 = Button(this).apply {
            text = "5  REQUEST PERMISSIONS（下一步）"

            setOnClickListener {

                appendLog(
                    """
REQUEST PERMISSIONS BLOCKED

暂时不会弹出 Samsung Health 授权页。
                    """.trimIndent()
                )
            }
        }

        val button6 = Button(this).apply {
            text = "6  READ ENERGY（下一步）"

            setOnClickListener {
                appendLog("ENERGY READ BLOCKED")
            }
        }

        val button7 = Button(this).apply {
            text = "7  READ SLEEP（下一步）"

            setOnClickListener {
                appendLog("SLEEP READ BLOCKED")
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
v0.3.2 启动成功。

启动阶段不会访问 Samsung Health。

请依次测试：

1  测试 APP
2  测试 SAMSUNG HEALTH SDK
3  GET HEALTH DATA STORE

步骤 3 是本轮关键测试。
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

    private fun safeRun(
        name: String,
        action: () -> Unit
    ) {

        try {

            action()

        } catch (t: Throwable) {

            val root = rootCause(t)

            appendLog(
                """
==============================

$name ERROR

Exception:
${t.javaClass.name}

Message:
${t.message ?: "(no message)"}

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

PERMISSION             NOT TESTED
ENERGY SCORE           --
SLEEP SCORE            --
            """.trimIndent()
        }
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
