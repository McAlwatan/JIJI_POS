package com.example.jijipos.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.example.jijipos.UiAnim;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * Split Bill utility (flow module M11). Replaces the old placeholder with a
 * real even-split calculator: bill total, optional tip and a people stepper
 * produce a live per-person figure plus a shareable summary.
 */
public class SplitBillFragment extends Fragment {

    private static final int MAX_PEOPLE = 30;

    private TextInputEditText inputTotal;
    private TextInputEditText inputTip;
    private TextView textPeople;
    private TextView textPerPerson;
    private TextView textBreakdown;

    private int people = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_split_bill, container, false);

        inputTotal = view.findViewById(R.id.inputSplitTotal);
        inputTip = view.findViewById(R.id.inputSplitTip);
        textPeople = view.findViewById(R.id.textSplitPeople);
        textPerPerson = view.findViewById(R.id.textSplitPerPerson);
        textBreakdown = view.findViewById(R.id.textSplitBreakdown);

        TextView btnMinus = view.findViewById(R.id.btnSplitMinus);
        TextView btnPlus = view.findViewById(R.id.btnSplitPlus);
        MaterialButton btnShare = view.findViewById(R.id.btnSplitShare);

        textPeople.setText(String.valueOf(people));

        btnMinus.setOnClickListener(v -> {
            if (people > 1) {
                people--;
                textPeople.setText(String.valueOf(people));
                recompute();
            }
        });
        btnPlus.setOnClickListener(v -> {
            if (people < MAX_PEOPLE) {
                people++;
                textPeople.setText(String.valueOf(people));
                recompute();
            }
        });

        TextWatcher watcher = new SimpleTextWatcher(this::recompute);
        inputTotal.addTextChangedListener(watcher);
        inputTip.addTextChangedListener(watcher);

        btnShare.setOnClickListener(v -> shareSplit());

        UiAnim.addPressScale(btnMinus);
        UiAnim.addPressScale(btnPlus);

        recompute();
        return view;
    }

    private void recompute() {
        double total = parse(inputTotal.getText());
        double tipPercent = parse(inputTip.getText());

        if (total <= 0) {
            textPerPerson.setText("0 TZS");
            textBreakdown.setText("Enter a bill total to begin");
            return;
        }

        double tipAmount = total * (tipPercent / 100.0);
        double grand = total + tipAmount;
        double perPerson = grand / Math.max(1, people);

        textPerPerson.setText(String.format(Locale.US, "%,.0f TZS", perPerson));
        textBreakdown.setText(String.format(Locale.US,
                "Bill  %,.0f TZS\nTip (%s%%)  %,.0f TZS\nTotal  %,.0f TZS\nSplit %d ways  %,.0f TZS each",
                total, trimNumber(tipPercent), tipAmount, grand, people, perPerson));
    }

    private void shareSplit() {
        double total = parse(inputTotal.getText());
        if (total <= 0) {
            Toast.makeText(getContext(), "Enter a bill total first", Toast.LENGTH_SHORT).show();
            return;
        }
        double tipPercent = parse(inputTip.getText());
        double tipAmount = total * (tipPercent / 100.0);
        double grand = total + tipAmount;
        double perPerson = grand / Math.max(1, people);

        String message = String.format(Locale.US,
                "JIJI POS - Split Bill\nBill: %,.0f TZS\nTip (%s%%): %,.0f TZS\nTotal: %,.0f TZS\nPeople: %d\nEach pays: %,.0f TZS",
                total, trimNumber(tipPercent), tipAmount, grand, people, perPerson);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(share, "Share split"));
    }

    private double parse(@Nullable Editable text) {
        if (text == null) return 0;
        String value = text.toString().trim();
        if (TextUtils.isEmpty(value)) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    /** Minimal TextWatcher so we only implement the changed callback. */
    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChanged;
        SimpleTextWatcher(Runnable onChanged) { this.onChanged = onChanged; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { onChanged.run(); }
    }
}
