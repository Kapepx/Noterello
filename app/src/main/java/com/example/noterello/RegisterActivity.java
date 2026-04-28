package com.example.noterello;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicjalizacja Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                // 1. Walidacja pustych pól
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Proszę wypełnić wszystkie pola!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Walidacja zgodności haseł
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Hasła nie są identyczne!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 3. Wymóg Firebase: hasło min. 6 znaków
                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Hasło musi mieć co najmniej 6 znaków!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 4. Tworzenie konta w Firebase Auth
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sukces! Konto utworzone.
                                    String userId = mAuth.getCurrentUser().getUid();

                                    // Tworzymy pełnoprawny obiekt User z losowym kolorem awatara
                                    String avatarColor = getRandomHexColor();
                                    User newUser = new User(userId, username, email.toLowerCase(), avatarColor, "Viewer");

                                    // 5. Zapisujemy obiekt User w Firestore
                                    db.collection("users").document(userId)
                                            .set(newUser)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> taskDb) {
                                                    if (taskDb.isSuccessful()) {
                                                        // 6. Wysyłamy link aktywacyjny na e-mail
                                                        mAuth.getCurrentUser().sendEmailVerification()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> verificationTask) {
                                                                        if (verificationTask.isSuccessful()) {
                                                                            Toast.makeText(RegisterActivity.this,
                                                                                    "Konto utworzone! Sprawdź maila, aby je aktywować.",
                                                                                    Toast.LENGTH_LONG).show();

                                                                            // Wylogowujemy, dopóki nie kliknie w link
                                                                            mAuth.signOut();

                                                                            // Zamykamy ekran rejestracji (wraca do logowania)
                                                                            finish();
                                                                        } else {
                                                                            Toast.makeText(RegisterActivity.this,
                                                                                    "Błąd wysyłania linku aktywacyjnego.",
                                                                                    Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Błąd zapisu profilu w bazie.", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                } else {
                                    // Wyłapanie błędów (np. taki e-mail już istnieje w bazie)
                                    Toast.makeText(RegisterActivity.this, "Błąd: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Wraca do ekranu logowania
            }
        });
    }

    private String getRandomHexColor() {
        String[] colors = {"#FF5252", "#FF4081", "#E040FB", "#7C4DFF", "#536DFE", "#448AFF", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#FF9800", "#FF5722", "#795548", "#607D8B"};
        return colors[new java.util.Random().nextInt(colors.length)];
    }
}