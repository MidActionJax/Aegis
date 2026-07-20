package com.example.projectaegis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectaegis.data.Credential;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CredentialAdapter extends RecyclerView.Adapter<CredentialAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(Credential credential);

        void onCopyPassword(Credential credential);
    }

    private final List<Credential> credentials = new ArrayList<>();
    private final Set<Long> revealedIds = new HashSet<>();
    private final Listener listener;

    public CredentialAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Credential> newList) {
        credentials.clear();
        credentials.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_credential, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Credential credential = credentials.get(position);
        boolean revealed = revealedIds.contains(credential.getId());

        String name = credential.getAccountName();
        holder.avatarLetter.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        holder.accountName.setText(name);
        holder.username.setText(credential.getUsername());
        holder.passwordMasked.setText(revealed ? credential.getPassword() : maskOf(credential.getPassword()));
        holder.toggleVisibility.setImageResource(revealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(credential));
        holder.toggleVisibility.setOnClickListener(v -> {
            if (revealedIds.contains(credential.getId())) {
                revealedIds.remove(credential.getId());
            } else {
                revealedIds.add(credential.getId());
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
        });
        holder.copyButton.setOnClickListener(v -> listener.onCopyPassword(credential));
    }

    @Override
    public int getItemCount() {
        return credentials.size();
    }

    private String maskOf(String password) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.max(password.length(), 8); i++) {
            builder.append('•');
        }
        return builder.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView avatarLetter;
        final TextView accountName;
        final TextView username;
        final TextView passwordMasked;
        final ImageButton toggleVisibility;
        final ImageButton copyButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarLetter = itemView.findViewById(R.id.avatarLetter);
            accountName = itemView.findViewById(R.id.accountName);
            username = itemView.findViewById(R.id.username);
            passwordMasked = itemView.findViewById(R.id.passwordMasked);
            toggleVisibility = itemView.findViewById(R.id.toggleVisibility);
            copyButton = itemView.findViewById(R.id.copyButton);
        }
    }
}
