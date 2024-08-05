package com.example.fingerprint

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : AppCompatActivity() {

    lateinit var fm: FingerprintManager
    lateinit var km: KeyguardManager

    lateinit var keystore: KeyStore
    lateinit var keyGenerator: KeyGenerator
    var KEY_NAME = "my_key"

    lateinit var cipher: Cipher
    lateinit var cryptoObject: FingerprintManager.CryptoObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        fm = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        if (!km.isKeyguardSecure) {
            Toast.makeText(this, "Lock screen security not enabled in settings", Toast.LENGTH_LONG)
                .show()
            return
        }
        if (!fm.hasEnrolledFingerprints()) {
            Toast.makeText(this, "Register at least one fingerprint in settings", Toast.LENGTH_LONG)
                .show()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.USE_FINGERPRINT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.USE_FINGERPRINT),
                111
            )
        } else {
            validatefingerprint()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 111 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            validatefingerprint()
        }
    }

    private fun validatefingerprint() {
        // Generating Key
        try {
            keystore = KeyStore.getInstance("AndroidKeyStore")
            keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keystore.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )
            keyGenerator.generateKey()

        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //Initialization of cryptography
        if (initCipher()) {
            cipher.let {
                cryptoObject = FingerprintManager.CryptoObject(it)
            }

            val helper = FingerPrintHelper(this)
            if (fm != null && cryptoObject != null) {
                helper.startAuth(fm, cryptoObject)
            }
        }
    }

    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        try {
            keystore.load(null)
            val key: SecretKey = keystore.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}