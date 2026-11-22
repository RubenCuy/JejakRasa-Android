package com.example.jejakrasa.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jejakrasa.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewHistoryActivity extends AppCompatActivity {

    private RecyclerView rvReviewHistory;
    private TextView tvEmptyHistory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<ReviewItem> reviewList = new ArrayList<>();
    private HistoryAdapter adapter;
    private SwipeTabHelper swipeTabHelper;

    static class ReviewItem {
        String id;
        String placeName;
        double rating;
        String comment;
        String dateText;
        long timestamp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        swipeTabHelper = new SwipeTabHelper(this, SwipeTabHelper.TAB_HISTORY);

        rvReviewHistory = findViewById(R.id.rvReviewHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        BottomNavigationView bottomNavHistory = findViewById(R.id.bottomNavHistory);

        bottomNavHistory.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        rvReviewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        rvReviewHistory.setAdapter(adapter);

        muatRiwayatUlasan();

        bottomNavHistory.setSelectedItemId(R.id.nav_history);
        bottomNavHistory.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_HISTORY, SwipeTabHelper.TAB_EXPLORE);
                return true;
            } else if (id == R.id.nav_saved) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_HISTORY, SwipeTabHelper.TAB_SAVED);
                return true;
            } else if (id == R.id.nav_profile) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_HISTORY, SwipeTabHelper.TAB_PROFILE);
                return true;
            }
            return id == R.id.nav_history;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        swipeTabHelper.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void muatRiwayatUlasan() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("reviews")
                .whereEqualTo("uid", user.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    reviewList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ReviewItem item = new ReviewItem();
                        item.id = doc.getId();
                        item.placeName = doc.getString("placeName");
                        Double r = doc.getDouble("rating");
                        item.rating = r != null ? r : 5.0;
                        item.comment = doc.getString("comment");
                        item.dateText = doc.getString("dateText");
                        Long ts = doc.getLong("timestamp");
                        item.timestamp = ts != null ? ts : 0L;
                        reviewList.add(item);
                    }

                    Collections.sort(reviewList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    adapter.notifyDataSetChanged();
                    tvEmptyHistory.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReviewItem item = reviewList.get(position);
            holder.tvPlace.setText(item.placeName != null ? item.placeName : "Tempat Kuliner");
            holder.tvRating.setText("★ " + item.rating);
            holder.tvDate.setText(item.dateText != null ? item.dateText : "");
            holder.tvComment.setText(item.comment != null ? item.comment : "");

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ReviewHistoryActivity.this)
                        .setTitle("Hapus Riwayat Ulasan")
                        .setMessage("Hapus ulasan untuk " + item.placeName + "?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            db.collection("reviews").document(item.id).delete()
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(ReviewHistoryActivity.this, "Ulasan dihapus", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return reviewList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPlace, tvRating, tvDate, tvComment;
            Button btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPlace = itemView.findViewById(R.id.tvHistoryPlaceName);
                tvRating = itemView.findViewById(R.id.tvHistoryRating);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvComment = itemView.findViewById(R.id.tvHistoryComment);
                btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
            }
        }
    }
}