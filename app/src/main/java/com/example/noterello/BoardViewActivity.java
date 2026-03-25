package com.example.noterello;

import android.graphics.Color;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardViewActivity extends AppCompatActivity {

    private TextView tvBoardName;
    private RecyclerView rvNotes;
    private FloatingActionButton fabAddNote;
    private ImageView btnShareBoard;
    private NoteAdapter adapter;
    private List<Note> noteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_view);

        tvBoardName = findViewById(R.id.tvBoardName);
        rvNotes = findViewById(R.id.rvNotes);
        fabAddNote = findViewById(R.id.fabAddNote);
        btnShareBoard = findViewById(R.id.btnShareBoard);

        String boardName = getIntent().getStringExtra("BOARD_NAME");
        if (boardName != null) {
            tvBoardName.setText(boardName);
        }

        noteList = new ArrayList<>();
        noteList.add(new Note("1", "Class Schedule", "Class schedule for this week", "#FFD54F", true, true));
        noteList.add(new Note("2", "To-Do List", "Math homework\nRead Chapter 5", "#A7FFEB", false, false));
        noteList.add(new Note("3", "Upcoming Exams", "Math: March 15\nPhysics: March 18", "#FFCCBC", false, false));

        sortNotes();

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvNotes.setLayoutManager(layoutManager);

        adapter = new NoteAdapter(noteList);
        rvNotes.setAdapter(adapter);

        fabAddNote.setOnClickListener(v -> showNoteDialog(null));
        btnShareBoard.setOnClickListener(v -> showShareDialog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.llAvatars).setOnClickListener(v -> showShareDialog());
    }

    private void sortNotes() {
        Collections.sort(noteList, (n1, n2) -> {
            if (n1.isPinned() && !n2.isPinned()) return -1;
            if (!n1.isPinned() && n2.isPinned()) return 1;
            return 0;
        });
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

        List<User> members = new ArrayList<>();
        members.add(new User("1", "Alice Johnson", "alice@example.com", "#448AFF", "Admin"));
        members.add(new User("2", "Bob Smith", "bob@example.com", "#00C853", "Editor"));
        members.add(new User("3", "Carol Williams", "carol@example.com", "#AA00FF", "Editor"));
        members.add(new User("4", "David Brown", "david@example.com", "#FF6D00", "Viewer"));

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(new MemberAdapter(members));

        final AlertDialog dialog = builder.create();

        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        
        btnInvite.setOnClickListener(v -> {
            String email = etSearch.getText().toString();
            if (!email.isEmpty()) {
                Toast.makeText(this, "Invitation sent to: " + email, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Enter email address!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
        private List<User> members;
        public MemberAdapter(List<User> members) { this.members = members; }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MemberViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            User user = members.get(position);
            holder.tvName.setText(user.getUsername());
            holder.tvEmail.setText(user.getEmail());
            holder.tvAvatar.setText(user.getInitials());
            holder.tvAvatar.setBackgroundColor(Color.parseColor(user.getAvatarColor()));
            holder.tvRole.setText(user.getRole());
            
            updateRoleColor(holder.tvRole, user.getRole());

            holder.tvRole.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(BoardViewActivity.this, holder.tvRole);
                popup.getMenu().add("Admin");
                popup.getMenu().add("Editor");
                popup.getMenu().add("Viewer");

                popup.setOnMenuItemClickListener(item -> {
                    String selectedRole = item.getTitle().toString();
                    user.setRole(selectedRole);
                    holder.tvRole.setText(selectedRole);
                    updateRoleColor(holder.tvRole, selectedRole);
                    Toast.makeText(BoardViewActivity.this, "Role updated to " + selectedRole, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        private void updateRoleColor(TextView tvRole, String role) {
            if (role.equals("Admin")) {
                tvRole.setTextColor(Color.parseColor("#FF5252"));
            } else if (role.equals("Editor")) {
                tvRole.setTextColor(Color.parseColor("#448AFF"));
            } else {
                tvRole.setTextColor(Color.parseColor("#9E9E9E"));
            }
        }

        @Override
        public int getItemCount() { return members.size(); }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvAvatar, tvRole;
            public MemberViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMemberName);
                tvEmail = v.findViewById(R.id.tvMemberEmail);
                tvAvatar = v.findViewById(R.id.tvMemberAvatar);
                tvRole = v.findViewById(R.id.tvMemberRole);
            }
        }
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
            
            // Show delete button when editing
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                noteList.remove(noteToEdit);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            });
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
                    noteList.add(0, new Note(String.valueOf(System.currentTimeMillis()), title, content, color, btnRectangle.isChecked(), cbPinned.isChecked()));
                } else {
                    noteToEdit.setTitle(title);
                    noteToEdit.setContent(content);
                    noteToEdit.setColor(color);
                    noteToEdit.setFullWidth(btnRectangle.isChecked());
                    noteToEdit.setPinned(cbPinned.isChecked());
                }
                sortNotes();
                adapter.notifyDataSetChanged();
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

    private void setPaletteSelection(RadioGroup rg, String color) {
        if (color.equals("#FFF9C4")) rg.check(R.id.color1);
        else if (color.equals("#C8E6C9")) rg.check(R.id.color2);
        else if (color.equals("#FFCCBC")) rg.check(R.id.color3);
        else if (color.equals("#B3E5FC")) rg.check(R.id.color4);
        else if (color.equals("#D1C4E9")) rg.check(R.id.color5);
    }

    private String getSelectedColor(RadioGroup rg) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.color1) return "#FFF9C4";
        if (id == R.id.color2) return "#C8E6C9";
        if (id == R.id.color3) return "#FFCCBC";
        if (id == R.id.color4) return "#B3E5FC";
        if (id == R.id.color5) return "#D1C4E9";
        return "#FFF9C4";
    }

    private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
        private List<Note> notes;
        public NoteAdapter(List<Note> notes) { this.notes = notes; }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.tvTitle.setText(note.getTitle());
            holder.tvContent.setText(note.getContent());
            holder.cardNote.setCardBackgroundColor(Color.parseColor(note.getColor()));
            holder.ivPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> showNoteDialog(note));
            holder.itemView.setOnLongClickListener(v -> {
                note.toggleFullWidth();
                notifyItemChanged(holder.getAdapterPosition());
                return true;
            });
            StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            lp.setFullSpan(note.isFullWidth());
            holder.itemView.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() { return notes.size(); }

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
    }
}
