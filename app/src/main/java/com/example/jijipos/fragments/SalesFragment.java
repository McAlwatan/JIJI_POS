package com.example.jijipos.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.entity.Transaction;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SalesFragment extends Fragment {

    private TextInputEditText inputItemName, inputItemPrice;
    private Button buttonGenerateInvoice;
    private MaterialCardView cardQrDisplayHolder;
    private ImageView imageQrOutputPlaceholder;

    // Location Tracking Components
    private FusedLocationProviderClient fusedLocationClient;
    private String resolvedLocationAddress = "Unknown Location, TZ";

    // Permission request launcher loop
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (selectedPermissionGranted()) {
                    fetchRealDeviceLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false); //

        inputItemName = view.findViewById(R.id.inputItemName); //
        inputItemPrice = view.findViewById(R.id.inputItemPrice); //
        buttonGenerateInvoice = view.findViewById(R.id.buttonGenerateInvoice); //
        cardQrDisplayHolder = view.findViewById(R.id.cardQrDisplayHolder); //
        imageQrOutputPlaceholder = view.findViewById(R.id.imageQrOutputPlaceholder); //

        // Initialize location services client provider natively
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Check and trigger real-time location lookups early on boot setup
        if (checkLocationPermissions()) {
            fetchRealDeviceLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        buttonGenerateInvoice.setOnClickListener(v -> executeReceiptQrGeneration()); //

        return view;
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean selectedPermissionGranted() {
        return checkLocationPermissions();
    }

    private void fetchRealDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    // Offload heavy reverse geocoding to background lanes to protect UI threads
                    Executors.newSingleThreadExecutor().execute(() -> {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                // Compile clear readable text string matching local environment
                                String locality = address.getLocality() != null ? address.getLocality() : "Dar es Salaam";
                                String featureName = address.getFeatureName() != null ? address.getFeatureName() : "";
                                resolvedLocationAddress = featureName + ", " + locality + ", TZ";
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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

        double totalAmount = Double.parseDouble(itemPrice);
        long currentUnixTime = System.currentTimeMillis();

        // Unpack real session variables securely passed from active DashboardActivity session
        Intent intent = requireActivity().getIntent();
        long activeCashierId = intent.getLongExtra("USER_ID", 1L); //
        long activeBusinessId = intent.getLongExtra("BUSINESS_ID", 1L); //
        String cashierName = intent.getStringExtra("USER_NAME"); //
        if (cashierName == null) cashierName = "Operator";
        final String finalCashierName = cashierName;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            // Build the local duplicate transaction tracking token explicitly linked to this specific Cashier
            Transaction salesRecord = new Transaction(
                    activeBusinessId, activeCashierId, 0L, totalAmount, itemName, currentUnixTime, false //
            );

            db.transactionDao().insertTransaction(salesRecord); // Persist natively to update personal totals ledger

            // Return to UI thread to compile and pop open the checkout QR barcode image
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //
                        String formattedDateString = dateFormatter.format(new Date(currentUnixTime)); //

                        // Structure JSON payload string explicitly matching Customer Scanner layout expectations
                        JSONObject receiptPayload = new JSONObject(); //
                        receiptPayload.put("item_name", itemName); //
                        receiptPayload.put("item_price", "TZS " + String.format(Locale.getDefault(), "%,.0f", totalAmount));
                        receiptPayload.put("timestamp", currentUnixTime); //
                        receiptPayload.put("date_readable", formattedDateString); //
                        receiptPayload.put("store_location", resolvedLocationAddress); // Integrated real location address string
                        receiptPayload.put("processed_by", finalCashierName); //

                        Bitmap qrCodeBitmap = generateQrCodeBitmapText(receiptPayload.toString()); //

                        if (qrCodeBitmap != null) { //
                            imageQrOutputPlaceholder.setImageBitmap(qrCodeBitmap); //
                            cardQrDisplayHolder.setVisibility(View.VISIBLE); //
                            Toast.makeText(getContext(), "Receipt copy saved & QR Code compiled!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException | WriterException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "QR compilation error!", Toast.LENGTH_SHORT).show(); //
                    }
                });
            }
        });
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
