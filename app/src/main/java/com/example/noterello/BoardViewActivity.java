package com.example.noterello;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardViewActivity extends AppCompatActivity {

    private TextView tvBoardName;
    private RecyclerView rvNotes;
    private FloatingActionButton fabAddNote;
    private ImageView btnShareBoard;
    private LinearLayout llAvatars;

    private MixedAdapter adapter;
    private List<Object> displayList;
    private List<Note> rawNotesList;

    private String currentSearchQuery = "";
    private boolean isDragging = false;
    private String lastAddedNoteId = "";
    private String pendingNoteToOpen = null;

    private String currentImageBase64 = "";
    private ImageView currentDialogImageView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String boardId;
    private String ownerId;
    private String currentUserRole = "Viewer";
    private TextView tvEmptyState;
    private ImageView ivEmptyStateArrow;

    private View rootLayoutBoardView;

    private void listenToBoardDetails() {
        db.collection("users").document(ownerId).collection("boards").document(boardId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) return;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String bg = documentSnapshot.getString("boardBackground");
                        if (bg != null) {
                            applyBoardBackground(bg);
                        } else {
                            applyBoardBackground("default"); // Default color
                        }
                    }
                });
    }

    private void applyBoardBackground(String bgType) {
        if (rootLayoutBoardView == null) return;

        switch (bgType) {
            case "midnight":
                rootLayoutBoardView.setBackgroundColor(Color.parseColor("#121212"));
                tvBoardName.setTextColor(Color.WHITE);
                break;
            case "blueprint":
                rootLayoutBoardView.setBackgroundColor(Color.parseColor("#1976D2"));
                tvBoardName.setTextColor(Color.WHITE);
                break;
            case "pastel1":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel1);
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            case "pastel2":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel2);
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            case "pastel3":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel3);
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            case "pastel4":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel4);
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            case "pastel5":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel5);
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            case "pastel6":
                rootLayoutBoardView.setBackgroundResource(R.drawable.pastel6); // <-- Tutaj poprawione na pastel6
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
            default:
                rootLayoutBoardView.setBackgroundColor(Color.parseColor("#F5F5F5"));
                tvBoardName.setTextColor(Color.parseColor("#333333"));
                break;
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Reminders won't work without permission.", Toast.LENGTH_LONG).show();
                }
            }
    );

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_view);
        rootLayoutBoardView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);

        tvBoardName = findViewById(R.id.tvBoardName);
        rvNotes = findViewById(R.id.rvNotes);
        fabAddNote = findViewById(R.id.fabAddNote);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ivEmptyStateArrow = findViewById(R.id.ivEmptyStateArrow);
        btnShareBoard = findViewById(R.id.btnShareBoard);
        llAvatars = findViewById(R.id.llAvatars);

        askNotificationPermission();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Auth error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setAddDuration(200);
        animator.setRemoveDuration(200);
        animator.setMoveDuration(200);
        animator.setChangeDuration(200);
        rvNotes.setItemAnimator(animator);

        boardId = getIntent().getStringExtra("BOARD_ID");
        String boardName = getIntent().getStringExtra("BOARD_NAME");
        ownerId = getIntent().getStringExtra("OWNER_ID");
        pendingNoteToOpen = getIntent().getStringExtra("OPEN_NOTE_ID");




        if (ownerId == null || ownerId.isEmpty()) {
            ownerId = currentUserId;
        }

        if (boardName != null) {
            tvBoardName.setText(boardName);
        }

        displayList = new ArrayList<>();
        rawNotesList = new ArrayList<>();

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvNotes.setLayoutManager(layoutManager);

        adapter = new MixedAdapter(displayList);
        rvNotes.setAdapter(adapter);

        setupDragAndDrop();

        if (boardId != null) {
            loadRoleAndNotes();
            loadBoardMembersAvatars();
            listenToBoardDetails();
        }

        fabAddNote.setOnClickListener(v -> showNoteDialog(null));
        btnShareBoard.setOnClickListener(v -> showShareDialog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        llAvatars.setOnClickListener(v -> showShareDialog());
        tvBoardName.setOnClickListener(v -> showShareDialog());

        EditText etSearch = findViewById(R.id.etSearch);
        ImageView ivSearch = findViewById(R.id.ivSearch);

        ivSearch.setOnClickListener(v -> {
            if (etSearch.getVisibility() == View.VISIBLE) {
                etSearch.setVisibility(View.GONE);
                etSearch.setText("");
            } else {
                etSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                sortNotesAndAddDivider();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarm(String noteId, String title, long time) {
        if (noteId == null || time <= System.currentTimeMillis()) return;

        try {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("noteId", noteId);
            intent.putExtra("BOARD_ID", boardId);
            intent.putExtra("OWNER_ID", ownerId);
            intent.putExtra("BOARD_NAME", tvBoardName.getText().toString());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    noteId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
                    } else {
                        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelAlarm(String noteId) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, noteId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("The app needs this permission to send you deadline reminders.")
                        .setPositiveButton("OK", (dialog, which) -> requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS))
                        .setNegativeButton("No thanks", null)
                        .show();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            int maxSize = 800;
            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(selectedImage, width, height, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();

            currentImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            if (currentDialogImageView != null) {
                currentDialogImageView.setImageBitmap(resizedBitmap);
            }
            Toast.makeText(this, "Image ready!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBoardMembersAvatars() {
        if (llAvatars == null) return;
        llAvatars.removeAllViews();
        db.collection("users").document(ownerId).get().addOnSuccessListener(ownerDoc -> {
            if (ownerDoc.exists()) { addAvatarToHeader(ownerDoc.toObject(User.class)); }
            db.collection("invitations").whereEqualTo("boardId", boardId).whereEqualTo("status", "accepted")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        if (llAvatars.getChildCount() > 1) llAvatars.removeViews(1, llAvatars.getChildCount() - 1);
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                String toEmail = doc.getString("toUserEmail");
                                // Bezpiecznik: sprawdzamy czy mail na pewno istnieje
                                if (toEmail != null && !toEmail.isEmpty()) {
                                    db.collection("users").whereEqualTo("email", toEmail).get()
                                            .addOnSuccessListener(userSnaps -> {
                                                if (!userSnaps.isEmpty()) addAvatarToHeader(userSnaps.getDocuments().get(0).toObject(User.class));
                                            });
                                }
                            }
                        }
                    });
        });
    }

    private void addAvatarToHeader(User user) {
        if (user == null) return;
        TextView tv = new TextView(this);
        tv.setText(user.getInitials() != null ? user.getInitials() : "?");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        int size = (int) (32 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        if (llAvatars.getChildCount() > 0) params.setMargins((int) (-8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        else params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);

        // Zabezpieczenie przed błędem parsowania pustego koloru
        try {
            shape.setColor(Color.parseColor(user.getAvatarColor()));
        } catch (Exception e) {
            shape.setColor(Color.parseColor("#448AFF")); // Domyślny niebieski
        }

        shape.setStroke((int) (2 * getResources().getDisplayMetrics().density), Color.parseColor("#FFFFFF"));
        tv.setBackground(shape);
        llAvatars.addView(tv);
    }

    private void loadRoleAndNotes() {
        if (currentUserId.equals(ownerId)) {
            currentUserRole = "Admin";
            fabAddNote.setVisibility(View.VISIBLE);
        } else {
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null) {
                db.collection("invitations").whereEqualTo("boardId", boardId).whereEqualTo("toUserEmail", email.toLowerCase()).whereEqualTo("status", "accepted")
                        .addSnapshotListener((value, error) -> {
                            if (error != null) return;
                            if (value != null && !value.isEmpty()) currentUserRole = value.getDocuments().get(0).toObject(Invitation.class).getRole();
                            else currentUserRole = "Viewer";
                            fabAddNote.setVisibility(currentUserRole.equals("Admin") ? View.VISIBLE : View.GONE);
                            if (adapter != null) adapter.notifyDataSetChanged();
                        });
            }
        }
        loadNotesFromFirebase();
    }

    private void loadNotesFromFirebase() {
        db.collection("users").document(ownerId).collection("boards").document(boardId).collection("notes").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || isDragging || value == null) return;
                    java.util.Map<String, Note> freshNotesMap = new java.util.LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Note n = doc.toObject(Note.class);
                        n.setId(doc.getId());
                        freshNotesMap.put(n.getId(), n);
                    }
                    List<Note> merged = new ArrayList<>();
                    for (Note localNote : rawNotesList) {
                        Note fresh = freshNotesMap.remove(localNote.getId());
                        if (fresh != null) {
                            fresh.setTimestamp(localNote.getTimestamp());
                            merged.add(fresh);
                        }
                    }
                    merged.addAll(freshNotesMap.values());
                    rawNotesList.clear();
                    rawNotesList.addAll(merged);
                    sortNotesAndAddDivider();

                    if (pendingNoteToOpen != null && !rawNotesList.isEmpty()) {
                        for (Note n : rawNotesList) {
                            if (n.getId().equals(pendingNoteToOpen)) {
                                showNoteDialog(n);
                                pendingNoteToOpen = null;
                                break;
                            }
                        }
                    }
                });
    }

    private void setupDragAndDrop() {
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN | androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT, 0) {
                    int draggedFrom = -1, draggedTo = -1;
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        if (currentUserRole.equals("Viewer")) return makeMovementFlags(0, 0);
                        return makeMovementFlags(androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN | androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT, 0);
                    }
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        int fromPosition = viewHolder.getAdapterPosition();
                        int toPosition = target.getAdapterPosition();
                        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) return false;
                        Object sourceObj = displayList.get(fromPosition);
                        Object targetObj = displayList.get(toPosition);
                        if (targetObj instanceof String) return false;
                        if (sourceObj instanceof Note && targetObj instanceof Note) {
                            if (((Note) sourceObj).isPinned() != ((Note) targetObj).isPinned()) return false;
                        }
                        if (draggedFrom == -1) draggedFrom = fromPosition;
                        draggedTo = toPosition;
                        Collections.swap(displayList, fromPosition, toPosition);
                        adapter.notifyItemMoved(fromPosition, toPosition);
                        return true;
                    }
                    @Override
                    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                        super.onSelectedChanged(viewHolder, actionState);
                        if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                            isDragging = true;
                            viewHolder.itemView.animate().scaleX(1.07f).scaleY(1.07f).translationZ(28f).setDuration(100).start();
                        }
                    }
                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);
                        viewHolder.itemView.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(100).start();
                        if (draggedFrom != -1 && draggedTo != -1 && draggedFrom != draggedTo) {
                            updateNoteOrderInFirebase(draggedTo, () -> {
                                rawNotesList.clear();
                                for (Object obj : displayList) if (obj instanceof Note) rawNotesList.add((Note) obj);
                            });
                        }
                        draggedFrom = -1;
                        draggedTo = -1;
                        viewHolder.itemView.postDelayed(() -> isDragging = false, 300);
                    }
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    }
                });
        itemTouchHelper.attachToRecyclerView(rvNotes);
    }

    private void updateNoteOrderInFirebase(int finalPosition, Runnable onComplete) {
        Object currentObj = displayList.get(finalPosition);
        if (!(currentObj instanceof Note)) { if (onComplete != null) onComplete.run(); return; }
        Note draggedNote = (Note) currentObj;
        double prevT = 0, nextT = 0;
        for (int i = finalPosition - 1; i >= 0; i--) if (displayList.get(i) instanceof Note) { prevT = ((Note) displayList.get(i)).getTimestamp(); break; }
        for (int i = finalPosition + 1; i < displayList.size(); i++) if (displayList.get(i) instanceof Note) { nextT = ((Note) displayList.get(i)).getTimestamp(); break; }
        double newT = (prevT == 0 && nextT == 0) ? 0 : (prevT == 0) ? nextT - 100000.0 : (nextT == 0) ? prevT + 100000.0 : (prevT + nextT) / 2.0;
        if (newT == 0) { if (onComplete != null) onComplete.run(); return; }
        draggedNote.setTimestamp(newT);
        db.collection("users").document(ownerId).collection("boards").document(boardId).collection("notes").document(draggedNote.getId()).update("timestamp", newT)
                .addOnSuccessListener(aVoid -> { if (onComplete != null) onComplete.run(); }).addOnFailureListener(e -> { if (onComplete != null) onComplete.run(); });
    }

    private void sortNotesAndAddDivider() {
        List<Note> pinnedNotes = new ArrayList<>();
        List<Note> normalNotes = new ArrayList<>();
        for (Note n : rawNotesList) {
            boolean matches = currentSearchQuery.isEmpty() ||
                    (n.getTitle() != null && n.getTitle().toLowerCase().contains(currentSearchQuery)) ||
                    (n.getContent() != null && n.getContent().toLowerCase().contains(currentSearchQuery));
            if (matches) {
                if (n.isPinned()) pinnedNotes.add(n);
                else normalNotes.add(n);
            }
        }

        List<Object> newList = new ArrayList<>(pinnedNotes);
        if (!pinnedNotes.isEmpty() && !normalNotes.isEmpty()) {
            newList.add("DIVIDER");
        }
        newList.addAll(normalNotes);

        // Wykryj czy zmienił się zestaw przypiętych notatek
        java.util.Set<String> oldPinnedIds = new java.util.HashSet<>();
        for (Object o : displayList) {
            if (o instanceof Note && ((Note) o).isPinned()) {
                oldPinnedIds.add(((Note) o).getId());
            }
        }
        java.util.Set<String> newPinnedIds = new java.util.HashSet<>();
        for (Note n : pinnedNotes) {
            newPinnedIds.add(n.getId());
        }
        boolean pinnedChanged = !oldPinnedIds.equals(newPinnedIds);

        if (pinnedChanged) {
            // Pełne odświeżenie – omijamy animację DiffUtil która dezorientuje SGLM
            displayList.clear();
            displayList.addAll(newList);
            adapter.notifyDataSetChanged();
            StaggeredGridLayoutManager lm = (StaggeredGridLayoutManager) rvNotes.getLayoutManager();
            if (lm != null) {
                // Post – czekamy aż RecyclerView przetworzy notifyDataSetChanged
                rvNotes.post(() -> lm.invalidateSpanAssignments());
            }
        } else {
            // Normalny tryb z DiffUtil (bez zmiany pinned – kolejność kolumn bezpieczna)
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult =
                    androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                        @Override public int getOldListSize() { return displayList.size(); }
                        @Override public int getNewListSize() { return newList.size(); }
                        @Override
                        public boolean areItemsTheSame(int oldPos, int newPos) {
                            Object o = displayList.get(oldPos), n = newList.get(newPos);
                            if (o instanceof Note && n instanceof Note)
                                return ((Note) o).getId().equals(((Note) n).getId());
                            return o.equals(n);
                        }
                        @Override
                        public boolean areContentsTheSame(int oldPos, int newPos) {
                            Object o = displayList.get(oldPos), n = newList.get(newPos);
                            if (o instanceof Note && n instanceof Note) {
                                Note on = (Note) o, nn = (Note) n;
                                return String.valueOf(on.getTitle()).equals(String.valueOf(nn.getTitle())) &&
                                        String.valueOf(on.getContent()).equals(String.valueOf(nn.getContent())) &&
                                        String.valueOf(on.getColor()).equals(String.valueOf(nn.getColor())) &&
                                        on.isPinned() == nn.isPinned() &&
                                        on.isFullWidth() == nn.isFullWidth() &&
                                        on.getDeadline() == nn.getDeadline() &&
                                        on.isCompleted() == nn.isCompleted() &&
                                        String.valueOf(on.getType()).equals(String.valueOf(nn.getType())) &&
                                        String.valueOf(on.getImageUrl()).equals(String.valueOf(nn.getImageUrl())) &&
                                        String.valueOf(on.getChecklist()).equals(String.valueOf(nn.getChecklist()));
                            }
                            return o.equals(n);
                        }
                    });
            displayList.clear();
            displayList.addAll(newList);
            diffResult.dispatchUpdatesTo(adapter);
            StaggeredGridLayoutManager lm = (StaggeredGridLayoutManager) rvNotes.getLayoutManager();
            if (lm != null) {
                lm.invalidateSpanAssignments();
            }
        }

        if (tvEmptyState != null && ivEmptyStateArrow != null) {
            if (rawNotesList.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                ivEmptyStateArrow.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                ivEmptyStateArrow.setVisibility(View.GONE);
            }
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        final long[] selectedDeadline = { noteToEdit != null ? noteToEdit.getDeadline() : 0 };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int targetHeight = (int) (displayMetrics.heightPixels * 0.95);

        builder.setView(view);
        final androidx.cardview.widget.CardView outerCardView = view.findViewById(R.id.outerCardView);
        final androidx.cardview.widget.CardView dialogCardView = view.findViewById(R.id.dialogCardView);
        final ImageView btnViewNote = view.findViewById(R.id.btnViewNote);
        final EditText etTitle = view.findViewById(R.id.etNoteTitle);
        final EditText etContent = view.findViewById(R.id.etNoteContent);
        final RadioGroup rgColors = view.findViewById(R.id.rgColors);
        final ToggleButton btnSquare = view.findViewById(R.id.btnSizeSquare);
        final ToggleButton btnRectangle = view.findViewById(R.id.btnSizeRectangle);
        final CheckBox cbPinned = view.findViewById(R.id.cbPinned);
        final Button btnSave = view.findViewById(R.id.btnAdd);
        final ImageView btnClose = view.findViewById(R.id.btnClose);
        final ImageView btnDelete = view.findViewById(R.id.btnDeleteNote);
        final TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        final RadioGroup rgNoteType = view.findViewById(R.id.rgNoteType);
        final LinearLayout layoutChecklist = view.findViewById(R.id.layoutChecklist);
        final RecyclerView rvChecklistItems = view.findViewById(R.id.rvChecklistItems);
        final Button btnAddChecklistItem = view.findViewById(R.id.btnAddChecklistItem);
        final LinearLayout layoutImage = view.findViewById(R.id.layoutImage);
        final ImageView ivNoteImage = view.findViewById(R.id.ivNoteImage);
        final Button btnPickImage = view.findViewById(R.id.btnPickImage);
        final ImageView btnEditNote = view.findViewById(R.id.btnEditNote);
        final LinearLayout llEditControls = view.findViewById(R.id.llEditControls);
        final TextView tvReminderValue = view.findViewById(R.id.tvReminderValue);
        final Button btnSetReminder = view.findViewById(R.id.btnSetReminder);
        final Button btnDone = view.findViewById(R.id.btnDone);
        final boolean[] isEditMode = { noteToEdit == null };

        currentImageBase64 = "";
        currentDialogImageView = ivNoteImage;

        ivNoteImage.setOnClickListener(v -> {
            if (!isEditMode[0] && currentImageBase64 != null && !currentImageBase64.isEmpty()) {
                try {
                    byte[] dec = Base64.decode(currentImageBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(dec, 0, dec.length);
                    showFullscreenImageDialog(decodedByte);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        final AlertDialog dialog = builder.create();

        List<ChecklistItem> currentChecklist = new ArrayList<>();
        if (noteToEdit != null && noteToEdit.getChecklist() != null) {
            currentChecklist.addAll(noteToEdit.getChecklist());
        }


        boolean userCanEdit = !currentUserRole.equals("Viewer");

        Runnable autoSaveChecklist = () -> {
            if (noteToEdit != null) {
                db.collection("users").document(ownerId).collection("boards").document(boardId)
                        .collection("notes").document(noteToEdit.getId())
                        .update("checklist", currentChecklist);
            }
        };

        ChecklistDialogAdapter checklistAdapter = new ChecklistDialogAdapter(currentChecklist, isEditMode[0], userCanEdit, autoSaveChecklist);


        rvChecklistItems.setLayoutManager(new LinearLayoutManager(this));
        rvChecklistItems.setAdapter(checklistAdapter);

        btnAddChecklistItem.setOnClickListener(v -> {
            currentChecklist.add(new ChecklistItem("", false));
            checklistAdapter.notifyItemInserted(currentChecklist.size() - 1);
        });

        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSetReminder.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view1, year, month, day) -> {
                new android.app.TimePickerDialog(this, (view2, hour, minute) -> {
                    cal.set(year, month, day, hour, minute);
                    selectedDeadline[0] = cal.getTimeInMillis();
                    tvReminderValue.setText(new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(cal.getTime()));
                    btnDone.setVisibility(View.VISIBLE);
                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show();
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        btnDone.setOnClickListener(v -> {
            if (noteToEdit != null) {
                db.collection("users").document(ownerId).collection("boards").document(boardId)
                        .collection("notes").document(noteToEdit.getId())
                        .update("completed", true, "deadline", 0)
                        .addOnSuccessListener(aVoid -> cancelAlarm(noteToEdit.getId()));
                dialog.dismiss();
            }
        });

        Runnable updateTypeUI = () -> {
            int checkedId = rgNoteType.getCheckedRadioButtonId();
            for (int i = 0; i < rgNoteType.getChildCount(); i++) {
                View child = rgNoteType.getChildAt(i);
                if (child.getId() == checkedId) {
                    child.setAlpha(1.0f); child.setBackgroundResource(R.drawable.bg_type_selected); child.setElevation(8f);
                } else {
                    child.setAlpha(0.7f); child.setBackgroundResource(R.drawable.bg_role_border); child.setElevation(0f);
                }
            }
        };

        Runnable updateColorCheckmark = () -> {
            for (int i = 0; i < rgColors.getChildCount(); i++) {
                android.widget.RadioButton rb = (android.widget.RadioButton) rgColors.getChildAt(i);
                if (rb.getId() == rgColors.getCheckedRadioButtonId()) {
                    rb.setText("✓"); rb.setTextColor(Color.WHITE); rb.setGravity(android.view.Gravity.CENTER); rb.setTextSize(18);
                } else {
                    rb.setText("");
                }
            }
        };

        Runnable updateViewMode = () -> {
            int padding10dp = (int) (10 * getResources().getDisplayMetrics().density);
            // Zmienna dla zmniejszonych, bocznych marginesów wewnętrznych
            int paddingWider = (int) (4 * getResources().getDisplayMetrics().density);
            Window window = dialog.getWindow();

            if (isEditMode[0]) {
                outerCardView.setContentPadding(0, 0, 0, 0);
                if (window != null) {
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    window.setGravity(android.view.Gravity.BOTTOM);
                    window.getDecorView().setPadding(0, 0, 0, 0);
                }
                btnViewNote.setVisibility(View.VISIBLE);
                btnEditNote.setVisibility(View.GONE);
                dialogCardView.setCardBackgroundColor(Color.WHITE);
                String col = getSelectedColor(rgColors);
                etTitle.setBackgroundColor(Color.parseColor(col));
                etContent.setBackgroundColor(Color.parseColor(col));
                llEditControls.setVisibility(View.VISIBLE);
                btnAddChecklistItem.setVisibility(View.VISIBLE);
                btnPickImage.setVisibility(View.VISIBLE);
                etTitle.setFocusableInTouchMode(true);
                etTitle.setFocusable(true);
                etContent.setFocusableInTouchMode(true);
                etContent.setFocusable(true);

                // Tryb edycji: pokazujemy pole tekstowe tytułu, nagłówek staje się statyczny
                etTitle.setVisibility(View.VISIBLE);
                tvDialogTitle.setText(noteToEdit != null ? "Edit Note" : "New Note");

                if (noteToEdit != null) {
                    if (currentUserRole.equals("Admin")) btnDelete.setVisibility(View.VISIBLE);
                }
                checklistAdapter.setEditMode(true);
                updateColorCheckmark.run();
            } else {
                // POPRAWKA: Zastosowanie mniejszych marginesów bocznych (paddingWider)
                outerCardView.setContentPadding(paddingWider, padding10dp, paddingWider, padding10dp);
                if (window != null) {
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    window.setGravity(android.view.Gravity.CENTER);
                    // POPRAWKA: Usunięcie bocznych marginesów systemowych okna (0 zamiast 32)
                    window.getDecorView().setPadding(0, 32, 0, 32);
                }
                btnViewNote.setVisibility(View.GONE);
                btnEditNote.setVisibility(currentUserRole.equals("Viewer") ? View.GONE : View.VISIBLE);
                String noteColor = (noteToEdit != null) ? noteToEdit.getColor() : getSelectedColor(rgColors);
                dialogCardView.setCardBackgroundColor(Color.parseColor(noteColor));
                etTitle.setBackgroundColor(Color.TRANSPARENT);
                etContent.setBackgroundColor(Color.TRANSPARENT);
                llEditControls.setVisibility(View.GONE);
                btnAddChecklistItem.setVisibility(View.GONE);
                btnPickImage.setVisibility(View.GONE);
                etTitle.setFocusable(false);
                etContent.setFocusable(false);
                checklistAdapter.setEditMode(false);

                // Tryb podglądu: ukrywamy pole edycji tytułu, tytuł "wskakuje" w miejsce nagłówka
                etTitle.setVisibility(View.GONE);
                String currentTitle = etTitle.getText().toString().trim();
                tvDialogTitle.setText(currentTitle.isEmpty() ? "Untitled" : currentTitle);
            }
        };

        btnEditNote.setOnClickListener(v -> { isEditMode[0] = true; updateViewMode.run(); });
        btnViewNote.setOnClickListener(v -> { isEditMode[0] = false; updateViewMode.run(); });

        rgColors.setOnCheckedChangeListener((group, checkedId) -> {
            if (isEditMode[0]) {
                String color = getSelectedColor(rgColors);
                etTitle.setBackgroundColor(Color.parseColor(color));
                etContent.setBackgroundColor(Color.parseColor(color));
                updateColorCheckmark.run();
            }
        });

        rgNoteType.setOnCheckedChangeListener((group, checkedId) -> {
            updateTypeUI.run();
            etContent.setVisibility(View.GONE);
            layoutChecklist.setVisibility(View.GONE);
            layoutImage.setVisibility(View.GONE);

            if (checkedId == R.id.rbTypeChecklist) {
                layoutChecklist.setVisibility(View.VISIBLE);
                if (currentChecklist.isEmpty() && isEditMode[0]) btnAddChecklistItem.performClick();
            } else if (checkedId == R.id.rbTypeImage) {
                layoutImage.setVisibility(View.VISIBLE);
            } else {
                etContent.setVisibility(View.VISIBLE);
            }
        });

        View.OnClickListener sizeToggleListener = v -> {
            boolean isSquare = v.getId() == R.id.btnSizeSquare;
            btnSquare.setChecked(isSquare); btnRectangle.setChecked(!isSquare);
            btnSquare.setBackgroundColor(isSquare ? Color.WHITE : Color.TRANSPARENT);
            btnRectangle.setBackgroundColor(!isSquare ? Color.WHITE : Color.TRANSPARENT);
        };
        btnSquare.setOnClickListener(sizeToggleListener);
        btnRectangle.setOnClickListener(sizeToggleListener);

        if (noteToEdit != null) {
            tvDialogTitle.setText("View Note");
            etTitle.setText(noteToEdit.getTitle());
            etContent.setText(noteToEdit.getContent());
            cbPinned.setChecked(noteToEdit.isPinned());

            if (noteToEdit.getDeadline() > 0) {
                btnDone.setVisibility(View.VISIBLE);
                tvReminderValue.setText(new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(noteToEdit.getDeadline())));
            } else {
                btnDone.setVisibility(View.GONE);
                tvReminderValue.setText("No reminder");
            }

            if (noteToEdit.isFullWidth()) btnRectangle.performClick(); else btnSquare.performClick();
            setPaletteSelection(rgColors, noteToEdit.getColor());

            if ("checklist".equals(noteToEdit.getType())) {
                rgNoteType.check(R.id.rbTypeChecklist);
            } else if ("image".equals(noteToEdit.getType())) {
                rgNoteType.check(R.id.rbTypeImage);
                if (noteToEdit.getImageUrl() != null && !noteToEdit.getImageUrl().isEmpty()) {
                    currentImageBase64 = noteToEdit.getImageUrl();
                    try {
                        byte[] dec = Base64.decode(currentImageBase64, Base64.DEFAULT);
                        ivNoteImage.setImageBitmap(BitmapFactory.decodeByteArray(dec, 0, dec.length));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            } else {
                rgNoteType.check(R.id.rbTypePlain);
            }

            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(BoardViewActivity.this)
                    .setTitle("Delete note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete", (conf, w) -> {
                        db.collection("users").document(ownerId).collection("boards").document(boardId).collection("notes").document(noteToEdit.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(ownerId).collection("boards").document(boardId).update("noteCount", FieldValue.increment(-1));
                                    cancelAlarm(noteToEdit.getId());
                                    Toast.makeText(BoardViewActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                });
                        dialog.dismiss();
                    }).setNegativeButton("Cancel", null).show());
        } else {
            rgNoteType.check(R.id.rbTypePlain);
        }

        updateTypeUI.run();
        updateViewMode.run();

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            String type = "plain";

            if (rgNoteType.getCheckedRadioButtonId() == R.id.rbTypeChecklist) type = "checklist";
            else if (rgNoteType.getCheckedRadioButtonId() == R.id.rbTypeImage) type = "image";

            // Zapis przejdzie tylko i wyłącznie, gdy tytuł nie jest pusty
            if (!title.isEmpty()) {
                String col = getSelectedColor(rgColors);

                if (noteToEdit == null) {
                    Note n = new Note("", title, content, col, btnRectangle.isChecked(), cbPinned.isChecked(), (double) System.currentTimeMillis(), type);
                    if (type.equals("checklist")) n.setChecklist(currentChecklist);
                    if (type.equals("image")) n.setImageUrl(currentImageBase64);
                    n.setDeadline(selectedDeadline[0]);

                    db.collection("users").document(ownerId).collection("boards").document(boardId).collection("notes").add(n)
                            .addOnSuccessListener(dr -> {
                                lastAddedNoteId = dr.getId();
                                db.collection("users").document(ownerId).collection("boards").document(boardId).update("noteCount", FieldValue.increment(1));
                                if (selectedDeadline[0] > 0) {
                                    scheduleAlarm(dr.getId(), title, selectedDeadline[0]);
                                }
                            });
                } else {
                    Note updatedNote = new Note(
                            noteToEdit.getId(),
                            title,
                            content,
                            col,
                            btnRectangle.isChecked(),
                            cbPinned.isChecked(),
                            noteToEdit.getTimestamp(),
                            type
                    );
                    if (type.equals("checklist")) updatedNote.setChecklist(currentChecklist);
                    if (type.equals("image")) updatedNote.setImageUrl(currentImageBase64);
                    updatedNote.setDeadline(selectedDeadline[0]);
                    updatedNote.setCompleted(noteToEdit.isCompleted());

                    db.collection("users").document(ownerId).collection("boards").document(boardId).collection("notes").document(noteToEdit.getId()).set(updatedNote)
                            .addOnSuccessListener(aVoid -> {
                                if (selectedDeadline[0] > 0) {
                                    scheduleAlarm(updatedNote.getId(), title, selectedDeadline[0]);
                                } else {
                                    cancelAlarm(updatedNote.getId());
                                }
                            });
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Title is required!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.getDecorView().setPadding(0, 32, 0, 32);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.setGravity(android.view.Gravity.CENTER); // Centrujemy na ekranie
            window.getAttributes().windowAnimations = R.style.SlowDialogAnimation;
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            // Wysokość dopasowana do treści
            updateViewMode.run();
        }

    }

    private void showShareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_share_board, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        RecyclerView rvMembers = view.findViewById(R.id.rvMembers);
        EditText etSearch = view.findViewById(R.id.etSearchUser);
        Button btnInvite = view.findViewById(R.id.btnInviteMember);
        ImageView btnClose = view.findViewById(R.id.btnShareClose);
        ImageView btnBack = view.findViewById(R.id.btnShareBack);

        if (!currentUserId.equals(ownerId)) {
            etSearch.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);
        }

        Button btnChangeBoardBg = view.findViewById(R.id.btnChangeBoardBg);

        // Hide the button if the user is just a Viewer
        if (currentUserRole.equals("Viewer")) {
            btnChangeBoardBg.setVisibility(View.GONE);
        }

        btnChangeBoardBg.setOnClickListener(v -> {
            dialog.dismiss();
            showBoardBackgroundMenu();
        });



        List<Invitation> members = new ArrayList<>();
        MemberAdapter memberAdapter = new MemberAdapter(members);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(memberAdapter);

        db.collection("users").document(ownerId).get().addOnSuccessListener(ownerDoc -> {
            User owner = ownerDoc.toObject(User.class);
            if (owner == null) return;

            db.collection("invitations")
                    .whereEqualTo("boardId", boardId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        members.clear();

                        Invitation ownerInv = new Invitation(ownerId, boardId, tvBoardName.getText().toString(), ownerId, owner.getEmail(), owner.getEmail(), "accepted", "Owner", 0);
                        members.add(ownerInv);

                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Invitation inv = doc.toObject(Invitation.class);
                                inv.setId(doc.getId());
                                if (!"rejected".equals(inv.getStatus())) {
                                    members.add(inv);
                                }
                            }
                        }
                        memberAdapter.notifyDataSetChanged();
                    });
        });


        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        btnInvite.setOnClickListener(v -> {
            String targetEmail = etSearch.getText().toString().trim().toLowerCase();
            if (targetEmail.isEmpty() || targetEmail.equals(mAuth.getCurrentUser().getEmail().toLowerCase())) return;

            // 1. Sprawdzamy czy zaproszenie już istnieje
            db.collection("invitations")
                    .whereEqualTo("boardId", boardId)
                    .whereEqualTo("toUserEmail", targetEmail)
                    .get()
                    .addOnSuccessListener(invites -> {
                        if (!invites.isEmpty()) {
                            Toast.makeText(this, "Ten użytkownik został już zaproszony!", Toast.LENGTH_SHORT).show();
                        } else {
                            // 2. Jeśli nie ma, sprawdzamy konto i dopiero wysyłamy
                            db.collection("users").whereEqualTo("email", targetEmail).get().addOnSuccessListener(q -> {
                                if (!q.isEmpty()) {
                                    db.collection("invitations").add(new Invitation("", boardId, tvBoardName.getText().toString(), ownerId, mAuth.getCurrentUser().getEmail(), targetEmail, "pending", "Viewer", System.currentTimeMillis()))
                                            .addOnSuccessListener(dr -> {
                                                Toast.makeText(this, "Zaproszenie wysłane!", Toast.LENGTH_SHORT).show();
                                                etSearch.setText("");
                                            });
                                } else {
                                    Toast.makeText(this, "Nie znaleziono użytkownika.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private void setPaletteSelection(RadioGroup rg, String color) {
        if (color.equals("#FFF9C4")) rg.check(R.id.color1);
        else if (color.equals("#C8E6C9")) rg.check(R.id.color2);
        else if (color.equals("#FFCCBC")) rg.check(R.id.color3);
        else if (color.equals("#B3E5FC")) rg.check(R.id.color4);
        else if (color.equals("#D1C4E9")) rg.check(R.id.color5);
        else if (color.equals("#F8BBD0")) rg.check(R.id.color6);
        else if (color.equals("#B2EBF2")) rg.check(R.id.color7);
        else if (color.equals("#DCEDC8")) rg.check(R.id.color8);
        else if (color.equals("#FFE0B2")) rg.check(R.id.color9);
        else if (color.equals("#CFD8DC")) rg.check(R.id.color10);
    }

    private String getSelectedColor(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.color1) return "#FFF9C4";
        if (id == R.id.color2) return "#C8E6C9";
        if (id == R.id.color3) return "#FFCCBC";
        if (id == R.id.color4) return "#B3E5FC";
        if (id == R.id.color5) return "#D1C4E9";
        if (id == R.id.color6) return "#F8BBD0";
        if (id == R.id.color7) return "#B2EBF2";
        if (id == R.id.color8) return "#DCEDC8";
        if (id == R.id.color9) return "#FFE0B2";
        if (id == R.id.color10) return "#CFD8DC";
        return "#FFF9C4";
    }

    private void showFullscreenImageDialog(Bitmap bitmap) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        com.github.chrisbanes.photoview.PhotoView ivFullscreen = dialog.findViewById(R.id.ivFullscreen);
        ImageView btnCloseFullscreen = dialog.findViewById(R.id.btnCloseFullscreen);
        ivFullscreen.setImageBitmap(bitmap);
        btnCloseFullscreen.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private class MixedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int VIEW_TYPE_NOTE = 1;
        static final int VIEW_TYPE_DIVIDER = 2;
        private List<Object> items;

        public MixedAdapter(List<Object> items) { this.items = items; }

        @Override
        public int getItemViewType(int position) {
            return (items.get(position) instanceof String) ? VIEW_TYPE_DIVIDER : VIEW_TYPE_NOTE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_DIVIDER) {
                return new DividerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_divider, parent, false));
            } else {
                return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof NoteViewHolder) {
                Note note = (Note) items.get(position);
                NoteViewHolder h = (NoteViewHolder) holder;
                h.tvTitle.setText(note.getTitle());

                h.tvContent.setVisibility(View.GONE);
                h.ivNotePicture.setVisibility(View.GONE);

                if ("checklist".equals(note.getType()) && note.getChecklist() != null) {
                    h.tvContent.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();
                    for (ChecklistItem item : note.getChecklist()) {
                        sb.append(item.isChecked() ? "☑ " : "☐ ").append(item.getText()).append("\n");
                    }
                    h.tvContent.setText(sb.toString().trim());
                } else if ("image".equals(note.getType()) && note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                    h.ivNotePicture.setVisibility(View.VISIBLE);
                    try {
                        byte[] decodedString = Base64.decode(note.getImageUrl(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        h.ivNotePicture.setImageBitmap(decodedByte);
                        h.ivNotePicture.setOnClickListener(v -> showFullscreenImageDialog(decodedByte));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    h.tvContent.setVisibility(View.VISIBLE);
                    h.tvContent.setText(note.getContent());
                }

                h.cardNote.setCardBackgroundColor(Color.parseColor(note.getColor()));
                h.ivPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

                if (note.isCompleted()) {
                    h.ivCompleted.setVisibility(View.VISIBLE);
                    h.ivReminder.setVisibility(View.GONE);
                } else if (note.getDeadline() > 0) {
                    h.ivCompleted.setVisibility(View.GONE);
                    h.ivReminder.setVisibility(View.VISIBLE);
                } else {
                    h.ivCompleted.setVisibility(View.GONE);
                    h.ivReminder.setVisibility(View.GONE);
                }

                h.itemView.setOnClickListener(v -> showNoteDialog(note));

                StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) h.itemView.getLayoutParams();
                lp.setFullSpan(note.isFullWidth());
                h.itemView.setLayoutParams(lp);

                // PO – używa translationY + alpha, nie dotyka wymiaru layoutu
                if (note.getId().equals(lastAddedNoteId)) {
                    h.itemView.setAlpha(0.0f);
                    h.itemView.setTranslationY(40f);
                    // scaleX/Y zawsze pełne – SGLM mierzy poprawną szerokość
                    h.itemView.setScaleX(1.0f);
                    h.itemView.setScaleY(1.0f);
                    h.itemView.animate()
                            .alpha(1.0f)
                            .translationY(0f)
                            .setDuration(220)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> lastAddedNoteId = "")
                            .start();
                } else {
                    h.itemView.animate().cancel();
                    h.itemView.setAlpha(1.0f);
                    h.itemView.setTranslationY(0f);
                    h.itemView.setScaleX(1.0f);
                    h.itemView.setScaleY(1.0f);
                }
            } else if (holder instanceof DividerViewHolder) {
                StaggeredGridLayoutManager.LayoutParams lp = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setFullSpan(true);
                holder.itemView.setLayoutParams(lp);
            }
        }



        @Override
        public int getItemCount() { return items.size(); }

        class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent;
            CardView cardNote;
            ImageView ivPin, ivNotePicture, ivReminder, ivCompleted;

            public NoteViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNoteTitle);
                tvContent = v.findViewById(R.id.tvNoteContent);
                cardNote = v.findViewById(R.id.cardNote);
                ivPin = v.findViewById(R.id.ivPin);
                ivNotePicture = v.findViewById(R.id.ivNotePicture);
                ivReminder = v.findViewById(R.id.ivReminder);
                ivCompleted = v.findViewById(R.id.ivCompleted);
            }
        }

        class DividerViewHolder extends RecyclerView.ViewHolder {
            public DividerViewHolder(@NonNull View v) { super(v); }
        }
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
        private List<Invitation> members;

        public MemberAdapter(List<Invitation> members) { this.members = members; }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MemberViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            Invitation inv = members.get(position);
            holder.tvEmail.setText(inv.getToUserEmail());
            String role = inv.getRole() != null ? inv.getRole() : "Viewer";
            String status = inv.getStatus();

            if ("pending".equals(status)) {
                holder.tvRole.setText("Pending");
                holder.tvRole.setTextColor(Color.parseColor("#B0BEC5"));
            } else {
                holder.tvRole.setText(role);
                updateRoleColor(holder.tvRole, role);
            }

            // Bezpiecznik: weryfikacja czy zaproszenie w ogóle posiada maila przed wysłaniem zapytania
            if (inv.getToUserEmail() != null && !inv.getToUserEmail().isEmpty()) {
                db.collection("users").whereEqualTo("email", inv.getToUserEmail()).get().addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        User user = snapshots.getDocuments().get(0).toObject(User.class);
                        holder.tvName.setText(user.getUsername() != null ? user.getUsername() : "Unknown");
                        holder.tvAvatar.setText(user.getInitials() != null ? user.getInitials() : "?");
                        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);

                        // Zabezpieczenie koloru profilu w liście
                        try {
                            shape.setColor(Color.parseColor(user.getAvatarColor()));
                        } catch (Exception e) {
                            shape.setColor(Color.parseColor("#9E9E9E")); // Domyślny szary dla błędu
                        }

                        holder.tvAvatar.setBackground(shape);
                    }
                });
            }

            if (currentUserId.equals(ownerId) && !role.equals("Owner")) {
                holder.tvRole.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(BoardViewActivity.this, holder.tvRole);
                    if ("pending".equals(status)) {
                        popup.getMenu().add("Cancel invitation");
                    } else {
                        popup.getMenu().add("Admin");
                        popup.getMenu().add("Editor");
                        popup.getMenu().add("Viewer");
                        popup.getMenu().add("Remove from board");
                    }

                    popup.setOnMenuItemClickListener(item -> {
                        String selected = item.getTitle().toString();
                        if (selected.equals("Remove from board") || selected.equals("Cancel invitation")) {
                            db.collection("invitations").document(inv.getId()).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(BoardViewActivity.this, "Access removed", Toast.LENGTH_SHORT).show());
                        } else {
                            db.collection("invitations").document(inv.getId()).update("role", selected);
                        }
                        return true;
                    });
                    popup.show();
                });
            } else {
                holder.tvRole.setOnClickListener(null);
            }
        }

        private void updateRoleColor(TextView tvRole, String role) {
            if (role.equals("Owner")) tvRole.setTextColor(Color.parseColor("#FFD700"));
            else if (role.equals("Admin")) tvRole.setTextColor(Color.parseColor("#FF5252"));
            else if (role.equals("Editor")) tvRole.setTextColor(Color.parseColor("#448AFF"));
            else tvRole.setTextColor(Color.parseColor("#9E9E9E"));
        }

        @Override
        public int getItemCount() { return members.size(); }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvEmail, tvRole;
            public MemberViewHolder(@NonNull View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvAvatar);
                tvName = v.findViewById(R.id.tvMemberName);
                tvEmail = v.findViewById(R.id.tvMemberEmail);
                tvRole = v.findViewById(R.id.tvMemberRole);
            }
        }
    }

    private class ChecklistDialogAdapter extends RecyclerView.Adapter<ChecklistDialogAdapter.ChecklistViewHolder> {
        private List<ChecklistItem> items;
        private boolean isEditMode;
        private boolean canEdit;
        private Runnable onCheckToggled;

        public ChecklistDialogAdapter(List<ChecklistItem> items, boolean isEditMode, boolean canEdit, Runnable onCheckToggled) {
            this.items = items;
            this.isEditMode = isEditMode;
            this.canEdit = canEdit;
            this.onCheckToggled = onCheckToggled;
        }

        public void setEditMode(boolean isEditMode) {
            this.isEditMode = isEditMode;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            layout.setPadding(0, 8, 0, 8);

            CheckBox cb = new CheckBox(parent.getContext());

            EditText et = new EditText(parent.getContext());
            LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            et.setLayoutParams(etParams);
            et.setBackgroundColor(Color.TRANSPARENT);
            et.setHint("Typing...");
            et.setTextSize(18);
            et.setTextColor(Color.parseColor("#333333"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                et.setTextCursorDrawable(null); // Tu też naprawiamy kursor!
            }
            try {
                et.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(parent.getContext(), R.font.cause));
            } catch (Exception ignored) {}

            ImageView ivDel = new ImageView(parent.getContext());
            ivDel.setImageResource(android.R.drawable.ic_menu_delete);
            ivDel.setPadding(16, 16, 16, 16);
            ivDel.setColorFilter(Color.parseColor("#9E9E9E"));

            layout.addView(cb);
            layout.addView(et);
            layout.addView(ivDel);

            return new ChecklistViewHolder(layout, cb, et, ivDel);
        }

        @Override
        public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
            ChecklistItem item = items.get(position);

            holder.cb.setOnCheckedChangeListener(null);
            if (holder.textWatcher != null) {
                holder.et.removeTextChangedListener(holder.textWatcher);
            }

            holder.cb.setChecked(item.isChecked());
            holder.et.setText(item.getText());

            holder.et.setFocusable(isEditMode);
            holder.et.setFocusableInTouchMode(isEditMode);
            holder.ivDel.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            // Blokada klikania w checkbox, jeśli jesteś tylko Viewerem w trybie podglądu
            holder.cb.setEnabled(isEditMode || canEdit);

            holder.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setChecked(isChecked);
                // Jeśli użytkownik odhacza zadanie w locie, robimy szybki update do bazy
                if (!isEditMode && onCheckToggled != null) {
                    onCheckToggled.run();
                }
            });

            holder.textWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setText(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            holder.et.addTextChangedListener(holder.textWatcher);

            holder.ivDel.setOnClickListener(v -> {
                items.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ChecklistViewHolder extends RecyclerView.ViewHolder {
            CheckBox cb; EditText et; ImageView ivDel; TextWatcher textWatcher;
            public ChecklistViewHolder(@NonNull View itemView, CheckBox cb, EditText et, ImageView ivDel) {
                super(itemView);
                this.cb = cb; this.et = et; this.ivDel = ivDel;
            }
        }
    }
    private void showBoardBackgroundMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_board_bg, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        View.OnClickListener bgClickListener = v -> {
            String selectedKey = "default";
            int id = v.getId();

            if (id == R.id.bgMidnight) selectedKey = "midnight";
            else if (id == R.id.bgBlueprint) selectedKey = "blueprint";
            else if (id == R.id.bgPastel1) selectedKey = "pastel1";
            else if (id == R.id.bgPastel2) selectedKey = "pastel2";
            else if (id == R.id.bgPastel3) selectedKey = "pastel3";
            else if (id == R.id.bgPastel4) selectedKey = "pastel4";
            else if (id == R.id.bgPastel5) selectedKey = "pastel5";
            else if (id == R.id.bgPastel6) selectedKey = "pastel6";

            java.util.Map<String, Object> updateData = new java.util.HashMap<>();
            updateData.put("boardBackground", selectedKey);

            db.collection("users").document(ownerId).collection("boards").document(boardId)
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(BoardViewActivity.this, "Tło zmienione!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(BoardViewActivity.this, "Błąd zmiany tła: " + e.getMessage(), Toast.LENGTH_LONG).show());

            dialog.dismiss();
        };

        view.findViewById(R.id.bgMidnight).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgBlueprint).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel1).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel2).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel3).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel4).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel5).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgPastel6).setOnClickListener(bgClickListener);

        view.findViewById(R.id.btnCloseBgMenu).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

}