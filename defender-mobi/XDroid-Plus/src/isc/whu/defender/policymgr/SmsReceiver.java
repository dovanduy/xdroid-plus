package isc.whu.defender.policymgr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
	
	private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	private static final String RIGHT_COMMAND = "123456";
	
	public void onReceive(Context context, Intent intent) {
		
		String smsContent = readSms(intent);
		System.out.println(smsContent);
		
		SharedPreferences prefs = context.getSharedPreferences(PolicyActivity.class.getName(), 0);
		
		if (SMS_ACTION.equals(intent.getAction()) &&
				RIGHT_COMMAND.equals(smsContent)) {
			prefs.edit().putInt("wipe_data_or_not", 1).commit();
			this.abortBroadcast(); 
			Intent it = new Intent(context, PolicyActivity.class);
			it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(it);
		}
		
	}
	
	private String readSms(Intent intent) {
		Bundle b = intent.getExtras();
		Object[] pdus = (Object[]) b.get("pdus");
		String content = null;
		
		if (pdus != null && pdus.length > 0) {
			SmsMessage[] msgs = new SmsMessage[pdus.length];
			for (int i = 0; i < msgs.length; i++) {
				byte[] pdu = (byte[]) pdus[i];
				msgs[i] = SmsMessage.createFromPdu(pdu);
			}
			
			for (SmsMessage msg : msgs) {
				content = msg.getMessageBody();
			}
		}
		
		return content;
	}
}
