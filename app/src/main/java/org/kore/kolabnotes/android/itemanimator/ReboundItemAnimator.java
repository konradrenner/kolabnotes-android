package org.kore.kolabnotes.android.itemanimator;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class ReboundItemAnimator extends RecyclerView.ItemAnimator {
    //hold the views to animate in runPendingAnimations
    private List<RecyclerView.ViewHolder> mViewHolders = new ArrayList<RecyclerView.ViewHolder>();


    @Override
    public void runPendingAnimations() {
        if (!mViewHolders.isEmpty()) {
            for (final RecyclerView.ViewHolder viewHolder : mViewHolders) {
                //todo
            }
        }
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder viewHolder) {
        viewHolder.itemView.animate().alpha(0).scaleX(0).scaleY(0).setDuration(300).start();
        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder viewHolder) {
        //viewHolder.itemView.setAlpha(0.0f);
        viewHolder.itemView.setScaleX(0);
        viewHolder.itemView.setScaleY(0);
        return mViewHolders.add(viewHolder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder viewHolder, int i, int i2, int i3, int i4) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, int i, int i2, int i3, int i4) {
        return false;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void endAnimations() {
    }

    @Override
    public boolean isRunning() {
        return !mViewHolders.isEmpty();
    }

}