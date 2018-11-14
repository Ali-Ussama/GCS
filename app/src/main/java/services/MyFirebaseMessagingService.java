package services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import activities.MapEditorActivity;
import com.gcs.riyadh.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "Firebase";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> map = remoteMessage.getData();
        String adminNotes = map.get("AdminNotes");
        String serviceName = map.get("ANAME");
        String serviceCategory = map.get("CATEGORY_A");
        String serviceId = map.get("OBJECTID");

        sendNotification(adminNotes,serviceName,serviceCategory,serviceId);
    }

    private void sendNotification(String adminNotes ,String serviceName,String serviceCategory,String serviceId) {
        Intent intent = new Intent(this, MapEditorActivity.class);
        intent.putExtra("OBJECTID",serviceId);
        intent.setAction("Notification");
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_speaker_notes_black_24dp)
                .setContentTitle(this.getString(R.string.notes) + " " + serviceName)
                .setContentText(adminNotes)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}