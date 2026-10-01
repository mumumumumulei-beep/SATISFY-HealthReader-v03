package com.satisfy.healthreader

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 诊断版启动阶段故意完全不调用 Samsung Health SDK。
        // 第一目标：确认 Activity 本身可以稳定运行。
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
            text = "v0.3.1 SAFE DIAGNOSTIC"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(24))
        }

        status = TextView(this).apply {
            text = """
APP START              OK

SAMSUNG HEALTH SDK     NOT STARTED

ENERGY SCORE           --
SLEEP SCORE            --
            """.trimIndent()

            textSize = 17f
            setPadding(0, 0, 0, dp(20))
        }

        val testAppButton = Button(this).apply {
            text = "1  测试 APP"
            setOnClickListener {
                safeRun("APP TEST") {
                    status.text = """
APP START              OK
APP TEST               OK

SAMSUNG HEALTH SDK     NOT STARTED

ENERGY SCORE           --
SLEEP SCORE            --
                    """.trimIndent()

                    appendLog("APP TEST OK")
                }
            }
        }

        val sdkButton = Button(this).apply {
            text = "2  测试 SAMSUNG HEALTH SDK"
            setOnClickListener {
                testSamsungSdkClass()
            }
        }

        val permissionButton = Button(this).apply {
            text = "3  权限测试（暂不执行）"
            setOnClickListener {
                appendLog(
                    "PERMISSION TEST BLOCKED\n" +
                    "v0.3.1 暂时不调用 requestPermissions()."
                )
            }
        }

        val energyButton = Button(this).apply {
            text = "4  ENERGY 测试（暂不执行）"
            setOnClickListener {
                appendLog(
                    "ENERGY READ BLOCKED\n" +
                    "等待 SDK 基础测试通过。"
                )
            }
        }

        val sleepButton = Button(this).apply {
            text = "5  SLEEP 测试（暂不执行）"
            setOnClickListener {
                appendLog(
                    "SLEEP READ BLOCKED\n" +
                    "等待 SDK 基础测试通过。"
                )
            }
        }

        val clearButton = Button(this).apply {
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
启动成功。
当前版本不会在启动时访问 Samsung Health。
请先点击：

1 测试 APP
2 测试 SAMSUNG HEALTH SDK
            """.trimIndent()

            textSize = 14f
            setPadding(0, dp(10), 0, dp(40))
            setTextIsSelectable(true)
        }

        root.addView(title)
        root.addView(version)
        root.addView(status)
        root.addView(testAppButton)
        root.addView(sdkButton)
        root.addView(permissionButton)
        root.addView(energyButton)
        root.addView(sleepButton)
        root.addView(clearButton)
        root.addView(logTitle)
        root.addView(log)

        val scroll = ScrollView(this)
        scroll.addView(root)

        setContentView(scroll)
    }

    private fun testSamsungSdkClass() {
        safeRun("SAMSUNG HEALTH SDK CLASS TEST") {

            val clazz = Class.forName(
                "com.samsung.android.sdk.health.data.HealthDataService"
            )

            appendLog(
                "SDK CLASS FOUND\n" +
                "Class = ${clazz.name}"
            )

            status.text = """
APP START              OK
SDK AAR                FOUND
SDK CLASS              OK

PERMISSION             NOT TESTED
ENERGY SCORE           --
SLEEP SCORE            --
            """.trimIndent()
        }
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
$name ERROR

Exception:
${t.javaClass.name}

Message:
${t.message ?: "(no message)"}

ROOT CAUSE:
${root.javaClass.name}

ROOT MESSAGE:
${root.message ?: "(no message)"}

------------------------------
                """.trimIndent()
            )

            status.text = """
APP START              OK
$name                  ERROR

${root.javaClass.simpleName}

ENERGY SCORE           --
SLEEP SCORE            --
            """.trimIndent()
        }
    }

    private fun rootCause(t: Throwable): Throwable {
        var current = t

        while (
            current.cause != null &&
            current.cause !== current
        ) {
            current = current.cause!!
        }

        return current
    }

    private fun appendLog(message: String) {
        log.append(
            "\n\n$message\n"
        )
    }
}
