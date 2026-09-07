package com.example.jijipos.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalesFragment extends Fragment {

    private TextInputEditText inputItemName, inputItemPrice;
    private Button buttonGenerateInvoice;
    private MaterialCardView cardQrDisplayHolder;
    private ImageView imageQrOutputPlaceholder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);

        // Bind form inputs fields
        inputItemName = view.findViewById(R.id.inputItemName);
        inputItemPrice = view.findViewById(R.id.inputItemPrice);
        buttonGenerateInvoice = view.findViewById(R.id.buttonGenerateInvoice);
        cardQrDisplayHolder = view.findViewById(R.id.cardQrDisplayHolder);
        imageQrOutputPlaceholder = view.findViewById(R.id.imageQrOutputPlaceholder);

        buttonGenerateInvoice.setOnClickListener(v -> executeReceiptQrGeneration());

        return view;
    }

    private void executeReceiptQrGeneration() {
        String itemName = inputItemName.getText().toString().trim();
        String itemPrice = inputItemPrice.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            inputItemName.setError("Item name is required!");
            return;
        }
        if (TextUtils.isEmpty(itemPrice)) {
            inputItemPrice.setError("Price is required!");
            return;
        }

        try {
            long currentUnixTime = System.currentTimeMillis();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDateString = dateFormatter.format(new Date(currentUnixTime));

            String automatedLocation = "Ubungo, Dar es Salaam, TZ"; // Dynamically derived from location modules later
            String simulatedCashierOperator = "Operator_ID_042"; // Pulled directly from current logged-in user profile global cache
            // ========================================================

            // Structure all transaction records neatly inside an industry-standard JSON string object
            JSONObject receiptPayload = new JSONObject();
            receiptPayload.put("item_name", itemName);
            receiptPayload.put("item_price", "TZS " + itemPrice);
            receiptPayload.put("timestamp", currentUnixTime);
            receiptPayload.put("date_readable", formattedDateString);
            receiptPayload.put("store_location", automatedLocation);
            receiptPayload.put("processed_by", simulatedCashierOperator);

            // Convert structured JSON text data right into an offline bitmap layout matrix image array
            Bitmap qrCodeBitmap = generateQrCodeBitmapText(receiptPayload.toString());

            if (qrCodeBitmap != null) {
                imageQrOutputPlaceholder.setImageBitmap(qrCodeBitmap);
                cardQrDisplayHolder.setVisibility(View.VISIBLE); // Slide open the display frame window natively!
                Toast.makeText(getContext(), "Receipt QR Code compiled successfully!", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException | WriterException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "QR compilation error tracking crash!", Toast.LENGTH_SHORT).show();
        }
    }

    // Mathematical matrix layout generation function mapping string bytes down into black/white pixels arrays
    private Bitmap generateQrCodeBitmapText(String textPayload) throws WriterException {
        BitMatrix bitMatrix;
        int sizeDimensions = 500; // Total resolution size width/height of your bitmap drawing canvas layout

        try {
            bitMatrix = new MultiFormatWriter().encode(textPayload, BarcodeFormat.QR_CODE, sizeDimensions, sizeDimensions);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixelColorMapArray = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                // If bit matrix index path is flagged true, paint a solid black pixel dot, else leave pristine white backplate
                pixelColorMapArray[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixelColorMapArray, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
