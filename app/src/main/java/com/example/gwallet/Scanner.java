package com.example.gwallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class Scanner extends AppCompatActivity {
    private static final String LOGTAG = "ScannerExample";
    private static final int REQUEST_CODE_PERMISSIONS = 102;

    private Button scanBtn;
    private TextView resultText;
    private GmsBarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner);

        // Initialize UI components
        scanBtn = findViewById(R.id.scanBtn);
        resultText = findViewById(R.id.resultText);

        // Configure ML Kit Barcode Scanner
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = GmsBarcodeScanning.getClient(this, options);

        // Set click listener for scan button
        scanBtn.setOnClickListener(v -> {
            // Check for camera permission before scanning
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                requestCameraPermission();
            }
        });
    }

    private void requestCameraPermission() {
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startScanning();
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                    }
                });
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startScanning() {
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String result = barcode.getRawValue();
                    if (result != null) {
                        Log.d(LOGTAG, "Have scan result: " + result);
                        String formattedResult = parseQRCodeContent(result);
                        resultText.setText(formattedResult);
                        Toast.makeText(this, "Scan successful", Toast.LENGTH_SHORT).show();
                    } else {
                        resultText.setText("No QR code data found");
                        Toast.makeText(this, "No QR code data found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnCanceledListener(() -> {
                    Log.d(LOGTAG, "Scan cancelled");
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOGTAG, "Scan failed: " + e.getMessage());
                    Toast.makeText(this, "QR Code could not be scanned: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String parseQRCodeContent(String result) {
        // Check for common QR code formats and parse accordingly
        try {
            // UPI Payment
            if (result.startsWith("upi://")) {
                Uri uri = Uri.parse(result);
                String upiId = uri.getQueryParameter("pa"); // Payee Address (UPI ID)
                String payeeName = uri.getQueryParameter("pn"); // Payee Name
                String amount = uri.getQueryParameter("am"); // Amount
                String transactionNote = uri.getQueryParameter("tn"); // Transaction Note
                StringBuilder upiDetails = new StringBuilder();
                if (upiId != null) upiDetails.append("UPI ID: ").append(upiId).append("\n");
                if (payeeName != null) upiDetails.append("Payee Name: ").append(payeeName).append("\n");
                if (amount != null) upiDetails.append("Amount: ").append(amount).append("\n");
                if (transactionNote != null) {
                    upiDetails.append("Items:\n");
                    String[] items = transactionNote.split(",");
                    for (String item : items) {
                        if (!item.trim().isEmpty()) {
                            upiDetails.append("  - ").append(item.trim().replace(":", ": ").replace("1x", "1x ")).append("\n");
                        }
                    }
                }
                return upiDetails.toString();
            }
            // URL
            else if (result.startsWith("http://") || result.startsWith("https://")) {
                return "URL: " + result + "\n\nOpen in browser?";
            }
            // Wi-Fi
            else if (result.startsWith("WIFI:")) {
                String ssid = "";
                String password = "";
                String type = "";
                String[] parts = result.split(";");
                for (String part : parts) {
                    if (part.startsWith("S:")) {
                        ssid = part.substring(2);
                    } else if (part.startsWith("P:")) {
                        password = part.substring(2);
                    } else if (part.startsWith("T:")) {
                        type = part.substring(2);
                    }
                }
                return "Wi-Fi Network\nSSID: " + ssid + "\nPassword: " + password + "\nType: " + type;
            }
            // Contact (vCard or MECARD)
            else if (result.startsWith("MECARD:") || result.startsWith("BEGIN:VCARD")) {
                String formattedContact = parseContact(result);
                return "Contact Details\n" + formattedContact;
            }
            // Email
            else if (result.startsWith("mailto:")) {
                Uri uri = Uri.parse(result);
                String email = uri.getSchemeSpecificPart();
                return "Email: " + email;
            }
            // Phone number
            else if (result.startsWith("tel:")) {
                String phone = result.substring(4);
                return "Phone Number: " + phone;
            }
            // SMS
            else if (result.startsWith("sms:")) {
                String[] smsParts = result.substring(4).split("\\?");
                String phone = smsParts[0];
                String message = "";
                if (smsParts.length > 1) {
                    message = smsParts[1].replace("body=", "");
                }
                return "SMS\nTo: " + phone + "\nMessage: " + message;
            }
            // Geo location
            else if (result.startsWith("geo:")) {
                String[] geoParts = result.substring(4).split(",");
                String latitude = geoParts[0];
                String longitude = geoParts.length > 1 ? geoParts[1] : "";
                return "Location\nLatitude: " + latitude + "\nLongitude: " + longitude;
            }
            // Plain text or unrecognized format
            else {
                return "Text: " + result;
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Error parsing QR code content: " + e.getMessage());
            return "Text: " + result;
        }
    }

    private String parseContact(String result) {
        StringBuilder contactInfo = new StringBuilder();
        if (result.startsWith("MECARD:")) {
            String[] parts = result.split(";");
            for (String part : parts) {
                if (part.startsWith("N:")) {
                    contactInfo.append("Name: ").append(part.substring(2)).append("\n");
                } else if (part.startsWith("TEL:")) {
                    contactInfo.append("Phone: ").append(part.substring(4)).append("\n");
                } else if (part.startsWith("EMAIL:")) {
                    contactInfo.append("Email: ").append(part.substring(6)).append("\n");
                } else if (part.startsWith("ADR:")) {
                    contactInfo.append("Address: ").append(part.substring(4)).append("\n");
                } else if (part.startsWith("ORG:")) {
                    contactInfo.append("Organization: ").append(part.substring(4)).append("\n");
                }
            }
        } else if (result.startsWith("BEGIN:VCARD")) {
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.startsWith("N:")) {
                    contactInfo.append("Name: ").append(line.substring(2).replace(";", " ")).append("\n");
                } else if (line.startsWith("TEL")) {
                    contactInfo.append("Phone: ").append(line.substring(line.indexOf(":") + 1)).append("\n");
                } else if (line.startsWith("EMAIL")) {
                    contactInfo.append("Email: ").append(line.substring(line.indexOf(":") + 1)).append("\n");
                } else if (line.startsWith("ADR")) {
                    contactInfo.append("Address: ").append(line.substring(line.indexOf(":") + 1).replace(";", " ")).append("\n");
                } else if (line.startsWith("ORG:")) {
                    contactInfo.append("Organization: ").append(line.substring(4)).append("\n");
                }
            }
        }
        return contactInfo.length() > 0 ? contactInfo.toString() : "No contact details found";
    }
}