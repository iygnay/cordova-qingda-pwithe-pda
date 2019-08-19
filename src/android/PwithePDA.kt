package qingda.cordova

import android.Manifest
import android.os.Build
import android.support.v4.app.ActivityCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode
import com.amap.api.location.AMapLocationListener
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray

class PwithePDAPlugin : CordovaPlugin(), AMapLocationListener {
    private var mLocationClient: AMapLocationClient? = null
    private val asyncSubject: PublishSubject<AMapLocation> = PublishSubject.create()
    private val PERMISSON_REQUESTCODE: Int = 0;
    private val checkPermission: Array<String> = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    )

    override fun pluginInitialize() {
        super.pluginInitialize()
        if (cordova.getActivity().getApplicationInfo().targetSdkVersion >= 23 &&
                Build.VERSION.SDK_INT >= 23) checkPermissions()
        initAMapLocation()
    }

    private fun checkPermissions() {
        ActivityCompat.requestPermissions(
                cordova.activity,
                checkPermission,
                PERMISSON_REQUESTCODE
        )
    }

    private fun initAMapLocation() {
        val mLocationClientOption = AMapLocationClientOption()
        mLocationClientOption.setOnceLocation(true)
        mLocationClientOption.setLocationMode(AMapLocationMode.Hight_Accuracy)

        mLocationClient = AMapLocationClient(cordova.activity.application.applicationContext)
        mLocationClient!!.setLocationOption(mLocationClientOption);
        mLocationClient!!.setLocationListener(this);
    }

    override fun execute(action: String?, args: JSONArray?, callbackContext: CallbackContext?): Boolean {
        if (action == "getCurrentPosition") {
            getCurrentPosition(callbackContext!!)
            return true
        } else return false
    }

    private fun getCurrentPosition(callbackContext: CallbackContext) {
        mLocationClient!!.startLocation()
        val json = JSONArray()
        lateinit var sub: Disposable;
        sub = asyncSubject.subscribe({
            mLocationClient!!.stopLocation()
            json.put(it.getLatitude())
            json.put(it.getLongitude())
            callbackContext.success(json)
            sub.dispose()
        }, {
            mLocationClient!!.stopLocation()
            callbackContext.error(it.message)
            sub.dispose()
        })
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location?.errorCode == 0) {
            asyncSubject.onNext(location)
            // asyncSubject.onComplete()
        } else {
            asyncSubject.onError(Exception(location?.errorCode.toString()))
            // asyncSubject.onComplete()
        }
    }
}