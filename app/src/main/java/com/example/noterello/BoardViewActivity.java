package com.example.noterello;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardViewActivity extends AppCompatActivity {

    private TextView tvBoardName;
    private RecyclerView rvNotes;
    private FloatingActionButton fabAddNote;
    private ImageView btnShareBoard;

    private MixedAdapter adapter;

    private List<Object> displayList;
    private List<Note> rawNotesList;

    private String currentSearchQuery = "";
    private boolean isDragging = false;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String boardId;

    // ID właściciela tablicy
    private String ownerId;

    // Rola zalogowanego użytkownika (domyślnie Viewer)
    private String currentUserRole = "Viewer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_view);

        tvBoardName = findViewById(R.id.tvBoardName);
        rvNotes = findViewById(R.id.rvNotes);
        fabAddNote = findViewById(R.id.fabAddNote);
        btnShareBoard = findViewById(R.id.btnShareBoard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Błąd autoryzacji", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boardId = getIntent().getStringExtra("BOARD_ID");
        String boardName = getIntent().getStringExtra("BOARD_NAME");
        ownerId = getIntent().getStringExtra("OWNER_ID");

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
        }

        fabAddNote.setOnClickListener(v -> showNoteDialog(null));
        btnShareBoard.setOnClickListener(v -> showShareDialog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.llAvatars).setOnClickListener(v -> showShareDialog());

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

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                sortNotesAndAddDivider();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadRoleAndNotes() {
        if (currentUserId.equals(ownerId)) {
            // Jesteśmy właścicielem -> mamy pełne uprawnienia
            currentUserRole = "Admin";
            fabAddNote.setVisibility(View.VISIBLE);
        } else {
            // Sprawdzamy rolę w bazie zaproszeń
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null) {
                db.collection("invitations")
                        .whereEqualTo("boardId", boardId)
                        .whereEqualTo("toUserEmail", email.toLowerCase())
                        .whereEqualTo("status", "accepted")
                        .addSnapshotListener((value, error) -> {
                            if (error != null) return;
                            if (value != null && !value.isEmpty()) {
                                Invitation inv = value.getDocuments().get(0).toObject(Invitation.class);
                                currentUserRole = inv.getRole() != null ? inv.getRole() : "Viewer";
                            } else {
                                currentUserRole = "Viewer";
                            }

                            // Przycisk dodawania notatki (+) widać tylko u Admina
                            fabAddNote.setVisibility(currentUserRole.equals("Admin") ? View.VISIBLE : View.GONE);

                            // Odświeżamy listę, aby zablokować/odblokować kliknięcia notatek na podstawie nowej roli
                            if (adapter != null) adapter.notifyDataSetChanged();
                        });
            }
        }

        // Ładujemy notatki niezależnie od roli
        loadNotesFromFirebase();
    }

    private void loadNotesFromFirebase() {
        db.collection("users").document(ownerId)
                .collection("boards").document(boardId)
                .collection("notes")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Błąd nasłuchiwania notatek", error);
                        return;
                    }

                    if (value != null) {
                        if (isDragging) {
                            Log.d("DragDrop", "Snapshot zignorowany – trwa drag");
                            return;
                        }

                        java.util.Map<String, Note> freshNotesMap = new java.util.LinkedHashMap<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Note note = doc.toObject(Note.class);
                            note.setId(doc.getId());
                            freshNotesMap.put(doc.getId(), note);
                        }

                        List<Note> merged = new ArrayList<>();

                        for (Note localNote : rawNotesList) {
                            Note fresh = freshNotesMap.remove(localNote.getId());
                            if (fresh != null) {
                                fresh.setTimestamp(localNote.getTimestamp());
                                merged.add(fresh);
                            }
                        }

                        for (Note newNote : freshNotesMap.values()) {
                            merged.add(newNote);
                        }

                        rawNotesList.clear();
                        rawNotesList.addAll(merged);
                        sortNotesAndAddDivider();
                    }
                });
    }

    private void setupDragAndDrop() {
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                        androidx.recyclerview.widget.ItemTouchHelper.UP |
                                androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                                androidx.recyclerview.widget.ItemTouchHelper.LEFT |
                                androidx.recyclerview.widget.ItemTouchHelper.RIGHT,
                        0) {

                    int draggedFrom = -1;
                    int draggedTo = -1;

                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        // Viewer nie może przesuwać notatek (blokada Drag & Drop)
                        if (currentUserRole.equals("Viewer")) {
                            return makeMovementFlags(0, 0);
                        }
                        int dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP |
                                androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                                androidx.recyclerview.widget.ItemTouchHelper.LEFT |
                                androidx.recyclerview.widget.ItemTouchHelper.RIGHT;
                        return makeMovementFlags(dragFlags, 0);
                    }

                    @Override
                    public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                               @NonNull RecyclerView.ViewHolder current,
                                               @NonNull RecyclerView.ViewHolder target) {
                        if (target.getItemViewType() == MixedAdapter.VIEW_TYPE_DIVIDER) return false;

                        int fromPos = current.getAdapterPosition();
                        int toPos = target.getAdapterPosition();
                        if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) return false;

                        Object fromObj = displayList.get(fromPos);
                        Object toObj = displayList.get(toPos);

                        if (fromObj instanceof Note && toObj instanceof Note) {
                            return ((Note) fromObj).isPinned() == ((Note) toObj).isPinned();
                        }
                        return false;
                    }

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int fromPosition = viewHolder.getAdapterPosition();
                        int toPosition = target.getAdapterPosition();

                        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                            return false;
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
                        if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
                                && viewHolder != null) {
                            isDragging = true;
                            viewHolder.itemView.animate()
                                    .scaleX(1.07f)
                                    .scaleY(1.07f)
                                    .translationZ(28f)
                                    .setDuration(150)
                                    .start();
                        }
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);

                        viewHolder.itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .translationZ(0f)
                                .setDuration(150)
                                .start();

                        if (draggedFrom != -1 && draggedTo != -1 && draggedFrom != draggedTo) {
                            updateNoteOrderInFirebase(draggedTo, () -> {
                                syncRawNotesListFromDisplay();
                            });
                        }

                        draggedFrom = -1;
                        draggedTo = -1;

                        viewHolder.itemView.postDelayed(() -> {
                            isDragging = false;
                        }, 300);
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
                });

        itemTouchHelper.attachToRecyclerView(rvNotes);
    }

    private void syncRawNotesListFromDisplay() {
        rawNotesList.clear();
        for (Object obj : displayList) {
            if (obj instanceof Note) {
                rawNotesList.add((Note) obj);
            }
        }
    }

    private void updateNoteOrderInFirebase(int finalPosition, Runnable onComplete) {
        Object currentObj = displayList.get(finalPosition);
        if (!(currentObj instanceof Note)) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Note draggedNote = (Note) currentObj;

        double prevTimestamp = 0;
        double nextTimestamp = 0;

        for (int i = finalPosition - 1; i >= 0; i--) {
            if (displayList.get(i) instanceof Note) {
                prevTimestamp = ((Note) displayList.get(i)).getTimestamp();
                break;
            }
        }

        for (int i = finalPosition + 1; i < displayList.size(); i++) {
            if (displayList.get(i) instanceof Note) {
                nextTimestamp = ((Note) displayList.get(i)).getTimestamp();
                break;
            }
        }

        double newTimestamp;

        if (prevTimestamp == 0 && nextTimestamp == 0) {
            if (onComplete != null) onComplete.run();
            return;
        } else if (prevTimestamp == 0) {
            newTimestamp = nextTimestamp - 100_000.0;
        } else if (nextTimestamp == 0) {
            newTimestamp = prevTimestamp + 100_000.0;
        } else {
            newTimestamp = (prevTimestamp + nextTimestamp) / 2.0;
        }

        draggedNote.setTimestamp(newTimestamp);

        db.collection("users").document(ownerId)
                .collection("boards").document(boardId)
                .collection("notes").document(draggedNote.getId())
                .update("timestamp", newTimestamp)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DragDrop", "Timestamp zaktualizowany: " + newTimestamp);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("DragDrop", "Błąd zapisu Firebase", e);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void sortNotesAndAddDivider() {
        List<Note> pinnedNotes = new ArrayList<>();
        List<Note> normalNotes = new ArrayList<>();

        for (Note n : rawNotesList) {
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
                String content = n.getContent() != null ? n.getContent().toLowerCase() : "";
                if (!title.contains(currentSearchQuery) && !content.contains(currentSearchQuery)) {
                    matchesSearch = false;
                }
            }

            if (matchesSearch) {
                if (n.isPinned()) pinnedNotes.add(n);
                else normalNotes.add(n);
            }
        }

        displayList.clear();
        displayList.addAll(pinnedNotes);
        if (!pinnedNotes.isEmpty() && !normalNotes.isEmpty()) {
            displayList.add("DIVIDER");
        }
        displayList.addAll(normalNotes);
        adapter.notifyDataSetChanged();
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        builder.setView(view);

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

        final AlertDialog dialog = builder.create();

        View.OnClickListener sizeToggleListener = v -> {
            boolean isSquare = v.getId() == R.id.btnSizeSquare;
            btnSquare.setChecked(isSquare);
            btnRectangle.setChecked(!isSquare);
            btnSquare.setBackgroundColor(isSquare ? Color.WHITE : Color.TRANSPARENT);
            btnRectangle.setBackgroundColor(!isSquare ? Color.WHITE : Color.TRANSPARENT);
        };
        btnSquare.setOnClickListener(sizeToggleListener);
        btnRectangle.setOnClickListener(sizeToggleListener);

        if (noteToEdit != null) {
            tvDialogTitle.setText("Edit Note");
            etTitle.setText(noteToEdit.getTitle());
            etContent.setText(noteToEdit.getContent());
            etTitle.setBackgroundColor(Color.parseColor(noteToEdit.getColor()));
            etContent.setBackgroundColor(Color.parseColor(noteToEdit.getColor()));
            cbPinned.setChecked(noteToEdit.isPinned());
            if (noteToEdit.isFullWidth()) btnRectangle.performClick(); else btnSquare.performClick();
            setPaletteSelection(rgColors, noteToEdit.getColor());

            // Kosz widoczny tylko dla Admina
            if (!currentUserRole.equals("Admin")) {
                btnDelete.setVisibility(View.GONE);
            } else {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    db.collection("users").document(ownerId)
                            .collection("boards").document(boardId)
                            .collection("notes").document(noteToEdit.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                db.collection("users").document(ownerId)
                                        .collection("boards").document(boardId)
                                        .update("noteCount", FieldValue.increment(-1));
                                Toast.makeText(this, "Usunięto notatkę", Toast.LENGTH_SHORT).show();
                            });
                    dialog.dismiss();
                });
            }
        } else {
            // Bezpieczeństwo - jeśli jakoś doszło do tworzenia nowej notatki bez uprawnień
            btnDelete.setVisibility(View.GONE);
        }

        rgColors.setOnCheckedChangeListener((group, checkedId) -> {
            String color = getSelectedColor(rgColors);
            int colorInt = Color.parseColor(color);
            etTitle.setBackgroundColor(colorInt);
            etContent.setBackgroundColor(colorInt);
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();

            if (!content.isEmpty()) {
                String color = getSelectedColor(rgColors);
                if (title.isEmpty()) {
                    title = content.split("\n")[0];
                    if (title.length() > 20) title = title.substring(0, 20) + "...";
                }

                if (noteToEdit == null) {
                    Note newNote = new Note("", title, content, color, btnRectangle.isChecked(), cbPinned.isChecked(), (double) System.currentTimeMillis());
                    db.collection("users").document(ownerId)
                            .collection("boards").document(boardId)
                            .collection("notes").add(newNote)
                            .addOnSuccessListener(documentReference -> {
                                db.collection("users").document(ownerId)
                                        .collection("boards").document(boardId)
                                        .update("noteCount", FieldValue.increment(1));
                            });
                } else {
                    noteToEdit.setTitle(title);
                    noteToEdit.setContent(content);
                    noteToEdit.setColor(color);
                    noteToEdit.setFullWidth(btnRectangle.isChecked());
                    noteToEdit.setPinned(cbPinned.isChecked());

                    db.collection("users").document(ownerId)
                            .collection("boards").document(boardId)
                            .collection("notes").document(noteToEdit.getId())
                            .set(noteToEdit);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Note cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private void showShareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_share_board, null);
        builder.setView(view);

        RecyclerView rvMembers = view.findViewById(R.id.rvMembers);
        EditText etSearch = view.findViewById(R.id.etSearchUser);
        Button btnInvite = view.findViewById(R.id.btnInviteMember);
        ImageView btnClose = view.findViewById(R.id.btnShareClose);
        ImageView btnBack = view.findViewById(R.id.btnShareBack);

        // Zapraszanie zarezerwowane dla Admina (Opcjonalne, ale w dobrym tonie)
        if (!currentUserId.equals(ownerId)) {
            etSearch.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);
        }

        List<Invitation> members = new ArrayList<>();
        MemberAdapter memberAdapter = new MemberAdapter(members);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(memberAdapter);

        db.collection("invitations")
                .whereEqualTo("boardId", boardId)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    members.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Invitation inv = doc.toObject(Invitation.class);
                            inv.setId(doc.getId());
                            members.add(inv);
                        }
                    }
                    memberAdapter.notifyDataSetChanged();
                });

        final AlertDialog dialog = builder.create();

        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        btnInvite.setOnClickListener(v -> {
            String targetEmail = etSearch.getText().toString().trim().toLowerCase();
            if (targetEmail.isEmpty()) {
                Toast.makeText(this, "Wpisz adres e-mail!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (targetEmail.equals(mAuth.getCurrentUser().getEmail().toLowerCase())) {
                Toast.makeText(this, "Nie możesz zaprosić samego siebie!", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").whereEqualTo("email", targetEmail).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "Nie znaleziono użytkownika z takim e-mailem.", Toast.LENGTH_SHORT).show();
                        } else {
                            String currentUserEmail = mAuth.getCurrentUser().getEmail();
                            String boardName = tvBoardName.getText().toString();

                            // Zapraszamy domyślnie jako Viewer
                            Invitation invite = new Invitation(
                                    "", boardId, boardName, ownerId, currentUserEmail, targetEmail, "pending", "Viewer", System.currentTimeMillis()
                            );

                            db.collection("invitations").add(invite)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Zaproszenie wysłane pomyślnie!", Toast.LENGTH_SHORT).show();
                                        etSearch.setText("");
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Błąd wysyłania zaproszenia", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Błąd wyszukiwania: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private class MixedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int VIEW_TYPE_NOTE = 1;
        static final int VIEW_TYPE_DIVIDER = 2;

        private List<Object> items;

        public MixedAdapter(List<Object> items) { this.items = items; }

        @Override
        public int getItemViewType(int position) {
            if (items.get(position) instanceof String) {
                return VIEW_TYPE_DIVIDER;
            }
            return VIEW_TYPE_NOTE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_DIVIDER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_divider, parent, false);
                return new DividerViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
                return new NoteViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_NOTE) {
                Note note = (Note) items.get(position);
                NoteViewHolder noteHolder = (NoteViewHolder) holder;

                noteHolder.tvTitle.setText(note.getTitle());
                noteHolder.tvContent.setText(note.getContent());

                try {
                    noteHolder.cardNote.setCardBackgroundColor(Color.parseColor(note.getColor()));
                } catch (Exception e) {
                    noteHolder.cardNote.setCardBackgroundColor(Color.parseColor("#FFF9C4"));
                }

                noteHolder.ivPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

                // Blokada otwierania edytora dla ról innych niż Admin / Editor
                if (currentUserRole.equals("Viewer")) {
                    noteHolder.itemView.setOnClickListener(v ->
                            Toast.makeText(BoardViewActivity.this, "Tylko do odczytu. Nie masz uprawnień do edycji.", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    noteHolder.itemView.setOnClickListener(v -> showNoteDialog(note));
                }

                StaggeredGridLayoutManager.LayoutParams lp =
                        (StaggeredGridLayoutManager.LayoutParams) noteHolder.itemView.getLayoutParams();
                lp.setFullSpan(note.isFullWidth());
                noteHolder.itemView.setLayoutParams(lp);

            } else if (holder.getItemViewType() == VIEW_TYPE_DIVIDER) {
                StaggeredGridLayoutManager.LayoutParams lp = new StaggeredGridLayoutManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setFullSpan(true);
                holder.itemView.setLayoutParams(lp);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent;
            CardView cardNote;
            ImageView ivPin;
            public NoteViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNoteTitle);
                tvContent = v.findViewById(R.id.tvNoteContent);
                cardNote = v.findViewById(R.id.cardNote);
                ivPin = v.findViewById(R.id.ivPin);
            }
        }

        class DividerViewHolder extends RecyclerView.ViewHolder {
            public DividerViewHolder(@NonNull View v) {
                super(v);
            }
        }
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
        private List<Invitation> members;
        public MemberAdapter(List<Invitation> members) { this.members = members; }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MemberViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            Invitation inv = members.get(position);

            // Wartości tymczasowe na czas ładowania z bazy
            holder.tvName.setText("Ładowanie...");
            holder.tvEmail.setText(inv.getToUserEmail());

            String currentRole = inv.getRole() != null ? inv.getRole() : "Viewer";
            holder.tvRole.setText(currentRole);
            updateRoleColor(holder.tvRole, currentRole);

            // Pobieramy dane użytkownika z bazy, aby wyciągnąć login i awatar
            db.collection("users").whereEqualTo("email", inv.getToUserEmail()).get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            User user = snapshots.getDocuments().get(0).toObject(User.class);

                            // Poprawne przypisanie tekstów!
                            holder.tvName.setText(user.getUsername()); // Wojtek na górze
                            holder.tvEmail.setText(user.getEmail());   // wojtek@email.com pod spodem

                            // Rysowanie awatara w nowym polu tvAvatar
                            holder.tvAvatar.setText(user.getInitials());
                            try {
                                int color = Color.parseColor(user.getAvatarColor());
                                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                                shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                                shape.setColor(color);
                                holder.tvAvatar.setBackground(shape);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

            if (currentUserId.equals(ownerId)) {
                holder.tvRole.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(BoardViewActivity.this, holder.tvRole);
                    popup.getMenu().add("Admin");
                    popup.getMenu().add("Editor");
                    popup.getMenu().add("Viewer");
                    popup.getMenu().add("Usuń z tablicy");

                    popup.setOnMenuItemClickListener(item -> {
                        String selected = item.getTitle().toString();
                        if (selected.equals("Usuń z tablicy")) {
                            db.collection("invitations").document(inv.getId()).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(BoardViewActivity.this, "Usunięto użytkownika", Toast.LENGTH_SHORT).show());
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
            if (role.equals("Admin")) tvRole.setTextColor(Color.parseColor("#FF5252"));
            else if (role.equals("Editor")) tvRole.setTextColor(Color.parseColor("#448AFF"));
            else tvRole.setTextColor(Color.parseColor("#9E9E9E"));
        }

        @Override
        public int getItemCount() { return members.size(); }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvEmail, tvRole;
            public MemberViewHolder(@NonNull View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvAvatar); // Nowe pole z inicjałem
                tvName = v.findViewById(R.id.tvMemberName);
                tvEmail = v.findViewById(R.id.tvMemberEmail);
                tvRole = v.findViewById(R.id.tvMemberRole);
            }
        }
    }
}