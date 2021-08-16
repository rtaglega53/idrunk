package com.uag.micros.idrunkapp.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ItemMarginDecoration extends RecyclerView.ItemDecoration {
    public int mMargin;

    public ItemMarginDecoration(int margin) {
        mMargin = margin;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.left = mMargin;
        outRect.right = mMargin;
        outRect.bottom = mMargin;
        outRect.top = mMargin;
    }
}
