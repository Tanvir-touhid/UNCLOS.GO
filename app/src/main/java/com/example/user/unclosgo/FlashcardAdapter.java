package com.example.user.unclosgo;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {

    public interface OnFlashcardClickListener {
        void onFlashcardClick(Flashcard flashcard);
    }

    public interface OnFlashcardDeleteListener {
        void onFlashcardDelete(Flashcard flashcard);
    }

    private List<Flashcard> flashcards;
    private OnFlashcardClickListener clickListener;
    private OnFlashcardDeleteListener deleteListener;

    public FlashcardAdapter(List<Flashcard> flashcards,
                            OnFlashcardClickListener clickListener,
                            OnFlashcardDeleteListener deleteListener) {
        this.flashcards = flashcards;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new ViewHolder(view, clickListener, deleteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Flashcard flashcard = flashcards.get(position);
        holder.bind(flashcard);
    }

    @Override
    public int getItemCount() {
        return flashcards.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView contentTextView;
        private final ImageButton moreButton;
        private Flashcard currentFlashcard;

        public ViewHolder(View itemView,
                          OnFlashcardClickListener clickListener,
                          OnFlashcardDeleteListener deleteListener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_flashcard_title);
            contentTextView = itemView.findViewById(R.id.text_view_flashcard_content);
            moreButton = itemView.findViewById(R.id.more_button);

            itemView.setOnClickListener(v -> {
                if (clickListener != null && currentFlashcard != null) {
                    clickListener.onFlashcardClick(currentFlashcard);
                }
            });

            moreButton.setOnClickListener(v -> {
                // Inflate your custom popup layout
                View popupView = LayoutInflater.from(v.getContext()).inflate(R.layout.popup_flashcard_menu, null);

                // Create the PopupWindow, set width & height explicitly
                PopupWindow popupWindow = new PopupWindow(popupView,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, v.getContext().getResources().getDisplayMetrics()),
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        true);

                // Set background drawable to enable outside touch dismissal
                popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(v.getContext(), R.drawable.popup_menu_background));
                popupWindow.setOutsideTouchable(true);
                popupWindow.setFocusable(true);

                // Show popup anchored to the button
                popupWindow.showAsDropDown(v, 0, 0);

                // Handle clicks inside popup
                TextView deleteItem = popupView.findViewById(R.id.menu_delete);
                deleteItem.setOnClickListener(view -> {
                    // Call your delete listener
                    if (currentFlashcard != null && deleteListener != null) {
                        deleteListener.onFlashcardDelete(currentFlashcard);
                    }
                    popupWindow.dismiss();
                });
            });
        }
        public void bind(Flashcard flashcard) {
            currentFlashcard = flashcard;
            titleTextView.setText(flashcard.getTitle());
            contentTextView.setText(flashcard.getContent());
        }
    }
}