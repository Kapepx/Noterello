package com.example.noterello;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class BoardSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddBoard;
    private BoardAdapter adapter;
    private List<Board> boardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_selection);

        recyclerView = findViewById(R.id.recyclerViewBoards);
        fabAddBoard = findViewById(R.id.fabAddBoard);

        // Dane testowe
        boardList = new ArrayList<>();
        boardList.add(new Board("1", "School", "#448AFF", 12));
        boardList.add(new Board("2", "Work", "#00C853", 8));
        boardList.add(new Board("3", "Private", "#FFD600", 5));
        boardList.add(new Board("4", "Gym", "#FF6D00", 3));

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BoardAdapter(boardList);
        recyclerView.setAdapter(adapter);

        fabAddBoard.setOnClickListener(v -> showAddBoardDialog());
    }

    private void showAddBoardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_board, null);
        builder.setView(view);

        final EditText etTitle = view.findViewById(R.id.etBoardTitle);
        final RadioGroup rgColors = view.findViewById(R.id.rgBoardColors);
        final Button btnCreate = view.findViewById(R.id.btnCreate);
        final Button btnCancel = view.findViewById(R.id.btnCancel);
        final ImageView btnClose = view.findViewById(R.id.btnClose);

        final AlertDialog dialog = builder.create();

        btnCreate.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            if (!title.isEmpty()) {
                String color = getSelectedColor(rgColors);
                Board newBoard = new Board(String.valueOf(System.currentTimeMillis()), title, color, 0);
                boardList.add(newBoard);
                adapter.notifyItemInserted(boardList.size() - 1);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private String getSelectedColor(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.bColor1) return "#448AFF";
        if (id == R.id.bColor2) return "#00C853";
        if (id == R.id.bColor3) return "#FFD600";
        if (id == R.id.bColor4) return "#FF6D00";
        if (id == R.id.bColor5) return "#AA00FF";
        return "#448AFF";
    }

    private class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.BoardViewHolder> {
        private List<Board> boards;

        public BoardAdapter(List<Board> boards) {
            this.boards = boards;
        }

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
            holder.tvNoteCount.setText(board.getNoteCount() + " notes");
            holder.cardBoard.setCardBackgroundColor(Color.parseColor(board.getColor()));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BoardSelectionActivity.this, BoardViewActivity.class);
                intent.putExtra("BOARD_NAME", board.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return boards.size();
        }

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
}