package com.example.projectwork.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.projectwork.R;

import java.util.Arrays;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    public interface OnRoleChangeListener {
        void onRoleChange(String userId, String newRole);
    }

    public interface OnRemoveMemberListener {
        void onRemoveMember(String userId);
    }

    public static class Member {
        public String userId;
        public String nickname;
        public String email;
        public String avatarUrl;
        public String role;

        public Member(String userId, String nickname, String email, String avatarUrl, String role) {
            this.userId = userId;
            this.nickname = nickname;
            this.email = email;
            this.avatarUrl = avatarUrl;
            this.role = role;
        }
    }

    private List<Member> members;
    private OnRoleChangeListener roleListener;
    private OnRemoveMemberListener removeListener;
    private boolean isOwner;
    private static final String[] ROLES = {"viewer", "editor", "admin"};

    public MemberAdapter(List<Member> members, boolean isOwner,
                         OnRoleChangeListener roleListener,
                         OnRemoveMemberListener removeListener) {
        this.members = members;
        this.isOwner = isOwner;
        this.roleListener = roleListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Member member = members.get(position);
        holder.tvNickname.setText(member.nickname);
        holder.tvEmail.setText(member.email);

        if (member.avatarUrl != null && !member.avatarUrl.isEmpty()) {
            Glide.with(holder.ivAvatar.getContext())
                    .load(member.avatarUrl)
                    .circleCrop()
                    .into(holder.ivAvatar);
        }

        if (member.role.equals("owner")) {
            holder.spinnerRole.setVisibility(View.GONE);
            holder.tvNickname.setText(member.nickname + " 👑");
        } else {
            holder.spinnerRole.setVisibility(View.VISIBLE);
            holder.spinnerRole.setEnabled(isOwner);

            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                    holder.itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    Arrays.asList(ROLES)
            );
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerRole.setAdapter(roleAdapter);

            int roleIndex = Arrays.asList(ROLES).indexOf(member.role);
            if (roleIndex >= 0) holder.spinnerRole.setSelection(roleIndex);

            holder.spinnerRole.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent,
                                                   View view, int pos, long id) {
                            String newRole = ROLES[pos];
                            if (!newRole.equals(member.role)) {
                                member.role = newRole;
                                roleListener.onRoleChange(member.userId, newRole);
                            }
                        }
                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });

            if (isOwner) {
                holder.itemView.setOnLongClickListener(v -> {
                    removeListener.onRemoveMember(member.userId);
                    return true;
                });
            }
        }
    }

    @Override
    public int getItemCount() { return members.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvNickname, tvEmail;
        Spinner spinnerRole;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            tvNickname = itemView.findViewById(R.id.tvMemberNickname);
            tvEmail = itemView.findViewById(R.id.tvMemberEmail);
            spinnerRole = itemView.findViewById(R.id.spinnerRole);
        }
    }
}