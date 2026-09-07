package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.jijipos.R;

public class StaffFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        TextView title = view.findViewById(R.id.textFragmentTitle);
        title.setText("👥 Staff Management Portal\n\n• Register Cashier Accounts\n• Audit Employee Cash Drawers");
        return view;
    }
}
