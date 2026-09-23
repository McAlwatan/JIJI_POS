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

public class SyncQueueFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView title = view.findViewById(R.id.textPlaceholderTitle);
        title.setText("Offline Sync Queue");
        
        TextView desc = view.findViewById(android.R.id.text1); // fragment_placeholder.xml doesn't have text1, let me check it.
        // Wait, I created fragment_placeholder.xml earlier. Let me check its content.
        return view;
    }
}
