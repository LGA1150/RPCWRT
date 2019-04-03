package com.example.android.rpcwrt.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.rpcwrt.R;
import com.example.android.rpcwrt.model.*;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<Item> items = new ArrayList<>();

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_view, parent, false));
        return viewHolder;
    }
    public void addItem(Item item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public void removeItem(int i) {
        items.remove(i);
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return items.size();
    }

    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        Item item = items.get(i);
        viewHolder.title.setText(item.title);
        viewHolder.content.setText(item.content);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView content;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            content = view.findViewById(R.id.content);
        }
    }
}
