package com.example.jejakrasa.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etBirthDate, etRegEmail, etRegPassword;
    private Button btnSubmitRegister, btnBackToLogin;
    private ProgressBar progressRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etBirthDate = findViewById(R.id.etBirthDate);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progressRegister = findViewById(R.id.progressRegister);

        etBirthDate.setOnClickListener(v -> tampilkanKalender());
        btnSubmitRegister.setOnClickListener(v -> prosesDaftarAkun());
        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void tampilkanKalender() {
        Calendar cal = Calendar.getInstance();
        int tahunDefault = cal.get(Calendar.YEAR) - 20;
        int bulanDefault = cal.get(Calendar.MONTH);
        int hariDefault = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String tanggalTerpilih = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etBirthDate.setText(tanggalTerpilih);
                    etBirthDate.setError(null);
                },
                tahunDefault, bulanDefault, hariDefault
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void prosesDaftarAkun() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError("Nama depan wajib diisi");
            etFirstName.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Nama belakang wajib diisi");
            etLastName.requestFocus();
            return;
        }
        if (birthDate.isEmpty()) {
            etBirthDate.setError("Pilih tanggal lahir dari kalender");
            tampilkanKalender();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Format email tidak valid");
            etRegEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegPassword.setError("Kata sandi minimal 6 karakter");
            etRegPassword.requestFocus();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        setLoading(false);
                        return;
                    }

                    // 1. Tunggu email verifikasi benar-benar terkirim dulu
                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                // 2. Setelah email terkirim, simpan biodata ke Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("uid", user.getUid());
                                userData.put("firstName", firstName);
                                userData.put("lastName", lastName);
                                userData.put("birthDate", birthDate);
                                userData.put("email", email);

                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnCompleteListener(task -> {
                                            setLoading(false);
                                            mAuth.signOut();
                                            new AlertDialog.Builder(this)
                                                    .setTitle("Verifikasi Email Terkirim!")
                                                    .setMessage("Link verifikasi resmi sudah dikirim ke " + email
                                                            + ".\n\nBuka Gmail, ketik 'jejakrasa' di kotak pencarian Gmail (termasuk folder Spam), lalu klik link di dalamnya sebelum Masuk.")
                                                    .setPositiveButton("Mengerti", (d, w) -> finish())
                                                    .setCancelable(false)
                                                    .show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Gagal kirim email verifikasi: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Pendaftaran gagal: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitRegister.setEnabled(!loading);
    }
}