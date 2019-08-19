package qingda.cordova

import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import com.rsk.api.RskApi
import java.lang.Exception

class PwithePDAPlugin : CordovaPlugin() {
    override fun execute(action: String?, args: JSONArray?, callbackContext: CallbackContext?): Boolean {
        if (action == "hello") {
            hello(callbackContext!!)
            return true
        } else return false
    }

    private fun hello(callbackContext: CallbackContext) {
        try {
            var version = RskApi.GetVersion();
            callbackContext.success("hello, RskApi version is " + version);
        } catch (error: Exception) {
            callbackContext.error(error.message);
        }
    }
}