package com.example.noterello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.firebase.firestore.FirebaseFirestore;

public class CompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String ownerId = intent.getStringExtra("OWNER_ID");
        String boardId = intent.getStringExtra("BOARD_ID");
        String noteId = intent.getStringExtra("noteId");

        if (noteId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(ownerId)
                    .collection("boards").document(boardId)
                    .collection("notes").document(noteId)
                    .update("completed", true, "deadline", 0);

            // Zamknij powiadomienie po kliknięciu
            android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(noteId.hashCode());
        }
    }
}