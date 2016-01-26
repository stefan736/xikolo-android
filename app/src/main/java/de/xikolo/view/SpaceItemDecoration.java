package de.xikolo.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int horizontalSpacing;
    private int verticalSpacing;
    private boolean includeEdge;
    private RecyclerViewInfo recyclerViewInfo;

    public SpaceItemDecoration(int horizontalSpacing, int verticalSpacing, boolean includeEdge, RecyclerViewInfo recyclerViewInfo) {
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        this.includeEdge = includeEdge;
        this.recyclerViewInfo = recyclerViewInfo;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);

        if (!recyclerViewInfo.isHeader(position)) {
            int spanCount = recyclerViewInfo.getSpanCount();

            int lastHeader = 0;

            for (int i = position - 1; i >= 0; i--) {
                if (recyclerViewInfo.isHeader(i)) {
                    lastHeader = i;
                    break;
                }
            }

            int relativePosition = position - lastHeader - 1;
            int column = relativePosition % spanCount;

            if (includeEdge || column != 0) {
                outRect.left = horizontalSpacing / 2;
            }
            if (includeEdge || column != spanCount - 1) {
                outRect.right = horizontalSpacing / 2;
            }
            if (includeEdge || relativePosition >= spanCount) {
                outRect.top = verticalSpacing / 2;
            }
            if (includeEdge || position < recyclerViewInfo.getItemCount() - spanCount) {
                outRect.bottom = verticalSpacing / 2;
            }
        }
    }

    public interface RecyclerViewInfo {

        boolean isHeader(int position);

        int getSpanCount();

        int getItemCount();

    }

}
