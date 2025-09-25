package com.example.gwallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class Scanner extends AppCompatActivity {
    private static final String LOGTAG = "ScannerExample";
    private static final int REQUEST_CODE_PERMISSIONS = 102;

    private TextView resultText;
    private GmsBarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner);

        // Initialize UI components
        resultText = findViewById(R.id.resultText);

        // Configure ML Kit Barcode Scanner
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = GmsBarcodeScanning.getClient(this, options);

        // Start scanning directly
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startScanning();
                    } else {
                        resultText.setText("Camera permission denied");
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startScanning() {
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawResult = barcode.getRawValue();
                    if (rawResult != null) {
                        Log.d(LOGTAG, "Have scan result: " + rawResult);
                        String formattedResult = parseQRCodeContent(rawResult);
                        resultText.setText(formattedResult);
                        Toast.makeText(this, "Scan successful", Toast.LENGTH_SHORT).show();

                        // Extract amount and UPI ID, pass back to HomePage
                        if (rawResult.startsWith("upi://")) {
                            Uri uri = Uri.parse(rawResult);
                            String amount = uri.getQueryParameter("am");
                            String upiId = uri.getQueryParameter("pa");
                            Intent resultIntent = new Intent();
                            if (amount != null && !amount.isEmpty()) {
                                resultIntent.putExtra("scanned_amount", amount);
                                resultIntent.putExtra("upi_id", upiId != null ? upiId : "");
                                resultIntent.putExtra("scanned_details", formattedResult); // Pass full parsed details
                                setResult(RESULT_OK, resultIntent);
                            } else {
                                resultText.setText("Error: No amount found in QR code\n\n" + formattedResult);
                                Toast.makeText(this, "No amount found in QR code", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            resultText.setText("Error: Scanned QR code is not a UPI payment code\n\n" + formattedResult);
                            Toast.makeText(this, "Invalid QR code for payment", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    } else {
                        resultText.setText("Error: No QR code data found");
                        Toast.makeText(this, "No QR code data found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnCanceledListener(() -> {
                    Log.d(LOGTAG, "Scan cancelled");
                    resultText.setText("Scan cancelled");
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOGTAG, "Scan failed: " + e.getMessage());
                    String errorMsg = "QR Code could not be scanned: " + e.getMessage();
                    resultText.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private String parseQRCodeContent(String result) {
        StringBuilder resultBuilder = new StringBuilder();
        try {
            if (result.startsWith("upi://")) {
                Uri uri = Uri.parse(result);
                String upiId = uri.getQueryParameter("pa");
                String payeeName = uri.getQueryParameter("pn");
                String amount = uri.getQueryParameter("am");
                String transactionNote = uri.getQueryParameter("tn");
                if (upiId != null) resultBuilder.append("UPI ID: ").append(upiId).append("\n");
                if (payeeName != null) resultBuilder.append("Payee Name: ").append(payeeName).append("\n");
                if (amount != null) resultBuilder.append("Amount: ").append(amount).append("\n");
                if (transactionNote != null) {
                    resultBuilder.append("Items:\n");
                    String[] items = transactionNote.split(",");
                    for (String item : items) {
                        if (!item.trim().isEmpty()) {
                            resultBuilder.append("  - ").append(item.trim().replace(":", ": ").replace("1x", "1x ")).append("\n");
                        }
                    }
                }
                return resultBuilder.toString();
            } else {
                return "Invalid QR code: Not a UPI payment code";
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Error parsing QR code content: " + e.getMessage());
            return "Error parsing QR code: " + e.getMessage();
        }
    }
}