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

    private List<Board> ownBoards;
    private List<Board> sharedBoards;
    private List<Board> boardList;
    private View rootLayoutBoard;

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
            new AlertDialog.Builder(BoardSelectionActivity.this)
                    .setTitle("Wyloguj")
                    .setMessage("Czy na pewno chcesz się wylogować?")
                    .setPositiveButton("Wyloguj", (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(BoardSelectionActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        Toast.makeText(BoardSelectionActivity.this, "Wylogowano", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Anuluj", null)
                    .show();
        });

        View btnChangeGlobalBg = findViewById(R.id.btnChangeGlobalBg);
        if (btnChangeGlobalBg != null) {
            btnChangeGlobalBg.setOnClickListener(v -> showBackgroundDialog());
        }
    }

    private void listenForInvitations() {
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        if (currentUserEmail == null) return;

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
                .setTitle("New board invitation!")
                .setMessage("User " + inv.getFromUserEmail() + " has invited you to:\n\n'" + inv.getBoardName() + "'")
                .setPositiveButton("Accept", (dialog, which) -> {
                    db.collection("invitations").document(inv.getId())
                            .update("status", "accepted")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Successfuly joined!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    db.collection("invitations").document(inv.getId())
                            .update("status", "rejected");
                })
                .setCancelable(false)
                .show();
    }

    private void applyBackground(String bgType) {
        if (bgType == null) bgType = "brown";

        if (rootLayoutBoard != null) {
            switch (bgType) {
                case "brown":
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#A67C52"));
                    break;
                case "green":
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#2E8B57"));
                    break;
                case "blue":
                    rootLayoutBoard.setBackgroundColor(Color.parseColor("#4682B4"));
                    break;
                case "cork":
                    rootLayoutBoard.setBackgroundResource(R.drawable.corkboard);
                    break;
                case "chalk":
                    rootLayoutBoard.setBackgroundResource(R.drawable.chalkboard);
                    break;
                case "greenboard":
                    rootLayoutBoard.setBackgroundResource(R.drawable.greenboard);
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

        // Wspólna funkcja nasłuchująca dla wszystkich kafelków
        View.OnClickListener bgClickListener = v -> {
            String selectedKey = "brown"; // Domyślnie
            int id = v.getId();

            if (id == R.id.bgOptBrown) selectedKey = "brown";
            else if (id == R.id.bgOptGreen) selectedKey = "green";
            else if (id == R.id.bgOptBlue) selectedKey = "blue";
            else if (id == R.id.bgOptCork) selectedKey = "cork";
            else if (id == R.id.bgOptChalk) selectedKey = "chalk";
            else if (id == R.id.bgOptGreenboard) selectedKey = "greenboard";

            // Zapis do Firebase i natychmiastowa zmiana
            db.collection("users").document(currentUserId).update("mainBackground", selectedKey);
            applyBackground(selectedKey);

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            dialog.dismiss();
        };

        // Podpinamy nasłuchiwacz pod wszystkie 6 kart
        view.findViewById(R.id.bgOptBrown).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgOptGreen).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgOptBlue).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgOptCork).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgOptChalk).setOnClickListener(bgClickListener);
        view.findViewById(R.id.bgOptGreenboard).setOnClickListener(bgClickListener);

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
        if (color.equals("#FFF9C4")) rg.check(R.id.bColor1);
        else if (color.equals("#C8E6C9")) rg.check(R.id.bColor2);
        else if (color.equals("#FFCCBC")) rg.check(R.id.bColor3);
        else if (color.equals("#B3E5FC")) rg.check(R.id.bColor4);
        else if (color.equals("#D1C4E9")) rg.check(R.id.bColor5);
        else if (color.equals("#F8BBD0")) rg.check(R.id.bColor6);
        else if (color.equals("#B2EBF2")) rg.check(R.id.bColor7);
        else if (color.equals("#DCEDC8")) rg.check(R.id.bColor8);
        else if (color.equals("#FFE0B2")) rg.check(R.id.bColor9);
        else if (color.equals("#CFD8DC")) rg.check(R.id.bColor10);
        else if (color.equals("#FFD54F")) rg.check(R.id.bColor11);
        else if (color.equals("#81C784")) rg.check(R.id.bColor12);
        else if (color.equals("#64B5F6")) rg.check(R.id.bColor13);
        else if (color.equals("#BA68C8")) rg.check(R.id.bColor14);
        else if (color.equals("#FF8A65")) rg.check(R.id.bColor15);
        else if (color.equals("#4DB6AC")) rg.check(R.id.bColor16);
    }

    private String getSelectedColor(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.bColor1) return "#FFF9C4";
        if (id == R.id.bColor2) return "#C8E6C9";
        if (id == R.id.bColor3) return "#FFCCBC";
        if (id == R.id.bColor4) return "#B3E5FC";
        if (id == R.id.bColor5) return "#D1C4E9";
        if (id == R.id.bColor6) return "#F8BBD0";
        if (id == R.id.bColor7) return "#B2EBF2";
        if (id == R.id.bColor8) return "#DCEDC8";
        if (id == R.id.bColor9) return "#FFE0B2";
        if (id == R.id.bColor10) return "#CFD8DC";
        if (id == R.id.bColor11) return "#FFD54F";
        if (id == R.id.bColor12) return "#81C784";
        if (id == R.id.bColor13) return "#64B5F6";
        if (id == R.id.bColor14) return "#BA68C8";
        if (id == R.id.bColor15) return "#FF8A65";
        if (id == R.id.bColor16) return "#4DB6AC";
        return "#FFF9C4";
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

            if (!board.getOwnerId().equals(currentUserId)) {
                holder.tvSharedLabel.setVisibility(View.VISIBLE);
            } else {
                holder.tvSharedLabel.setVisibility(View.GONE);
            }
            holder.tvNoteCount.setText(board.getNoteCount() + " notes");

            try {
                holder.cardBoard.setCardBackgroundColor(Color.parseColor(board.getColor()));
            } catch (Exception e) {
                holder.cardBoard.setCardBackgroundColor(Color.parseColor("#448AFF"));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BoardSelectionActivity.this, BoardViewActivity.class);
                intent.putExtra("BOARD_ID", board.getId());
                intent.putExtra("BOARD_NAME", board.getName());
                intent.putExtra("OWNER_ID", board.getOwnerId());
                startActivity(intent);
            });



        }

        @Override
        public int getItemCount() { return boards.size(); }

        class BoardViewHolder extends RecyclerView.ViewHolder {
            TextView tvBoardTitle, tvNoteCount, tvSharedLabel;
            CardView cardBoard;
            public BoardViewHolder(View itemView) {
                super(itemView);
                tvBoardTitle = itemView.findViewById(R.id.tvBoardTitle);
                tvNoteCount = itemView.findViewById(R.id.tvNoteCount);
                cardBoard = itemView.findViewById(R.id.cardBoard);
                tvSharedLabel = itemView.findViewById(R.id.tvSharedLabel);
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
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else {
                holder.tvName.setText(board.getName());
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            // Otwieranie tablicy po kliknięciu w dowolne miejsce wiersza
            holder.itemView.setOnClickListener(v -> {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                Intent intent = new Intent(BoardSelectionActivity.this, BoardViewActivity.class);
                intent.putExtra("BOARD_ID", board.getId());
                intent.putExtra("BOARD_NAME", board.getName());
                intent.putExtra("OWNER_ID", board.getOwnerId());
                startActivity(intent);
            });

            holder.btnEdit.setOnClickListener(v -> {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                showBoardDialog(board);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (!board.getOwnerId().equals(currentUserId)) {
                    new AlertDialog.Builder(BoardSelectionActivity.this)
                            .setTitle("Leave board")
                            .setMessage("Are you sure you want to leave this board '" + board.getName() + "'?\nYou will lose access to it.")
                            .setPositiveButton("Leave", (dialog, which) -> {
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
                                                Toast.makeText(BoardSelectionActivity.this, "Left board", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    new AlertDialog.Builder(BoardSelectionActivity.this)
                            .setTitle("Delete board")
                            .setMessage("Are you sure you want to delete this board '" + board.getName() + "'?\nThis action is irreversible.")
                            .setPositiveButton("Delete", (dialog, which) -> { // Zmieniono "Usuń" na "Delete"
                                db.collection("users").document(currentUserId)
                                        .collection("boards").document(board.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(BoardSelectionActivity.this, "Deleted board", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
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