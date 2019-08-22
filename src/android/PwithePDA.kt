package qingda.cordova

import android.graphics.BitmapFactory
import android.util.Base64
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import com.rsk.api.RskApi
import org.json.JSONObject
import java.lang.Exception

const val PRINT_LAYOUT_CENTER: Byte = 0x49;
const val PRINT_LAYOUT_CANCEL: Byte = 0x4b;
const val PRINT_FONT_24_24: Byte = 0x1;
const val PRINT_FONT_16_16: Byte = 0x2;
const val PRINT_FONT_16_32: Byte = 0x3;
const val PRINT_FONT_20_40: Byte = 0x4;

class PwithePDAPlugin : CordovaPlugin() {
    private var printerInited = false;

    override fun execute(action: String?, args: JSONArray?, callbackContext: CallbackContext?): Boolean {
        if (action == "hello") {
            hello(callbackContext!!)
            return true
        } else if (action == "printPage") {
            printPage(args!!, callbackContext!!)
            return true
        } else return false
    }

    private fun _initZGAndPrinter() {
        if (printerInited) {
            return;
        }

        var r = RskApi.ZGOpenPower()
        if (r != 0) {
            throw Exception("不支持的设备型号, 此模块仅能在警翼设备中使用");
        }

        // note(杨逸):
        // 按照文档ZG模块上电后需要等待2秒才能继续.
        Thread.sleep(2000)

        r = RskApi.PrintOpen()
        if (r != 0) {
            throw Exception("打印机上电失败, code=$r");
        }
        // note(杨逸):
        // 虽然文档中没有说明, 但这里我们也稍微暂停一下等待打印机上电完成.
        Thread.sleep(500)

        // completed
        printerInited = true;
    }

    /**
     * 打印:
     */
    private fun printPage(args: JSONArray, callbackContext: CallbackContext) {
        cordova.threadPool.run {
            try {
                // 初始化
                _initZGAndPrinter()

                // 预备
                RskApi.PrintSetGray(200.toByte())
                RskApi.PrintSetColor(0)
                RskApi.PrintSetSpeed(40)

                val data = args.getJSONObject(0)
                val elements = data.getJSONArray("elements")

                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)

                    when (element.getString("tagName")) {
                        "text" -> printTextElement(element)
                        "image" -> printImageElement(element)
                    }
                }

                RskApi.PrintDotLines(20);
                callbackContext.success("ok")
            } catch (error: Exception) {
                callbackContext.error(error.message)
            }
        }
    }

    /**
     * 重置文本打印设置.
     * - 例如: 字体大小和行高等
     * - 每次打印文本前都调用此方法
     */
    private fun resetPrintTextSettings() {
        RskApi.PrintFontSet(PRINT_FONT_24_24)
        RskApi.PrintFontMagnify(1)
        RskApi.PrintSetDotLine(12)
        RskApi.PrintSetLayout(PRINT_LAYOUT_CANCEL)
    }

    /**
     * 打印图片
     */
    private fun printImageElement(element: JSONObject) {
        val base64 = element.getString("base64")
        // val width = element.getString("width")
        // val height = element.getInt("height")

        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        Thread.sleep(1000)
        RskApi.PrintSetLayout(PRINT_LAYOUT_CENTER)
        RskApi.PrintBitmap(bitmap)
        RskApi.PrintSetLayout(PRINT_LAYOUT_CANCEL)
    }

    /**
     * 打印文本
     */
    private fun printTextElement(element: JSONObject) {
        val text = element.getString("text")
        val fontSize = element.getString("fontSize")
        val indent = element.getInt("indent")
        val align = element.getString("align");

        resetPrintTextSettings()
        RskApi.PrintSetLayout(if (align == "center") PRINT_LAYOUT_CENTER else PRINT_LAYOUT_CANCEL)
        if (fontSize == "large") {
            RskApi.PrintFontSet(PRINT_FONT_16_16)
            RskApi.PrintFontMagnify(2)
        }
        RskApi.PrintChars(text)
        RskApi.PrintLine()
    }

    private fun hello(callbackContext: CallbackContext) {
        try {
            var version = RskApi.GetVersion();
            callbackContext.success("hello, RskApi version is $version");
        } catch (error: Exception) {
            callbackContext.error(error.message);
        }
    }
}