package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat

/**
 * Native Android Biometric Authentication Helper & JS Bridge (Zero External Dependencies).
 * Uses built-in android.hardware.biometrics.BiometricPrompt (Android P+) and KeyguardManager,
 * plus WebAuthn API fallback support for browser/WebView contexts.
 */
object BiometricAuthHelper {

    const val DEFAULT_PROMPT_TITLE = "تأكيد الهوية - منصة مدرستي"
    const val DEFAULT_PROMPT_SUBTITLE = "ضع بصمة إصبعك لتسجيل الدخول السريع"

    /**
     * Checks whether the device supports biometric fingerprint or secure device authentication.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java)
                if (bm != null) {
                    val result = bm.canAuthenticate()
                    if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                        return true
                    }
                }
            }
            @Suppress("DEPRECATION")
            val fm = context.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager
            @Suppress("DEPRECATION")
            if (fm != null && fm.isHardwareDetected && fm.hasEnrolledFingerprints()) {
                return true
            }
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && km.isDeviceSecure) {
                return true
            }
            // Check package manager fingerprint feature
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks specifically if hardware biometrics are actively enrolled in Android OS Settings.
     */
    fun hasEnrolledHardwareBiometrics(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java)
                if (bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                    return true
                }
            }
            @Suppress("DEPRECATION")
            val fm = context.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager
            @Suppress("DEPRECATION")
            fm != null && fm.isHardwareDetected && fm.hasEnrolledFingerprints()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches the native Android OS BiometricPrompt ("تأكيد الهوية - منصة مدرستي").
     * If hardware biometrics are not enrolled on the current test device/emulator, triggers
     * onFallbackRequired so the UI can present the interactive Biometric Sensor Verification dialog.
     */
    fun authenticateBiometric(
        context: Context,
        title: String = DEFAULT_PROMPT_TITLE,
        subtitle: String = DEFAULT_PROMPT_SUBTITLE,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackRequired: (() -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (!hasEnrolledHardwareBiometrics(context) && onFallbackRequired != null) {
                    onFallbackRequired()
                    return
                }

                val executor = ContextCompat.getMainExecutor(context)
                val cancellationSignal = CancellationSignal()

                val builder = BiometricPrompt.Builder(context)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(subtitle)
                    .setNegativeButton(
                        "إلغاء",
                        executor,
                        DialogInterface.OnClickListener { _, _ ->
                            onError("تم إلغاء التحقق من بصمة الإصبع")
                        }
                    )

                val prompt = builder.build()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            StandardQrCodeHelper.playScanBeepAndVibrate(context)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // Codes 1 (HW_UNAVAILABLE), 11 (NO_BIOMETRICS), 12 (HW_NOT_PRESENT), 14 (NO_DEVICE_CREDENTIAL)
                            if ((errorCode == 1 || errorCode == 11 || errorCode == 12 || errorCode == 14) &&
                                onFallbackRequired != null
                            ) {
                                onFallbackRequired()
                            } else {
                                val msg = errString?.toString()?.ifBlank { "تعذر التحقق من البصمة" }
                                    ?: "تعذر التحقق من البصمة"
                                onError(msg)
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // Keep prompt open so user can retry touching the fingerprint sensor
                        }
                    }
                )
            } catch (e: Exception) {
                if (onFallbackRequired != null) {
                    onFallbackRequired()
                } else {
                    onError(e.localizedMessage ?: "تعذر تشغيل مستشعر البصمة")
                }
            }
        } else {
            if (onFallbackRequired != null) {
                onFallbackRequired()
            } else {
                onError("هذا الإصدار من أندرويد لا يدعم نافذة البصمة المدمجة")
            }
        }
    }

    /**
     * WebAuthn API (navigator.credentials.create / get) script for browser fallback support.
     */
    const val WEBAUTHN_BROWSER_FALLBACK_JS = """
        window.triggerBrowserWebAuthn = async function() {
            try {
                if (!window.PublicKeyCredential || !navigator.credentials) {
                    if (window.onBiometricError) window.onBiometricError("المتصفح لا يدعم WebAuthn API");
                    return;
                }
                const challenge = new Uint8Array(32);
                window.crypto.getRandomValues(challenge);
                const publicKey = {
                    challenge: challenge,
                    timeout: 60000,
                    userVerification: "required"
                };
                await navigator.credentials.get({ publicKey: publicKey });
                if (window.onBiometricSuccess) window.onBiometricSuccess();
            } catch (err) {
                if (window.onBiometricError) window.onBiometricError(err.message || "فشل التحقق عبر WebAuthn");
            }
        };
    """
}

/**
 * Native AndroidBridge (@JavascriptInterface) exposing Biometric & App Icon methods
 * to WebView / JavaScript environments alongside native Jetpack Compose.
 */
class WebAppInterface(
    private val context: Context,
    private val webView: WebView? = null,
    private val onNativeBiometricSuccess: (() -> Unit)? = null,
    private val onNativeBiometricError: ((String) -> Unit)? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun isBiometricAvailable(): Boolean {
        return BiometricAuthHelper.isBiometricAvailable(context)
    }

    @JavascriptInterface
    fun authenticateBiometric() {
        mainHandler.post {
            BiometricAuthHelper.authenticateBiometric(
                context = context,
                title = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
                subtitle = BiometricAuthHelper.DEFAULT_PROMPT_SUBTITLE,
                onSuccess = {
                    onNativeBiometricSuccess?.invoke()
                    webView?.evaluateJavascript(
                        "if (typeof window.onBiometricSuccess === 'function') { window.onBiometricSuccess(); }",
                        null
                    )
                },
                onError = { errMsg ->
                    onNativeBiometricError?.invoke(errMsg)
                    val safeMsg = errMsg.replace("'", "\\'").replace("\n", " ")
                    webView?.evaluateJavascript(
                        "if (typeof window.onBiometricError === 'function') { window.onBiometricError('$safeMsg'); }",
                        null
                    )
                }
            )
        }
    }

    @JavascriptInterface
    fun setAppIcon(aliasName: String) {
        AppIconSwitcherHelper.setAppIcon(context, aliasName)
    }
}
