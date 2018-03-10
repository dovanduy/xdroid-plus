package isc.whu.defender.xmonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.DeadObjectException;
import android.text.TextUtils;
import android.util.Log;

public class XContentResolver extends XHook {
	private Methods mMethod;

	private XContentResolver(Methods method, String restrictionName) {
		super(restrictionName, method.name(), null);
		mMethod = method;
	}

	private XContentResolver(Methods method, String restrictionName, int sdk) {
		super(restrictionName, "query", null, sdk);
		mMethod = method;
	}

	public String getClassName() {
		return (mMethod == Methods.cquery ? "android.content.ContentProviderClient"
				: "android.content.ContentResolver");
	}

	// @formatter:off

	// public static SyncInfo getCurrentSync()
	// static List<SyncInfo> getCurrentSyncs()
	// static SyncAdapterType[] getSyncAdapterTypes()

	// public Cursor query(Uri url, String[] projection, String selection,
	// String[] selectionArgs, String sortOrder)
	// public Cursor query(Uri url, String[] projection, String selection,
	// String[] selectionArgs, String sortOrder, CancellationSignal
	// cancellationSignal)

	// TODO: public final void registerContentObserver(Uri uri, boolean
	// notifyForDescendents, ContentObserver observer)

	// https://developers.google.com/gmail/android/
	// http://developer.android.com/reference/android/content/ContentResolver.html
	// http://developer.android.com/reference/android/content/ContentProviderClient.html

	// http://developer.android.com/reference/android/provider/Contacts.People.html
	// http://developer.android.com/reference/android/provider/ContactsContract.Contacts.html
	// http://developer.android.com/reference/android/provider/ContactsContract.Data.html
	// http://developer.android.com/reference/android/provider/ContactsContract.PhoneLookup.html
	// http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
	// http://developer.android.com/reference/android/provider/ContactsContract.RawContacts.html

	// frameworks/base/core/java/android/content/ContentResolver.java

	// @formatter:on

