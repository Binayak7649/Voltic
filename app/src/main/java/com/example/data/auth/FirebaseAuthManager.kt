package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWith(Result.failure(exception))
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object SendingPhoneCode : AuthState()
    data class PhoneCodeSent(val verificationId: String) : AuthState()
    object VerifyingCode : AuthState()
    object GoogleSigningIn : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthManager {
    companion object {
        const val DEBUG_SHA1 = "3F:A0:63:52:F8:AB:99:E4:7A:F4:DF:09:B7:1D:07:B0:7C:AB:BA:AE"
        const val DEBUG_SHA256 = "1A:CF:69:BE:52:E7:42:5A:32:C8:D9:AE:E8:D7:CC:BE:BF:3A:69:86:FD:84:4C:8E:DB:15:DD:F7:23:33:62:78"
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var currentVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Prevent duplicate concurrent requests
    @Volatile
    private var isRequestInProgress: Boolean = false

    val currentUser: FirebaseUser?
        get() = try { auth.currentUser } catch (_: Exception) { null }

    private fun ensureFirebaseInitialized(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error ensuring FirebaseApp is initialized", e)
        }
    }

    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSentCallback: ((String) -> Unit)? = null
    ) {
        if (isRequestInProgress) {
            Log.w("FirebaseAuthManager", "Request already in progress. Ignoring duplicate call.")
            return
        }

        ensureFirebaseInitialized(activity)
        isRequestInProgress = true
        _authState.value = AuthState.SendingPhoneCode

        // International format with default India (+91)
        val trimmed = phoneNumber.trim()
        val formattedNumber = when {
            trimmed.startsWith("+") -> trimmed
            trimmed.length == 10 -> "+91$trimmed"
            else -> "+91$trimmed"
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                isRequestInProgress = false
                Log.d("FirebaseAuthManager", "Instant phone verification completed")
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isRequestInProgress = false
                Log.e("FirebaseAuthManager", "Firebase Phone Auth verification failed: ${e::class.simpleName}")
                val errCode = (e as? FirebaseAuthException)?.errorCode ?: "PHONE_AUTH_FAILED"
                val diagnostic = when {
                    e.message?.contains("app-not-authorized", ignoreCase = true) == true ||
                    errCode == "ERROR_APP_NOT_AUTHORIZED" ->
                        "[$errCode] App not authorized. Ensure SHA-1 ($DEBUG_SHA1) and SHA-256 ($DEBUG_SHA256) are added to Firebase Console."
                    e.message?.contains("quota-exceeded", ignoreCase = true) == true ||
                    errCode == "ERROR_TOO_MANY_REQUESTS" ->
                        "[$errCode] SMS quota exceeded or throttled. Please try again later or use Firebase test phone numbers."
                    e.message?.contains("invalid-phone-number", ignoreCase = true) == true ||
                    errCode == "ERROR_INVALID_PHONE_NUMBER" ->
                        "[$errCode] Invalid phone number format. Please provide a valid 10-digit mobile number."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "[$errCode] Network error contacting Firebase Authentication."
                    else -> "[$errCode] ${e.localizedMessage ?: "Verification failed."}"
                }
                _authState.value = AuthState.Error(diagnostic)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isRequestInProgress = false
                Log.d("FirebaseAuthManager", "Phone verification code sent")
                currentVerificationId = verificationId
                resendToken = token
                _authState.value = AuthState.PhoneCodeSent(verificationId)
                onCodeSentCallback?.invoke(verificationId)
            }
        }

        try {
            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            resendToken?.let { builder.setForceResendingToken(it) }

            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            isRequestInProgress = false
            Log.e("FirebaseAuthManager", "Error initializing phone auth", e)
            val errCode = (e as? FirebaseAuthException)?.errorCode ?: "INIT_FAILED"
            _authState.value = AuthState.Error("[$errCode] ${e.message ?: "Could not initiate verification."}")
        }
    }

    fun verifyCode(code: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (isRequestInProgress) {
            Log.w("FirebaseAuthManager", "Verification already in progress. Ignoring duplicate call.")
            return
        }

        val verificationId = currentVerificationId
        if (verificationId == null) {
            val err = "[NO_VERIFICATION_ID] Missing verification session. Please request a new OTP code."
            _authState.value = AuthState.Error(err)
            onComplete?.invoke(false, err)
            return
        }

        isRequestInProgress = true
        _authState.value = AuthState.VerifyingCode

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
            signInWithPhoneCredential(credential, onComplete)
        } catch (e: Exception) {
            isRequestInProgress = false
            val errCode = (e as? FirebaseAuthException)?.errorCode ?: "INVALID_CODE_FORMAT"
            val err = "[$errCode] ${e.localizedMessage ?: "Invalid verification code format."}"
            _authState.value = AuthState.Error(err)
            onComplete?.invoke(false, err)
        }
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isRequestInProgress = false
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d("FirebaseAuthManager", "Phone sign in successful for user uid: ${user?.uid}")
                    _authState.value = AuthState.Success(user)
                    onComplete?.invoke(true, null)
                } else {
                    val ex = task.exception
                    val errCode = (ex as? FirebaseAuthException)?.errorCode ?: "PHONE_SIGNIN_FAILED"
                    val err = "[$errCode] ${ex?.localizedMessage ?: "Failed to sign in with code."}"
                    Log.e("FirebaseAuthManager", "Phone sign in failed: $errCode")
                    _authState.value = AuthState.Error(err)
                    onComplete?.invoke(false, err)
                }
            }
    }

    suspend fun signInWithGoogle(
        activity: Activity,
        serverClientId: String? = null
    ): Result<FirebaseUser> {
        if (isRequestInProgress) {
            return Result.failure(IllegalStateException("Sign in operation already in progress"))
        }

        ensureFirebaseInitialized(activity)
        val clientId = serverClientId?.takeIf { it.isNotBlank() } ?: getWebClientId(activity)
        if (clientId.isBlank()) {
            val missingMsg = "[CONFIG_PENDING] Google Sign-In requires an OAuth 2.0 Web Client ID. " +
                "In Firebase Console (Project: 'voltelite'), register the app's SHA-1 fingerprint ($DEBUG_SHA1), " +
                "enable Google Sign-In (Authentication > Sign-in method), and download the updated google-services.json."
            _authState.value = AuthState.Error(missingMsg)
            return Result.failure(IllegalStateException(missingMsg))
        }

        isRequestInProgress = true
        _authState.value = AuthState.GoogleSigningIn

        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).awaitTask()
                val user = authResult.user

                isRequestInProgress = false
                _authState.value = AuthState.Success(user)
                Result.success(user!!)
            } else {
                isRequestInProgress = false
                val err = "[UNSUPPORTED_CREDENTIAL] Unexpected credential type received from Credential Manager."
                _authState.value = AuthState.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            isRequestInProgress = false
            _authState.value = AuthState.Idle
            Log.d("FirebaseAuthManager", "User cancelled Google sign in dialog")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            isRequestInProgress = false
            val err = "[NO_CREDENTIAL] No Google accounts found on this device. Please add a Google account in Android Settings."
            Log.e("FirebaseAuthManager", "No credentials available: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        } catch (e: GetCredentialCustomException) {
            isRequestInProgress = false
            val isDevError = e.message?.contains("10:") == true || e.message?.contains("DEVELOPER_ERROR") == true
            val err = if (isDevError) {
                "[DEVELOPER_ERROR_10] SHA-1 fingerprint mismatch. Register $DEBUG_SHA1 for package com.aistudio.voltelite.evcapp in Firebase Console."
            } else {
                "[CREDENTIAL_ERROR] ${e.message ?: "Google Credential Manager error"}"
            }
            Log.e("FirebaseAuthManager", "Google sign-in error: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            isRequestInProgress = false
            val err = "[CREDENTIAL_EXCEPTION] ${e.localizedMessage ?: "Failed to get Google credentials."}"
            Log.e("FirebaseAuthManager", "Credential exception: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        } catch (e: FirebaseAuthException) {
            isRequestInProgress = false
            val err = "[${e.errorCode}] ${e.localizedMessage ?: "Firebase Google sign-in failed."}"
            Log.e("FirebaseAuthManager", "Firebase Auth exception: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        } catch (e: FirebaseException) {
            isRequestInProgress = false
            val err = "[FIREBASE_ERROR] ${e.localizedMessage ?: "Firebase error during Google sign-in."}"
            Log.e("FirebaseAuthManager", "Firebase exception: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        } catch (e: Exception) {
            isRequestInProgress = false
            val err = "[AUTH_ERROR] ${e.localizedMessage ?: "Google sign-in failed."}"
            Log.e("FirebaseAuthManager", "Google sign-in exception: $err")
            _authState.value = AuthState.Error(err)
            Result.failure(e)
        }
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            val tokenResult = user.getIdToken(forceRefresh).awaitTask()
            tokenResult.token
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Failed to retrieve Firebase ID token", e)
            null
        }
    }

    fun getWebClientId(context: Context): String {
        // 1. Check generated default_web_client_id in namespace (com.example)
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", "com.example")
            if (resId != 0) {
                val key = context.getString(resId)
                if (key.isNotBlank()) return key
            }
        } catch (_: Exception) {}

        // 2. Check default_web_client_id in context.packageName (com.aistudio.voltelite.evcapp)
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val key = context.getString(resId)
                if (key.isNotBlank()) return key
            }
        } catch (_: Exception) {}

        // 3. Check com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID
        try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GOOGLE_WEB_CLIENT_ID")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) return value
        } catch (_: Exception) {}

        // 4. Check application package BuildConfig
        try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val field = buildConfigClass.getField("GOOGLE_WEB_CLIENT_ID")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) return value
        } catch (_: Exception) {}

        return ""
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _authState.value = AuthState.Idle
        currentVerificationId = null
        resendToken = null
        isRequestInProgress = false
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        isRequestInProgress = false
    }
}
