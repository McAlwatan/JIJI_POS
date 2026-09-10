package com.example.jijipos.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.example.jijipos.StorageManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerScanFragment extends Fragment {

    private LinearLayout layoutScanActionsGroup;
    private TextView textScanResultHint;

    private PreviewView cameraPreviewView;
    private LinearLayout layoutScanSuccessHeader;
    private TextView textScanResultData;
    private Button buttonScanAgain, buttonScanContinue;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isScanThrottled = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLiveCameraHardwarePipeline();
                } else {
                    textScanResultData.setText("Camera permission denied. Toggle settings manually to scan.");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_scan, container, false);

        cameraPreviewView = view.findViewById(R.id.cameraPreviewView);
        layoutScanSuccessHeader = view.findViewById(R.id.layoutScanSuccessHeader);
        textScanResultData = view.findViewById(R.id.textScanResultData);
        buttonScanAgain = view.findViewById(R.id.buttonScanAgain);
        buttonScanContinue = view.findViewById(R.id.buttonScanContinue);
        layoutScanActionsGroup = view.findViewById(R.id.layoutScanActionsGroup);
        textScanResultHint = view.findViewById(R.id.textScanResultHint);

        cameraExecutor = Executors.newSingleThreadExecutor();

        buttonScanAgain.setOnClickListener(v -> resetScannerState());
        buttonScanContinue.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Processing receipt entry payload...", Toast.LENGTH_SHORT).show();
        });
        checkPermissionsAndAutoLaunch();
        return view;
    }

    private void executeBackgroundStorageMaintenance() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContext();
            if (context == null) return;

            // Enforce a strict 50 MB local storage ceiling rule safety barrier
            if (StorageManager.isStorageLimitExceeded(context, 50.0)) {
                android.util.Log.w("JIJI_STORAGE", "Local DB size exceeded 50MB! Cleaning old history...");

                // Calculate time window limit matching exactly 3 months ago (90 Days)
                long ninetyDaysInMillis = 90L * 24L * 60L * 60L * 1000L;
                long threeMonthsCutoffTimestamp = System.currentTimeMillis() - ninetyDaysInMillis;

                // Run pruning query directly via the AppDatabase engine instance
                com.example.jijipos.database.AppDatabase db = com.example.jijipos.database.AppDatabase.getInstance(context);
                int rowsDeleted = db.transactionDao().pruneOldLocalHistory(threeMonthsCutoffTimestamp);

                android.util.Log.i("JIJI_STORAGE", "Storage optimization complete. Purged " + rowsDeleted + " old local receipt records.");
            }
        });
    }

    private void checkPermissionsAndAutoLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startLiveCameraHardwarePipeline();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startLiveCameraHardwarePipeline() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraStreamingAnalysisUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraStreamingAnalysisUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // 1. Configure the visual display panel view finder viewport route
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        // 2. Configure the background computer lens frames image analysis pipeline stream loop
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Instantiate ML Kit configuration models targeting common code standard variants
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_EAN_13)
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @SuppressWarnings("UnsafeOptInUsageError")
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null && !isScanThrottled) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                String rawValue = barcode.getRawValue();
                                if (rawValue != null) {
                                    isScanThrottled = true; // Lock extraction thread to avoid multi-firing glitches
                                    handleSuccessfulBarcodeExtraction(rawValue);
                                    break;
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close()); // Essential close call to free stream memory pipeline
            } else {
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            cameraProvider.unbindAll(); // Wipe baseline attachments clean
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSuccessfulBarcodeExtraction(String rawValue) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // 1. Unpack the raw JSON string elements securely
                    org.json.JSONObject dataObj = new org.json.JSONObject(rawValue);
                    String itemName = dataObj.getString("item_name");
                    String itemPrice = dataObj.getString("item_price");
                    String location = dataObj.getString("store_location");
                    String readableDate = dataObj.getString("date_readable");
                    long timestamp = dataObj.getLong("timestamp");

                    // Parse out currency symbols to save clean double data values to the database
                    double numericalPrice = Double.parseDouble(itemPrice.replaceAll("[^0-9.]", ""));

                    // 2. Build the Custom Alert Dialog Box overlay
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_receipt_success, null);
                    builder.setView(dialogView);

                    android.app.AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    // 3. Bind parsed text data into dialog view nodes
                    TextView dName = dialogView.findViewById(R.id.dialogItemName);
                    TextView dPrice = dialogView.findViewById(R.id.dialogItemPrice);
                    TextView dLoc = dialogView.findViewById(R.id.dialogLocation);
                    TextView dDate = dialogView.findViewById(R.id.dialogDate);
                    Button btnCancel = dialogView.findViewById(R.id.dialogButtonCancel);
                    Button btnSave = dialogView.findViewById(R.id.dialogButtonSave);

                    dName.setText("🛒 Item: " + itemName);
                    dPrice.setText("Amount: " + itemPrice);
                    dLoc.setText("📍 Location: " + location);
                    dDate.setText("📅 Date: " + readableDate);

                    // 4. Set up interactive buttons workflows
                    btnCancel.setOnClickListener(v -> {
                        dialog.dismiss();
                        resetScannerState(); // Resume live scanning stream loop
                    });

                    btnSave.setOnClickListener(v -> {
                        // Create the structural receipt record explicitly linked to your Customer tracking slot
                        // Setting customerId to 99L to match your dashboard tracking filters perfectly
                        com.example.jijipos.database.entity.Transaction newReceiptRecord = new com.example.jijipos.database.entity.Transaction(
                                1L, 1L, 99L, numericalPrice, itemName, timestamp, false
                        );

                        // Initialize repository subsystem context mapping to execute database write
                        com.example.jijipos.repository.TransactionRepository repo = new com.example.jijipos.repository.TransactionRepository(requireContext());

                        repo.insertTransaction(newReceiptRecord, newId -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Receipt saved to history logs!", Toast.LENGTH_SHORT).show();

                                    // FIX: Simply dismiss the dialog window layout frames and resume scanner
                                    // DO NOT trigger any clear-task welcome intents here!
                                    dialog.dismiss();
                                    resetScannerState();

                                    // Execute your background storage ceiling optimization safely
                                    executeBackgroundStorageMaintenance();
                                });
                            }
                        });
                    });


                    dialog.show();

                } catch (org.json.JSONException | NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Invalid JIJI POS Receipt Code format!", Toast.LENGTH_LONG).show();
                    resetScannerState();
                }
            });
        }
    }


    private void resetScannerState() {
        isScanThrottled = false;
        layoutScanSuccessHeader.setVisibility(View.INVISIBLE);
        layoutScanActionsGroup.setVisibility(View.GONE);

        textScanResultHint.setVisibility(View.VISIBLE);
        textScanResultHint.setText("Position your camera well above the QR code...");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown(); // Safely terminate threaded workers pool to prevent memory leaks
    }

}
