package com.example.jijipos.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.jijipos.R;

public class HomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        TextView title = view.findViewById(R.id.textFragmentTitle);
        title.setText("Core Metrics Feed\\n\\n• Real-Time Sales Volume\\n• Transaction Activity Status");
        return  view;
    }
}
