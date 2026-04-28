package com.example.noterello;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Noterello);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // BLOKADA 1: Auto-logowanie sprawdza, czy email jest zweryfikowany
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                startActivity(new Intent(LoginActivity.this, BoardSelectionActivity.class));
                finish();
            } else {
                mAuth.signOut(); // Pamięta go, ale nie zweryfikował, więc wyrzucamy
            }
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Wypełnij wszystkie pola!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // BLOKADA 2: Sprawdzamy przy ręcznym logowaniu
                            if (mAuth.getCurrentUser().isEmailVerified()) {
                                Toast.makeText(LoginActivity.this, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, BoardSelectionActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Najpierw zweryfikuj swój adres e-mail!", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Błąd: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Obsługa resetowania hasła
        tvForgotPassword.setOnClickListener(v -> showPasswordResetDialog());
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resetowanie hasła");
        builder.setMessage("Podaj swój adres e-mail, na który wyślemy link do zmiany hasła.");


        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Wyślij", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> Toast.makeText(LoginActivity.this, "Link wysłany! Sprawdź skrzynkę.", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(LoginActivity.this, "Wpisz e-mail!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}