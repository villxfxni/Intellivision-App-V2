package com.example.directionstest.ADAPTERS;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.directionstest.ENTITY.Command;
import com.example.directionstest.R;

import java.util.List;

public class CommandsAdapter extends RecyclerView.Adapter<CommandsAdapter.CommandViewHolder> {

    private List<Command> comandos;

    public CommandsAdapter(List<Command> comandos) {
        this.comandos = comandos;
    }

    @NonNull
    @Override
    public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.command_item, parent, false);
        return new CommandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {
        Command command = comandos.get(position);
        holder.icon.setImageResource(command.getIconResId());
        holder.title.setText(command.getTitle());
        holder.descripcion.setText(command.getDescription());
    }

    @Override
    public int getItemCount() {
        return comandos.size();
    }

    public static class CommandViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView descripcion;

        public CommandViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.commandIcon);
            title = itemView.findViewById(R.id.commandTitle);
            descripcion = itemView.findViewById(R.id.commandDescription);
        }
    }
}

