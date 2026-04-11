package com.example.noterello;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BoardSelectionActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private RecyclerView rvDrawerBoards;
    private FloatingActionButton fabAddBoard;

    private BoardAdapter adapter;
    private DrawerBoardAdapter drawerAdapter;

    // Podzieliliśmy listę na dwie, żeby łatwiej zarządzać własnymi i udostępnionymi tablicami
    private List<Board> ownBoards;
    private List<Board> sharedBoards;
    private List<Board> boardList;
    private View rootLayoutBoard;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_selection);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        drawerLayout = findViewById(R.id.drawerLayout);
        recyclerView = findViewById(R.id.recyclerViewBoards);
        rvDrawerBoards = findViewById(R.id.rvDrawerBoards);
        fabAddBoard = findViewById(R.id.fabAddBoard);
        TextView tvUsername = findViewById(R.id.tvUsername);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        ImageView btnLogout = findViewById(R.id.btnLogout);

        View parentView = (View) recyclerView.getParent();
        rootLayoutBoard = parentView != null ? parentView : findViewById(android.R.id.content);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        db.collection("users").document(currentUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Błąd", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null && !username.isEmpty()) {
                            tvUsername.setText("Witaj, " + username);
                        } else {
                            tvUsername.setText("Witaj!");
                        }

                        String bg = documentSnapshot.getString("mainBackground");
                        applyBackground(bg);
                    }
                });

        ownBoards = new ArrayList<>();
        sharedBoards = new ArrayList<>();
        boardList = new ArrayList<>();

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BoardAdapter(boardList);
        recyclerView.setAdapter(adapter);

        rvDrawerBoards.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new DrawerBoardAdapter(boardList);
        rvDrawerBoards.setAdapter(drawerAdapter);

        fabAddBoard.setOnClickListener(v -> showBoardDialog(null));

        loadBoardsFromFirebase();
        listenForInvitations();



        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(BoardSelectionActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(this, "Wylogowano", Toast.LENGTH_SHORT).show();
        });

        View btnChangeGlobalBg = findViewById(R.id.btnChangeGlobalBg);
        if (btnChangeGlobalBg != null) {
            btnChangeGlobalBg.setOnClickListener(v -> showBackgroundDialog());
        }
    }

    private void listenForInvitations() {
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        if (currentUserEmail == null) return;

        // Nasłuchujemy ZAPROSZEŃ OCZEKUJĄCYCH (pop-up)
        db.collection("invitations")
                .whereEqualTo("toUserEmail", currentUserEmail.toLowerCase())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Invitation inv = doc.toObject(Invitation.class);
                            inv.setId(doc.getId());
                            showInvitationDialog(inv);
                        }
                    }
                });
    }

    private void showInvitationDialog(Invitation inv) {
        new AlertDialog.Builder(this)
                .setTitle("Nowe zaproszenie!")
                .setMessage("Użytkownik " + inv.getFromUserEmail() + " zaprasza Cię do tablicy:\n\n'" + inv.getBoardName() + "'")
                .setPositiveButton("Akceptuj", (dialog, which) -> {
                    db.collection("invitations").document(inv.getId())
                            .update("status", "accepted")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Zaakceptowano tablicę!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Odrzuć", (dialog, which) -> {
                    db.collection("invitations").document(inv.getId())
                            .update("status", "rejected");
                })
                .setCancelable(false)
                .show();
    }

    private void applyBackground(String bgType) {
        if (bgType == null) bgType = "cork";
        if (rootLayoutBoard != null) {
            switch (bgType) {
                case "chalk":
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#2E4C3B"));
                    break;
                case "white":
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#41E0FF"));
                    break;
                default:
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#A67C52"));
                    break;
            }
        }
    }

    private void showBackgroundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_background, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        view.findViewById(R.id.bgOptCork).setOnClickListener(v -> {
            db.collection("users").document(currentUserId).update("mainBackground", "cork");
            drawerLayout.closeDrawer(GravityCompat.START);
            dialog.dismiss();
        });

        view.findViewById(R.id.bgOptChalk).setOnClickListener(v -> {
            db.collection("users").document(currentUserId).update("mainBackground", "chalk");
            drawerLayout.closeDrawer(GravityCompat.START);
            dialog.dismiss();
        });

        view.findViewById(R.id.bgOptWhite).setOnClickListener(v -> {
            db.collection("users").document(currentUserId).update("mainBackground", "white");
            drawerLayout.closeDrawer(GravityCompat.START);
            dialog.dismiss();
        });

        View btnCancelBg = view.findViewById(R.id.btnCancelBg);
        if (btnCancelBg != null) {
            btnCancelBg.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private void loadBoardsFromFirebase() {
        // 1. ŁADOWANIE WŁASNYCH TABLIC
        db.collection("users").document(currentUserId).collection("boards")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        ownBoards.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Board board = doc.toObject(Board.class);
                            String ownerId = board.getOwnerId() != null ? board.getOwnerId() : currentUserId;
                            Board boardWithId = new Board(doc.getId(), board.getName(), board.getColor(), board.getNoteCount(), board.getTimestamp(), ownerId);
                            ownBoards.add(boardWithId);
                        }
                        updateCombinedBoardList();
                    }
                });

        // 2. ŁADOWANIE UDOSTĘPNIONYCH TABLIC (Zaakceptowane zaproszenia)
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        if (currentUserEmail != null) {
            db.collection("invitations")
                    .whereEqualTo("toUserEmail", currentUserEmail.toLowerCase())
                    .whereEqualTo("status", "accepted")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        if (value != null) {
                            sharedBoards.clear();

                            if (value.isEmpty()) {
                                updateCombinedBoardList();
                                return;
                            }

                            int expectedCount = value.size();
                            int[] processedCount = {0};

                            for (QueryDocumentSnapshot doc : value) {
                                Invitation inv = doc.toObject(Invitation.class);

                                // Pobieramy dane tablicy bezpośrednio od jej właściciela
                                db.collection("users").document(inv.getFromUserId())
                                        .collection("boards").document(inv.getBoardId())
                                        .get()
                                        .addOnSuccessListener(boardDoc -> {
                                            if (boardDoc.exists()) {
                                                Board board = boardDoc.toObject(Board.class);
                                                Board sharedBoard = new Board(boardDoc.getId(), board.getName(), board.getColor(), board.getNoteCount(), board.getTimestamp(), inv.getFromUserId());
                                                sharedBoards.add(sharedBoard);
                                            }
                                            processedCount[0]++;
                                            if (processedCount[0] == expectedCount) updateCombinedBoardList();
                                        })
                                        .addOnFailureListener(e -> {
                                            processedCount[0]++;
                                            if (processedCount[0] == expectedCount) updateCombinedBoardList();
                                        });
                            }
                        }
                    });
        }
    }

    // Łączy obie listy i odświeża interfejs
    private void updateCombinedBoardList() {
        boardList.clear();
        boardList.addAll(ownBoards);
        boardList.addAll(sharedBoards);

        adapter.notifyDataSetChanged();
        drawerAdapter.notifyDataSetChanged();

        TextView tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageView ivEmptyStateArrow = findViewById(R.id.ivEmptyStateArrow);

        if (boardList.isEmpty()) {
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
            if (ivEmptyStateArrow != null) ivEmptyStateArrow.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            if (ivEmptyStateArrow != null) ivEmptyStateArrow.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showBoardDialog(final Board boardToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_board, null);
        builder.setView(view);

        final TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        final EditText etTitle = view.findViewById(R.id.etBoardTitle);
        final RadioGroup rgColors = view.findViewById(R.id.rgBoardColors);
        final Button btnCreate = view.findViewById(R.id.btnCreate);
        final Button btnCancel = view.findViewById(R.id.btnCancel);
        final ImageView btnClose = view.findViewById(R.id.btnClose);

        final AlertDialog dialog = builder.create();

        Runnable updateCheckmark = () -> {
            for (int i = 0; i < rgColors.getChildCount(); i++) {
                android.widget.RadioButton rb = (android.widget.RadioButton) rgColors.getChildAt(i);
                if (rb.getId() == rgColors.getCheckedRadioButtonId()) rb.setText("✓");
                else rb.setText("");
            }
        };

        rgColors.setOnCheckedChangeListener((group, checkedId) -> updateCheckmark.run());

        if (boardToEdit != null) {
            if (tvDialogTitle != null) tvDialogTitle.setText("Edit board");
            etTitle.setText(boardToEdit.getName());
            setPaletteSelection(rgColors, boardToEdit.getColor());
            btnCreate.setText("Save");
        } else {
            if (tvDialogTitle != null) tvDialogTitle.setText("Create new board");
            btnCreate.setText("Create");
        }

        btnCreate.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            if (!title.isEmpty()) {
                String color = getSelectedColor(rgColors);

                if (boardToEdit == null) {
                    Board newBoard = new Board("", title, color, 0, System.currentTimeMillis(), currentUserId);
                    db.collection("users").document(currentUserId).collection("boards")
                            .add(newBoard)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Tablica zapisana!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show());
                } else {
                    db.collection("users").document(currentUserId).collection("boards").document(boardToEdit.getId())
                            .update("name", title, "color", color)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Zaktualizowano tablicę!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                }
            } else {
                Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        updateCheckmark.run();
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private void setPaletteSelection(RadioGroup rg, String color) {
        if (color.equals("#448AFF")) rg.check(R.id.bColor1);
        else if (color.equals("#00C853")) rg.check(R.id.bColor2);
        else if (color.equals("#FFD600")) rg.check(R.id.bColor3);
        else if (color.equals("#FF6D00")) rg.check(R.id.bColor4);
        else if (color.equals("#AA00FF")) rg.check(R.id.bColor5);
        else if (color.equals("#F44336")) rg.check(R.id.bColor6);
        else if (color.equals("#00BCD4")) rg.check(R.id.bColor7);
        else if (color.equals("#8BC34A")) rg.check(R.id.bColor8);
        else if (color.equals("#E91E63")) rg.check(R.id.bColor9);
        else if (color.equals("#795548")) rg.check(R.id.bColor10);
    }

    private String getSelectedColor(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.bColor1) return "#448AFF";
        if (id == R.id.bColor2) return "#00C853";
        if (id == R.id.bColor3) return "#FFD600";
        if (id == R.id.bColor4) return "#FF6D00";
        if (id == R.id.bColor5) return "#AA00FF";
        if (id == R.id.bColor6) return "#F44336";
        if (id == R.id.bColor7) return "#00BCD4";
        if (id == R.id.bColor8) return "#8BC34A";
        if (id == R.id.bColor9) return "#E91E63";
        if (id == R.id.bColor10) return "#795548";
        return "#448AFF";
    }

    private class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.BoardViewHolder> {
        private List<Board> boards;
        public BoardAdapter(List<Board> boards) { this.boards = boards; }

        @NonNull
        @Override
        public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_board, parent, false);
            return new BoardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BoardViewHolder holder, int position) {
            Board board = boards.get(position);
            holder.tvBoardTitle.setText(board.getName());

            // ZNACZNIK UDOSTĘPNIENIA: Dodajemy specjalny opis, jeśli nie my jesteśmy właścicielem
            if (!board.getOwnerId().equals(currentUserId)) {
                holder.tvNoteCount.setText("🤝 Udostępniona • " + board.getNoteCount() + " notes");
            } else {
                holder.tvNoteCount.setText(board.getNoteCount() + " notes");
            }

            try {
                holder.cardBoard.setCardBackgroundColor(Color.parseColor(board.getColor()));
            } catch (Exception e) {
                holder.cardBoard.setCardBackgroundColor(Color.parseColor("#448AFF"));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BoardSelectionActivity.this, BoardViewActivity.class);
                intent.putExtra("BOARD_ID", board.getId());
                intent.putExtra("BOARD_NAME", board.getName());

                // PRZEKAZUJEMY ID WŁAŚCICIELA, aby BoardViewActivity wiedziało, z czyjej bazy czytać notatki!
                intent.putExtra("OWNER_ID", board.getOwnerId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return boards.size(); }

        class BoardViewHolder extends RecyclerView.ViewHolder {
            TextView tvBoardTitle, tvNoteCount;
            CardView cardBoard;
            public BoardViewHolder(View itemView) {
                super(itemView);
                tvBoardTitle = itemView.findViewById(R.id.tvBoardTitle);
                tvNoteCount = itemView.findViewById(R.id.tvNoteCount);
                cardBoard = itemView.findViewById(R.id.cardBoard);
            }
        }
    }

    private class DrawerBoardAdapter extends RecyclerView.Adapter<DrawerBoardAdapter.DrawerViewHolder> {
        private List<Board> boards;
        public DrawerBoardAdapter(List<Board> boards) { this.boards = boards; }

        @NonNull
        @Override
        public DrawerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_board, parent, false);
            return new DrawerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawerViewHolder holder, int position) {
            Board board = boards.get(position);

            if (!board.getOwnerId().equals(currentUserId)) {
                holder.tvName.setText("🤝 " + board.getName());
                holder.btnEdit.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.VISIBLE); // Pokazujemy przycisk! Będzie służył do wyjścia
            } else {
                holder.tvName.setText(board.getName());
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            holder.btnEdit.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showBoardDialog(board);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (!board.getOwnerId().equals(currentUserId)) {
                    // LOGIKA: OPUSZCZANIE CUDZEJ TABLICY
                    new AlertDialog.Builder(BoardSelectionActivity.this)
                            .setTitle("Opuść tablicę")
                            .setMessage("Czy na pewno chcesz opuścić tablicę '" + board.getName() + "'?\nStracisz do niej dostęp.")
                            .setPositiveButton("Opuść", (dialog, which) -> {
                                String currentUserEmail = mAuth.getCurrentUser().getEmail();
                                if (currentUserEmail != null) {
                                    db.collection("invitations")
                                            .whereEqualTo("boardId", board.getId())
                                            .whereEqualTo("toUserEmail", currentUserEmail.toLowerCase())
                                            .get()
                                            .addOnSuccessListener(query -> {
                                                for(QueryDocumentSnapshot doc : query) {
                                                    db.collection("invitations").document(doc.getId()).delete();
                                                }
                                                Toast.makeText(BoardSelectionActivity.this, "Opuszczono tablicę", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .setNegativeButton("Anuluj", null)
                            .show();
                } else {
                    // LOGIKA: USUWANIE WŁASNEJ TABLICY
                    new AlertDialog.Builder(BoardSelectionActivity.this)
                            .setTitle("Usuń tablicę")
                            .setMessage("Czy na pewno chcesz usunąć tablicę '" + board.getName() + "'?\nTa akcja jest nieodwracalna.")
                            .setPositiveButton("Usuń", (dialog, which) -> {
                                db.collection("users").document(currentUserId)
                                        .collection("boards").document(board.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(BoardSelectionActivity.this, "Usunięto tablicę", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Anuluj", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() { return boards.size(); }

        class DrawerViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView btnEdit, btnDelete;
            public DrawerViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvDrawerBoardName);
                btnEdit = itemView.findViewById(R.id.btnEditBoard);
                btnDelete = itemView.findViewById(R.id.btnDeleteBoard);
            }
        }
    }


    private String getRandomHexColor() {
        String[] colors = {"#FF5252", "#FF4081", "#E040FB", "#7C4DFF", "#536DFE", "#448AFF", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#FF9800", "#FF5722", "#795548", "#607D8B"};
        return colors[new java.util.Random().nextInt(colors.length)];
    }
}