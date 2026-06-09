package com.example.noterello;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String noteId = intent.getStringExtra("noteId");

        // Wyciągamy dane tablicy
        String boardId = intent.getStringExtra("BOARD_ID");
        String ownerId = intent.getStringExtra("OWNER_ID");
        String boardName = intent.getStringExtra("BOARD_NAME");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "noterello_reminders";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        // Pakujemy dane do nowej aktywności
        Intent activityIntent = new Intent(context, BoardViewActivity.class);
        activityIntent.putExtra("BOARD_ID", boardId);
        activityIntent.putExtra("OWNER_ID", ownerId);
        activityIntent.putExtra("BOARD_NAME", boardName);
        activityIntent.putExtra("OPEN_NOTE_ID", noteId); // Nasza nowa flaga dla konkretnej notatki

        PendingIntent pendingIntent = PendingIntent.getActivity(context, noteId.hashCode(), activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        Intent doneIntent = new Intent(context, CompleteReceiver.class);
        doneIntent.putExtra("OWNER_ID", ownerId);
        doneIntent.putExtra("BOARD_ID", boardId);
        doneIntent.putExtra("noteId", noteId);

        PendingIntent donePendingIntent = PendingIntent.getBroadcast(context, noteId.hashCode() + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);



        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Deadline: " + title)
                .setContentText("Time to complete this task!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_agenda, "Mark as Done", donePendingIntent) // DODANY PRZYCISK
                .setContentIntent(pendingIntent);

        manager.notify(noteId.hashCode(), builder.build());
    }
}