	private enum Methods {
		getCurrentSync, getCurrentSyncs, getSyncAdapterTypes, query, cquery
	};

	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		listHook.add(new XContentResolver(Methods.getCurrentSync,
				HookManager.cAccounts));
		listHook.add(new XContentResolver(Methods.getCurrentSyncs,
				HookManager.cAccounts));
		listHook.add(new XContentResolver(Methods.getSyncAdapterTypes,
				HookManager.cAccounts));
		listHook.add(new XContentResolver(Methods.query, null, 1));
		listHook.add(new XContentResolver(Methods.cquery, null, 1));
		return listHook;
	}

	@Override
	protected void before(XParam param) throws Throwable {
		if (mMethod == Methods.query || mMethod == Methods.cquery)
			try {
				handleUriBefore(param);
			} catch (DeadObjectException ignored) {
			} catch (Throwable ex) {
				Util.bug(this, ex);
			}
	}

	@Override
	protected void after(XParam param) throws Throwable {
		if (mMethod == Methods.getCurrentSync) {
			if (isRestricted(param))
				param.setResult(null);

		} else if (mMethod == Methods.getCurrentSyncs) {
			if (isRestricted(param))
				param.setResult(new ArrayList<SyncInfo>());

		} else if (mMethod == Methods.getSyncAdapterTypes) {
			if (isRestricted(param))
				param.setResult(new SyncAdapterType[0]);

		} else if (mMethod == Methods.query || mMethod == Methods.cquery) {
			try {
				handleUriAfter(param);
			} catch (DeadObjectException ignored) {
			} catch (Throwable ex) {
				Util.bug(this, ex);
			}

		} else
			Util.log(this, Log.WARN, "Unknown method=" + param.method.getName());
	}

	@SuppressLint("DefaultLocale")
	private void handleUriBefore(XParam param) throws Throwable {
		// Check URI
		if (param.args.length > 1 && param.args[0] instanceof Uri) {
			String uri = ((Uri) param.args[0]).toString().toLowerCase();
			String[] projection = (param.args[1] instanceof String[] ? (String[]) param.args[1]
					: null);
			Util.log(this, Log.INFO, "Before uri=" + uri);

			if (uri.startsWith("content://com.android.contacts/contacts/name_phone_or_email")) {
				// Do nothing

			} else if (uri
					.startsWith("content://com.android.contacts/contacts")
					|| uri.startsWith("content://com.android.contacts/data")
					|| uri.startsWith("content://com.android.contacts/phone_lookup")
					|| uri.startsWith("content://com.android.contacts/raw_contacts")) {
				String[] components = uri.replace("content://com.android.", "")
						.split("/");
				String methodName = components[0] + "/"
						+ components[1].split("\\?")[0];

				// if (isRestrictedExtra(param, HookManager.cContacts,
				// methodName,
				// uri)) {
				if (isRestrictedExtra(param, HookManager.cReadContacts,
						methodName, uri)) {
					// Modify projection
					boolean added = false;
					if (projection != null) {
						List<String> listProjection = new ArrayList<String>();
						listProjection.addAll(Arrays.asList(projection));
						String cid = getIdForUri(uri);
						if (cid != null && !listProjection.contains(cid)) {
							added = true;
							listProjection.add(cid);
						}
						param.args[1] = listProjection.toArray(new String[0]);
					}
					if (added)
						param.setObjectExtra("column_added", added);
				}
			}
		}
	}

	@SuppressLint("DefaultLocale")
	private void handleUriAfter(XParam param) throws Throwable {
		// Check URI
		if (param.args.length > 1 && param.args[0] instanceof Uri
				&& param.getResult() != null) {
			String uri = ((Uri) param.args[0]).toString().toLowerCase();
			Cursor cursor = (Cursor) param.getResult();
			Util.log(this, Log.INFO, "After uri=" + uri);

			if (uri.startsWith("content://applications")) {
				// Applications provider: allow selected applications
				if (isRestrictedExtra(param, HookManager.cSystem,
						"ApplicationsProvider", uri)) {
					MatrixCursor result = new MatrixCursor(
							cursor.getColumnNames());
					while (cursor.moveToNext()) {
						int colPackage = cursor.getColumnIndex("package");
						String packageName = (colPackage < 0 ? null : cursor
								.getString(colPackage));
						if (packageName != null
								&& XPackageManager
										.isPackageAllowed(packageName))
							copyColumns(cursor, result);
					}
					result.respond(cursor.getExtras());
					param.setResult(result);
					cursor.close();
				}

			} else if (uri
					.startsWith("content://com.google.android.gsf.gservices")) {
				// Google services provider: block only android_id
				if (param.args.length > 3 && param.args[3] != null) {
					List<String> listSelection = Arrays
							.asList((String[]) param.args[3]);
					if (listSelection.contains("android_id"))
						if (isRestrictedExtra(param,
								HookManager.cIdentification,
								"GservicesProvider", uri)) {
							int ikey = cursor.getColumnIndex("key");
							int ivalue = cursor.getColumnIndex("value");
							if (ikey == 0 && ivalue == 1
									&& cursor.getColumnCount() == 2) {
								MatrixCursor result = new MatrixCursor(
										cursor.getColumnNames());
								while (cursor.moveToNext()) {
									if ("android_id".equals(cursor
											.getString(ikey)))
										result.addRow(new Object[] {
												"android_id",
												HookManager.getDefacedProp(
														Binder.getCallingUid(),
														"GSF_ID") });
									else
										copyColumns(cursor, result);
								}
								result.respond(cursor.getExtras());
								param.setResult(result);
								cursor.close();
							} else
								Util.log(
										this,
										Log.ERROR,
										"Unexpected result uri=" + uri
												+ " columns="
												+ cursor.getColumnNames());
						}
				}

			} else if (uri
					.startsWith("content://com.android.contacts/contacts/name_phone_or_email")) {
				// Do nothing

			} else if (uri
					.startsWith("content://com.android.contacts/contacts")
					|| uri.startsWith("content://com.android.contacts/data")
					|| uri.startsWith("content://com.android.contacts/phone_lookup")
					|| uri.startsWith("content://com.android.contacts/raw_contacts")) {
				// Contacts provider: allow selected contacts
				String[] components = uri.replace("content://com.android.", "")
						.split("/");
				String methodName = components[0] + "/"
						+ components[1].split("\\?")[0];
				// if (isRestrictedExtra(param, HookManager.cContacts,
				// methodName,
				// uri)) {
				if (isRestrictedExtra(param, HookManager.cReadContacts,
						methodName, uri)) {
					// Modify column names back
					Object column_added = param.getObjectExtra("column_added");
					boolean added = (column_added == null ? false
							: (Boolean) param.getObjectExtra("column_added"));

					List<String> listColumn = new ArrayList<String>();
					listColumn.addAll(Arrays.asList(cursor.getColumnNames()));
					if (added)
						listColumn.remove(listColumn.size() - 1);

					MatrixCursor result = new MatrixCursor(
							listColumn.toArray(new String[0]));

					// Filter rows
					String cid = getIdForUri(uri);
					int iid = (cid == null ? -1 : cursor.getColumnIndex(cid));
					if (iid >= 0)
						while (cursor.moveToNext()) {
							// Check if allowed
							long id = cursor.getLong(iid);
							boolean allowed = HookManager.getSettingBool(
									Binder.getCallingUid(), Meta.cTypeContact,
									Long.toString(id), false, true);
							if (allowed)
								copyColumns(cursor, result, listColumn.size());
						}
					else
						Util.log(this, Log.ERROR, "ID missing uri=" + uri
								+ " added=" + added + "/" + cid + " columns="
								+ TextUtils.join(",", cursor.getColumnNames()));

					result.respond(cursor.getExtras());
					param.setResult(result);
					cursor.close();
				}

			} else {
				// Other uri restrictions
				String restrictionName = null;
				String methodName = null;
				if (uri.startsWith("content://browser")) {
					// restrictionName = HookManager.cBrowser;
					restrictionName = HookManager.cWebHistory;
					methodName = "BrowserProvider2";
				}

				else if (uri.startsWith("content://com.android.calendar")) {
					restrictionName = HookManager.cCalendar;
					methodName = "CalendarProvider2";
				}

				else if (uri.startsWith("content://call_log")) {
					// restrictionName = HookManager.cPhone;
					restrictionName = HookManager.cReadCalllog;
					methodName = "CallLogProvider";
				}

				else if (uri.startsWith("content://contacts/people")) {
					// restrictionName = HookManager.cContacts;
					restrictionName = HookManager.cReadContacts;
					methodName = "contacts/people";
				}

				else if (uri
						.startsWith("content://com.android.contacts/profile")) {
					// restrictionName = HookManager.cContacts;
					restrictionName = HookManager.cReadContacts;
					methodName = "contacts/profile";
				}

				else if (uri.startsWith("content://com.android.contacts")) {
					// restrictionName = HookManager.cContacts;
					restrictionName = HookManager.cReadContacts;
					methodName = "ContactsProvider2"; // fall-back
				}

				else if (uri.startsWith("content://downloads")) {
					restrictionName = HookManager.cBrowser;
					methodName = "Downloads";
				}

				else if (uri.startsWith("content://com.android.email.provider")) {
					restrictionName = HookManager.cEMail;
					methodName = "EMailProvider";
				}

				else if (uri.startsWith("content://com.google.android.gm")) {
					restrictionName = HookManager.cEMail;
					methodName = "GMailProvider";
				}

				else if (uri.startsWith("content://icc")) {
					// restrictionName = HookManager.cContacts;
					restrictionName = HookManager.cReadContacts;
					methodName = "IccProvider";
				}

				else if (uri.startsWith("content://mms")) {
					// restrictionName = HookManager.cMessages;
					restrictionName = HookManager.cReadMessage;
					methodName = "MmsProvider";
				}

				else if (uri.startsWith("content://mms-sms")) {
					// restrictionName = HookManager.cMessages;
					restrictionName = HookManager.cReadMessage;
					methodName = "MmsSmsProvider";
				}

				else if (uri.startsWith("content://sms")) {
					// restrictionName = HookManager.cMessages;
					restrictionName = HookManager.cReadMessage;
					methodName = "SmsProvider";
				}

				else if (uri.startsWith("content://telephony")) {
					restrictionName = HookManager.cPhone;
					methodName = "TelephonyProvider";
				}

				else if (uri.startsWith("content://user_dictionary")) {
					restrictionName = HookManager.cDictionary;
					methodName = "UserDictionary";
				}

				else if (uri.startsWith("content://com.android.voicemail")) {
					// restrictionName = HookManager.cMessages;
					restrictionName = HookManager.cReadMessage;
					methodName = "VoicemailContentProvider";
				}

				// Check if know / restricted
				if (restrictionName != null && methodName != null) {
					if (isRestrictedExtra(param, restrictionName, methodName,
							uri)) {
						// Return empty cursor
						MatrixCursor result = new MatrixCursor(
								cursor.getColumnNames());
						result.respond(cursor.getExtras());
						param.setResult(result);
						cursor.close();
					}
				}
			}
		}
	}

	private String getIdForUri(String uri) {
		if (uri.startsWith("content://com.android.contacts/contacts"))
			return "_id";
		else if (uri.startsWith("content://com.android.contacts/data"))
			return "contact_id";
		else if (uri.startsWith("content://com.android.contacts/phone_lookup"))
			return "_id";
		else if (uri.startsWith("content://com.android.contacts/raw_contacts"))
			return "contact_id";
		else
			Util.log(this, Log.ERROR, "Unexpected uri=" + uri);
		return null;
	}

	private void copyColumns(Cursor cursor, MatrixCursor result) {
		copyColumns(cursor, result, cursor.getColumnCount());
	}

	private void copyColumns(Cursor cursor, MatrixCursor result, int count) {
		try {
			Object[] columns = new Object[count];
			for (int i = 0; i < count; i++)
				switch (cursor.getType(i)) {
				case Cursor.FIELD_TYPE_NULL:
					columns[i] = null;
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					columns[i] = cursor.getInt(i);
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					columns[i] = cursor.getFloat(i);
					break;
				case Cursor.FIELD_TYPE_STRING:
					columns[i] = cursor.getString(i);
					break;
				case Cursor.FIELD_TYPE_BLOB:
					columns[i] = cursor.getBlob(i);
					break;
				default:
					Util.log(this, Log.WARN, "Unknown cursor data type="
							+ cursor.getType(i));
				}
			result.addRow(columns);
		} catch (Throwable ex) {
			Util.bug(this, ex);
		}
	}

	@SuppressWarnings("unused")
	private void _dumpCursor(String uri, Cursor cursor) {
		_dumpHeader(uri, cursor);
		int i = 0;
		while (cursor.moveToNext() && i++ < 10)
			_dumpColumns(cursor, "");
		cursor.moveToFirst();
	}

	private void _dumpHeader(String uri, Cursor cursor) {
		Util.log(this, Log.WARN, TextUtils.join(", ", cursor.getColumnNames())
				+ " uri=" + uri);
	}

	private void _dumpColumns(Cursor cursor, String msg) {
		String[] columns = new String[cursor.getColumnCount()];
		for (int i = 0; i < cursor.getColumnCount(); i++)
			switch (cursor.getType(i)) {
			case Cursor.FIELD_TYPE_NULL:
				columns[i] = null;
				break;
			case Cursor.FIELD_TYPE_INTEGER:
				columns[i] = Integer.toString(cursor.getInt(i));
				break;
			case Cursor.FIELD_TYPE_FLOAT:
				columns[i] = Float.toString(cursor.getFloat(i));
				break;
			case Cursor.FIELD_TYPE_STRING:
				columns[i] = cursor.getString(i);
				break;
			case Cursor.FIELD_TYPE_BLOB:
				columns[i] = "[blob]";
				break;
			default:
				Util.log(this, Log.WARN,
						"Unknown cursor data type=" + cursor.getType(i));
			}
		Util.log(this, Log.WARN, TextUtils.join(", ", columns) + " " + msg);
	}
}
