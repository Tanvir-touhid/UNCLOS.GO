package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView articleIcon;
        ImageView readIndicator;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.articleTitleTextView);
            articleIcon = itemView.findViewById(R.id.articleIcon);
            readIndicator = itemView.findViewById(R.id.readIndicator);
        }
    }

    private BroadcastReceiver fontSizeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("FONT_SIZE_CHANGED".equals(intent.getAction())) {
                notifyDataSetChanged();
            }
        }
    };

    private Context context;
    private List<Article> originalList;
    private List<Article> filteredList;

    public ArticleAdapter(Context context, List<Article> articleList) {
        this.context = context;
        this.originalList = new ArrayList<>(articleList);
        this.filteredList = new ArrayList<>(articleList);

        // Register receiver for font size changes
        IntentFilter filter = new IntentFilter("FONT_SIZE_CHANGED");
        context.registerReceiver(fontSizeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    // Add this method to clean up the receiver
    public void cleanup() {
        try {
            context.unregisterReceiver(fontSizeChangeReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        if (filteredList == null || position < 0 || position >= filteredList.size()) return;

        Article article = filteredList.get(position);

        // Set title and text color
        holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        String fullTitle = article.getTitle();
        int colonIndex = fullTitle.indexOf(':');
        String rawTitle = (colonIndex != -1) ? fullTitle.substring(colonIndex + 1).trim() : fullTitle;

        String label = "Article " + article.getId() + ":";
        String fullText = label + "\n" + rawTitle;

        // Make the label bold
        SpannableString spannable = new SpannableString(fullText);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.titleTextView.setText(spannable);

        // Set article icon
        holder.articleIcon.setImageResource(article.getIconResId());
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            holder.articleIcon.setAlpha(0.8f);
        } else {
            holder.articleIcon.setAlpha(1.0f);
        }

        // Handle font size from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        float fontSize = prefs.getFloat("font_size_value", 16f);
        holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        // Animation
        holder.itemView.setTranslationY(300f);
        holder.itemView.animate()
                .translationY(0f)
                .setInterpolator(new OvershootInterpolator(2f))
                .setDuration(700)
                .start();

        // Read status indicator
        boolean isRead = prefs.getBoolean("read_" + fullTitle, false);
        holder.readIndicator.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.readIndicator.setImageResource(R.drawable.ic_check);

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArticleDetailActivity.class);
            intent.putExtra("title", article.getTitle());
            intent.putExtra("content", article.getContent());
            context.startActivity(intent);
        });

        // Ensure proper text layout
        holder.titleTextView.post(() -> {
            holder.titleTextView.setMaxLines(Integer.MAX_VALUE);
            holder.titleTextView.invalidate();
            holder.titleTextView.requestLayout();
        });
    }

    public void updateReadStatuses() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            text = text.toLowerCase();
            for (Article article : originalList) {
                if (article.getTitle().toLowerCase().contains(text)) {
                    filteredList.add(article);
                }
            }
        }
        notifyDataSetChanged();
    }
}