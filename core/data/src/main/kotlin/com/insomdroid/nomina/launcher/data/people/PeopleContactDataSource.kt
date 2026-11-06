package com.insomdroid.nomina.data.people

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Contact details used only to enrich a person tile in the People widget. */
data class PeopleContact(
    val name: String,
    val lookupUri: String,
    val number: String?,
    val photoUri: String?,
    val id: Long,
)

/** Resolves a People-widget display name to matching contacts on the device. */
interface PeopleContactDataSource {
    suspend fun findMatches(query: String, limit: Int): List<PeopleContact>
}

/** Runtime permission seam for the People widget. */
fun interface PeoplePermissionChecker {
    fun has(permission: String): Boolean
}

/** ContentResolver-backed contact lookup used by the People widget. */
class ContentResolverPeopleContactDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : PeopleContactDataSource {

    override suspend fun findMatches(query: String, limit: Int): List<PeopleContact> =
        withContext(Dispatchers.IO) {
            val uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_FILTER_URI,
                Uri.encode(query),
            )
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
            )
            val out = ArrayList<PeopleContact>()
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val lookupColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                val photoColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
                val hasPhoneColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                while (cursor.moveToNext() && out.size < limit) {
                    val id = cursor.getLong(idColumn)
                    val lookup = cursor.getString(lookupColumn)
                    val name = cursor.getString(nameColumn) ?: continue
                    val photo = cursor.getString(photoColumn)
                    val number = if (cursor.getInt(hasPhoneColumn) > 0) primaryNumber(id) else null
                    val lookupUri = ContactsContract.Contacts.getLookupUri(id, lookup)?.toString()
                        ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id).toString()
                    out.add(PeopleContact(name, lookupUri, number, photo, id))
                }
            }
            out
        }

    private fun primaryNumber(contactId: Long): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )
        return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
    }
}

/** Checks the app's current runtime permission state for People-widget contact matching. */
class AndroidPeoplePermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : PeoplePermissionChecker {
    override fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
