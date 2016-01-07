package org.kore.kolabnotes.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;

import java.util.ArrayList;

/**
 * Created by yaroslav on 02.01.16.
 */
public abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    @SuppressWarnings("unused")
    private static final String TAG = SelectableAdapter.class.getSimpleName();

    private static SparseBooleanArray selectedItems;


    public  SelectableAdapter() {
        selectedItems = new SparseBooleanArray();
    }

    public boolean isSelected(int position) {
        return SelectableAdapter.getSelectedItems().contains(position);
    }

    public void toggleSelection(int position) {

        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        ArrayList<Integer> selection = getSelectedItems();
        selectedItems.clear();
        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public static ArrayList<Integer> getSelectedItems() {
        ArrayList<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); ++i) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void setSelectedItems(ArrayList<Integer> items) {
        for (int i : items) {
            selectedItems.put(i, true);
        }
    }
}
