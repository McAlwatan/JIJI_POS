package com.example.jijipos.fragments;

import android.Manifest;
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

        // Configure footer action clicks
        buttonScanAgain.setOnClickListener(v -> resetScannerState());
        buttonScanContinue.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Processing receipt entry payload...", Toast.LENGTH_SHORT).show();
        });

        // AUTOMATIC INSTANT LAUNCH INVOCATION SEQUENCE
        checkPermissionsAndAutoLaunch();

        return view;
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
                // Instantly update presentation frames directly matching your UI requirements layout
                layoutScanSuccessHeader.setVisibility(View.VISIBLE);
                textScanResultHint.setVisibility(View.GONE);
                textScanResultData.setText("Decoded Receipt Payload:\n\n" + rawValue);
                layoutScanActionsGroup.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Scan successful!", Toast.LENGTH_SHORT).show();
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
