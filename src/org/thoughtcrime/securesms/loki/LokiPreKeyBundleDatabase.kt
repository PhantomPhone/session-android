package org.thoughtcrime.securesms.loki

import android.content.ContentValues
import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import org.thoughtcrime.securesms.crypto.IdentityKeyUtil
import org.thoughtcrime.securesms.crypto.PreKeyUtil
import org.thoughtcrime.securesms.database.Database
import org.thoughtcrime.securesms.database.DatabaseFactory
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper
import org.thoughtcrime.securesms.util.Base64
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.loki.messaging.LokiPreKeyDatabaseProtocol

class LokiPreKeyBundleDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper), LokiPreKeyDatabaseProtocol {

    companion object {
        private val tableName = "loki_pre_key_bundle_database"
        private val hexEncodedPublicKey = "public_key"
        private val preKeyID = "pre_key_id"
        private val preKeyPublic = "pre_key_public"
        private val signedPreKeyID = "signed_pre_key_id"
        private val signedPreKeyPublic = "signed_pre_key_public"
        private val signedPreKeySignature = "signed_pre_key_signature"
        private val identityKey = "identity_key"
        private val deviceID = "device_id"
        private val registrationID = "registration_id"
        @JvmStatic val createTableCommand = "CREATE TABLE $tableName (" + "$hexEncodedPublicKey TEXT PRIMARY KEY," + "$preKeyID INTEGER," +
            "$preKeyPublic TEXT NOT NULL," + "$signedPreKeyID INTEGER," + "$signedPreKeyPublic TEXT NOT NULL," +
            "$signedPreKeySignature TEXT," + "$identityKey TEXT NOT NULL," + "$deviceID INTEGER," + "$registrationID INTEGER" + ");"
    }

    fun generatePreKeyBundle(hexEncodedPublicKey: String): PreKeyBundle? {
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context)
        val signedPreKey = PreKeyUtil.getActiveSignedPreKey(context) ?: return null
        val preKeyRecord = DatabaseFactory.getLokiPreKeyRecordDatabase(context).getOrCreatePreKey(hexEncodedPublicKey)
        val registrationID = TextSecurePreferences.getLocalRegistrationId(context)
        if (registrationID == 0) return null
        val deviceID = SignalServiceAddress.DEFAULT_DEVICE_ID
        return PreKeyBundle(registrationID, deviceID,preKeyRecord.id, preKeyRecord.keyPair.publicKey, signedPreKey.id, signedPreKey.keyPair.publicKey, signedPreKey.signature, identityKeyPair.publicKey)
    }

    override fun getPreKeyBundle(hexEncodedPublicKey: String): PreKeyBundle? {
        val database = databaseHelper.readableDatabase
        return database.get(tableName, "${Companion.hexEncodedPublicKey} = ?", arrayOf( hexEncodedPublicKey )) { cursor ->
            val registrationID = cursor.getInt(registrationID)
            val deviceID = cursor.getInt(deviceID)
            val preKeyID = cursor.getInt(preKeyID)
            val preKey = Curve.decodePoint(cursor.getBase64EncodedData(preKeyPublic), 0)
            val signedPreKeyID = cursor.getInt(signedPreKeyID)
            val signedPreKey = Curve.decodePoint(cursor.getBase64EncodedData(signedPreKeyPublic), 0)
            val signedPreKeySignature = cursor.getBase64EncodedData(signedPreKeySignature)
            val identityKey = IdentityKey(cursor.getBase64EncodedData(identityKey), 0)
            PreKeyBundle(registrationID, deviceID, preKeyID, preKey, signedPreKeyID, signedPreKey, signedPreKeySignature, identityKey)
        }
    }

    fun setPreKeyBundle(hexEncodedPublicKey: String, preKeyBundle: PreKeyBundle) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(9)
        contentValues.put(registrationID, preKeyBundle.registrationId)
        contentValues.put(deviceID, preKeyBundle.deviceId)
        contentValues.put(preKeyID, preKeyBundle.preKeyId)
        contentValues.put(preKeyPublic, Base64.encodeBytes(preKeyBundle.preKey.serialize()))
        contentValues.put(signedPreKeyID, preKeyBundle.signedPreKeyId)
        contentValues.put(signedPreKeyPublic, Base64.encodeBytes(preKeyBundle.signedPreKey.serialize()))
        contentValues.put(signedPreKeySignature, Base64.encodeBytes(preKeyBundle.signedPreKeySignature))
        contentValues.put(identityKey, Base64.encodeBytes(preKeyBundle.identityKey.serialize()))
        contentValues.put(Companion.hexEncodedPublicKey, hexEncodedPublicKey)
        database.insertWithOnConflict(tableName, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun removePreKeyBundle(hexEncodedPublicKey: String) {
        val database = databaseHelper.writableDatabase
        database.delete(tableName, "${Companion.hexEncodedPublicKey} = ?", arrayOf( hexEncodedPublicKey ))
    }
}