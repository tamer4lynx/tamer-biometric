package com.nanofuxion.tamerbiometric

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lynx.jsbridge.LynxMethod
import com.lynx.jsbridge.LynxModule
import com.lynx.react.bridge.Callback
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class BiometricModule(context: Context) : LynxModule(context) {

    companion object {
        private const val FINGERPRINT = 1
        private const val FACIAL_RECOGNITION = 2
        private const val IRIS = 3

        @Volatile
        internal var hostView: View? = null

        fun attachHostView(view: View?) {
            hostView = view
        }
    }

    private val biometricManager: BiometricManager? by lazy {
        try {
            mContext?.let { BiometricManager.from(it) }
        } catch (_: Exception) {
            null
        }
    }

    @LynxMethod
    fun hasHardwareAsync(callback: Callback) {
        val bm = biometricManager
        val available = if (bm == null) false else when (bm.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> true
            else -> false
        }
        callback.invoke(JSONObject().put("value", available).toString())
    }

    @LynxMethod
    fun isEnrolledAsync(callback: Callback) {
        val bm = biometricManager
        val enrolled = bm != null && bm.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        callback.invoke(JSONObject().put("value", enrolled).toString())
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @LynxMethod
    fun authenticateAsync(optionsJson: String, callback: Callback) {
        mainHandler.post {
            try {
                val activity = hostView?.context as? FragmentActivity
                if (activity == null) {
                    callback.invoke(JSONObject().apply {
                        put("success", false)
                        put("error", "not_available")
                    }.toString())
                    return@post
                }
                val options = try {
                    JSONObject(optionsJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                val promptMessage = options.optString("promptMessage", "Authenticate")
                val cancelLabel = options.optString("cancelLabel", "Cancel")
                val disableDeviceFallback = options.optBoolean("disableDeviceFallback", false)

                val useDeviceFallback = !disableDeviceFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                val authenticators = if (useDeviceFallback) {
                    BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                } else {
                    BIOMETRIC_STRONG
                }

                val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptMessage)
                    .setAllowedAuthenticators(authenticators)
                if (!useDeviceFallback) {
                    promptInfoBuilder.setNegativeButtonText(cancelLabel)
                }
                val promptInfo = promptInfoBuilder.build()

                val executor = ContextCompat.getMainExecutor(activity)
                val invoked = AtomicBoolean(false)

                fun invokeResult(success: Boolean, error: String? = null) {
                    if (invoked.compareAndSet(false, true)) {
                        val json = JSONObject().put("success", success)
                        if (error != null) json.put("error", error)
                        callback.invoke(json.toString())
                    }
                }

                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            val error = when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                BiometricPrompt.ERROR_CANCELED -> "user_cancel"
                                BiometricPrompt.ERROR_LOCKOUT,
                                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "lockout"
                                BiometricPrompt.ERROR_NO_BIOMETRICS -> "not_enrolled"
                                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                BiometricPrompt.ERROR_HW_NOT_PRESENT -> "not_available"
                                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> "security_update_required"
                                BiometricPrompt.ERROR_TIMEOUT -> "timeout"
                                else -> "unknown"
                            }
                            invokeResult(false, error)
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            invokeResult(true)
                        }
                    }
                )

                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                callback.invoke(JSONObject().apply {
                    put("success", false)
                    put("error", "not_available")
                }.toString())
            }
        }
    }

    @LynxMethod
    fun supportedAuthenticationTypesAsync(callback: Callback) {
        val types = JSONArray()
        val ctx = mContext ?: return callback.invoke(JSONObject().put("types", types).toString())
        val pm = ctx.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                types.put(FINGERPRINT)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_FACE)) {
                types.put(FACIAL_RECOGNITION)
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_IRIS)) {
                types.put(IRIS)
            }
        }
        callback.invoke(JSONObject().put("types", types).toString())
    }
}
