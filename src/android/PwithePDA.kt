package qingda.cordova

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.pwithe.printapi.PrintApi
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import io.reactivex.subjects.AsyncSubject
import io.reactivex.subjects.PublishSubject

class PwithePDAPlugin : CordovaPlugin() {
    private var zgDeviceInited = false;
    private var isOldDev = false;
    private var initPrinterSubject = AsyncSubject.create<Int>()
    private var completePrintSubject = PublishSubject.create<Int>()

    override fun execute(action: String?, args: JSONArray?, callbackContext: CallbackContext?): Boolean {
        if (action == "hello") {
            hello(callbackContext!!)
            return true
        } else if (action == "printPage") {
            printPage(args!!, callbackContext!!)
            return true
        } else return false
    }

    private fun initZGAndPrinter() {
        if (!zgDeviceInited) {
            PrintApi.CheckDevcie {
                this.isOldDev = it
                Log.i("PrintApi","CheckDevcie isOldDev=" + this.isOldDev)

                PrintApi.InitPrint(cordova.activity) {
                    // 打印完成逻辑...
                    Log.i("PrintApi.InitPrint","print completed")
                    this.completePrintSubject.onNext(1)
                }

                Log.i("PrintApi","InitPrint")
                this.initPrinterSubject.onNext(1)
                this.initPrinterSubject.onComplete()
            }

            // completed
            zgDeviceInited = true;
        }
    }

    /**
     * 打印:
     */
    private fun printPage(args: JSONArray, callbackContext: CallbackContext) {
        try {
            // 初始化
            initZGAndPrinter()
            this.initPrinterSubject
                .subscribe ({
                    this.completePrintSubject.subscribe {
                        callbackContext.success("ok")
                    }

                    try {
                        val data = args.getJSONObject(0)
                        val elements = data.getJSONArray("elements")

                        for (i in 0 until elements.length()) {
                            val element = elements.getJSONObject(i)

                            when (element.getString("tagName")) {
                                "text" -> printTextElement(element)
                                "image" -> printImageElement(element)
                            }
                        }

                        // 结尾符号
                        PrintApi.DoPrintMsg("\n\n")
                        PrintApi.DoSetPrintLayout(1)
                        PrintApi.DoPrintMsg("_____________________________\n\n\n\n\n");
                        PrintApi.DoPrintOver()
                    } catch (error: Exception) {
                        callbackContext.error(error.message)
                    }

                }, {
                    callbackContext.error("打印机初始化失败")
                })
        } catch (error: Exception) {
            callbackContext.error(error.message)
        }
    }

    /**
     * 打印图片
     */
    private fun printImageElement(element: JSONObject) {
        val base64 = element.getString("base64")
        var qrcode = element.getString("qrcode")
        // val width = element.getString("width")
        // val height = element.getInt("height")
        PrintApi.DoSetPrintLayout(1)

        if (qrcode != null && qrcode != "") {
            // 二维码模式
            PrintApi.DoPrintSetQrWidth(260)
            PrintApi.DoPrintQrc(qrcode)
            PrintApi.DoPrintMsg("\n")
        } else {
            // 图片模式
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            PrintApi.DoPrintImg(bitmap)
            PrintApi.DoPrintMsg("\n")
        }
    }

    /**
     * 打印文本
     */
    private fun printTextElement(element: JSONObject) {
        Log.i("PrintApi", "printTextElement")
        val text = element.getString("text")
        val fontSize = element.getString("fontSize")
        val indent = element.getInt("indent")
        val align = element.getString("align");

        // 设置对齐
        // 0 左对齐, 1 居中, 2 右对齐
        PrintApi.DoSetPrintLayout(when (align) {
            "center" -> 1
            "right" -> 2
            else -> 0
        })

        if (text == "_"  || text == "") {
            PrintApi.DoPrintMsg("\n")
        } else {
            if (fontSize == "large") {
                // note(杨逸): 警翼新驱动不支持字体大小
            }

            val prefixChars = CharArray(indent)
            Arrays.fill(prefixChars, ' ')
            val prefix = String(prefixChars)
            PrintApi.DoPrintMsg(prefix + text + "\n\n")
        }
    }

    private fun hello(callbackContext: CallbackContext) {
        initZGAndPrinter()
        this.initPrinterSubject.subscribe {
            try {
                var version = PrintApi.DoGetPrintVersion()
                callbackContext.success("hello, DoGetPrintVersion version is $version");
            } catch (error: Exception) {
                callbackContext.error(error.message);
            }
        }
    }
}