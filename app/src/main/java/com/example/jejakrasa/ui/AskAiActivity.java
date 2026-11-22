package com.example.jejakrasa.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.R;
import com.example.jejakrasa.model.CulinarySpot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AskAiActivity extends AppCompatActivity {

    private LinearLayout layoutChatContainer;
    private ScrollView scrollChatAi;
    private EditText etPromptAi;
    private final List<CulinarySpot> liveSpots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ask_ai);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            scrollToBottom();
            return insets;
        });

        layoutChatContainer = findViewById(R.id.layoutChatContainer);
        scrollChatAi = findViewById(R.id.scrollChatAi);
        etPromptAi = findViewById(R.id.etPromptAi);
        Button btnSendAi = findViewById(R.id.btnSendAi);
        TextView btnBackAi = findViewById(R.id.btnBackAi);

        Button btnSuggestHelp = findViewById(R.id.btnSuggestHelp);
        Button btnSuggestJakarta = findViewById(R.id.btnSuggestJakarta);
        Button btnSuggestBudget = findViewById(R.id.btnSuggestBudget);
        Button btnSuggestCoffee = findViewById(R.id.btnSuggestCoffee);

        btnBackAi.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        muatKatalogLive();

        addBubble("Halo! Saya Kurator AI JejakRasa.\n\nSaya terhubung langsung ke katalog kuliner aplikasi ini. Ketuk tombol 'Bisa bantu apa?' di atas atau tulis keinginanmu!", false);

        btnSendAi.setOnClickListener(v -> kirimPertanyaan(etPromptAi.getText().toString()));
        etPromptAi.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                kirimPertanyaan(etPromptAi.getText().toString());
                return true;
            }
            return false;
        });

        btnSuggestHelp.setOnClickListener(v -> kirimPertanyaan("Kamu bisa bantu apa saja?"));
        btnSuggestJakarta.setOnClickListener(v -> kirimPertanyaan("Rekomendasi kuliner legendaris di Jakarta"));
        btnSuggestBudget.setOnClickListener(v -> kirimPertanyaan("Makanan enak dengan budget di bawah 30rb"));
        btnSuggestCoffee.setOnClickListener(v -> kirimPertanyaan("Rekomendasi kopi atau jajanan legendaris"));
    }

    private void muatKatalogLive() {
        FirebaseFirestore.getInstance().collection("culinary_spots")
                .get()
                .addOnSuccessListener(snapshots -> {
                    liveSpots.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        CulinarySpot spot = doc.toObject(CulinarySpot.class);
                        if (spot != null) liveSpots.add(spot);
                    }
                });
    }

    private void kirimPertanyaan(String prompt) {
        String text = prompt.trim();
        if (text.isEmpty()) return;

        addBubble(text, true);
        etPromptAi.setText("");

        String jawaban = hasilkanRekomendasi(text.toLowerCase());
        addBubble(jawaban, false);
    }

    private String hasilkanRekomendasi(String q) {
        if (q.contains("bantu") || q.contains("fitur") || q.contains("bisa apa") || q.contains("halo")) {
            return "Saya bisa membantumu:\n"
                    + "• Mencari kuliner berdasarkan kota (contoh: 'Jakarta', 'Jogja', 'Solo')\n"
                    + "• Memfilter berdasarkan budget (contoh: 'di bawah 30rb', 'murah')\n"
                    + "• Mencari kategori ('kopi', 'makanan berat', 'jajanan')\n"
                    + "• Memilih rekomendasi acak kalau kamu bingung ('rekomendasi acak')\n"
                    + "• Menampilkan seluruh daftar kuliner legendaris ('semua kuliner')";
        }

        if (liveSpots.isEmpty()) {
            return "Sebentar ya, katalog kuliner sedang disinkronkan dari server. Coba kirim ulang pertanyaanmu dalam 2 detik!";
        }

        if (q.contains("semua") || q.contains("daftar") || q.contains("katalog")) {
            StringBuilder sb = new StringBuilder("Berikut seluruh " + liveSpots.size() + " kuliner legendaris di JejakRasa:\n\n");
            for (CulinarySpot s : liveSpots) {
                sb.append("• ").append(s.getNamaTempat())
                        .append(" (").append(s.getAlamatSingkat()).append(")\n  ")
                        .append(s.getKisaranHarga()).append(" • ★ ").append(s.getRating()).append("\n\n");
            }
            return sb.toString().trim();
        }

        if (q.contains("acak") || q.contains("bingung") || q.contains("terserah")) {
            CulinarySpot pick = liveSpots.get(new Random().nextInt(liveSpots.size()));
            return "Pilihan spesial untukmu hari ini:\n\n★ " + pick.getNamaTempat()
                    + " (Berdiri " + pick.getTahunBerdiri() + ")\n"
                    + "Lokasi: " + pick.getAlamatSingkat() + "\n"
                    + "Harga: " + pick.getKisaranHarga() + "\n\n"
                    + pick.getKisahLegendaris();
        }

        List<CulinarySpot> cocok = new ArrayList<>();
        for (CulinarySpot s : liveSpots) {
            String nama = s.getNamaTempat() != null ? s.getNamaTempat().toLowerCase() : "";
            String alamat = s.getAlamatSingkat() != null ? s.getAlamatSingkat().toLowerCase() : "";
            String kategori = s.getKategori() != null ? s.getKategori().toLowerCase() : "";
            String harga = s.getKisaranHarga() != null ? s.getKisaranHarga().toLowerCase() : "";

            boolean matchCity = (q.contains("jakarta") && alamat.contains("jakarta"))
                    || ((q.contains("jogja") || q.contains("yogyakarta")) && alamat.contains("yogyakarta"))
                    || ((q.contains("solo") || q.contains("surakarta")) && alamat.contains("surakarta"));

            boolean matchBudget = (q.contains("30") || q.contains("murah") || q.contains("hemat"))
                    && (harga.contains("20.000") || harga.contains("25.000") || harga.contains("28.000"));

            boolean matchCat = (q.contains("kopi") && kategori.contains("kopi"))
                    || (q.contains("jajanan") && kategori.contains("jajanan"))
                    || (q.contains("berat") && kategori.contains("berat"));

            boolean matchKeyword = nama.contains(q) || alamat.contains(q);

            if (matchCity || matchBudget || matchCat || matchKeyword) {
                cocok.add(s);
            }
        }

        if (!cocok.isEmpty()) {
            StringBuilder sb = new StringBuilder("Ditemukan " + cocok.size() + " rekomendasi yang pas untukmu:\n\n");
            for (CulinarySpot s : cocok) {
                sb.append("★ ").append(s.getNamaTempat())
                        .append(" (Sejak ").append(s.getTahunBerdiri()).append(")\n")
                        .append("Lokasi: ").append(s.getAlamatSingkat()).append("\n")
                        .append("Harga: ").append(s.getKisaranHarga()).append("\n\n");
            }
            return sb.toString().trim();
        }

        return "Belum ada yang persis cocok dengan '" + q + "'. Coba ketik nama kota (Jakarta, Jogja, Solo), 'budget di bawah 30rb', 'kopi', atau ketuk 'Bisa bantu apa?'!";
    }

    private void addBubble(String message, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(14f);
        tv.setPadding(36, 26, 36, 26);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 22;

        if (isUser) {
            params.gravity = Gravity.END;
            params.leftMargin = 100;
            GradientDrawable userBg = new GradientDrawable();
            userBg.setColor(ContextCompat.getColor(this, R.color.primary_terracotta));
            userBg.setCornerRadius(36f);
            tv.setBackground(userBg);
            tv.setTextColor(Color.WHITE);
        } else {
            params.gravity = Gravity.START;
            params.rightMargin = 70;
            tv.setBackgroundResource(R.drawable.bg_search_bar);
            tv.setTextColor(Color.parseColor("#1C1917"));
        }

        tv.setLayoutParams(params);
        layoutChatContainer.addView(tv);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollChatAi.post(() -> scrollChatAi.fullScroll(View.FOCUS_DOWN));
    }
}