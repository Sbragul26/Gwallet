package com.example.gwallet;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HomePage extends AppCompatActivity {

    private double balance = 0.00; // Will be loaded from database
    private TextView txtBalance;
    private TextView txtJsonResponse; // TextView for JSON response and scanned details
    private ActivityResultLauncher<Intent> scannerLauncher;
    private RequestQueue requestQueue;  // For Volley
    private DatabaseHelper databaseHelper; // Database helper

    private static final String TAG = "HomePage";
    private static final String SERVER_URL = "http://192.168.1.9:5000/payment";
    // For emulator testing, uncomment the line below and comment the above SERVER_URL
    // private static final String SERVER_URL = "http://10.0.2.2:5000/payment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize Database Helper
        databaseHelper = new DatabaseHelper(this);

        // Initialize TextViews
        txtBalance = findViewById(R.id.txtBalance);
        txtJsonResponse = findViewById(R.id.txtJsonResponse);

        // Load balance from database
        loadBalanceFromDatabase();

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Test server reachability on startup
        testServerReachability();

        // Initialize ActivityResultLauncher for Scanner activity
        scannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String amountStr = result.getData().getStringExtra("scanned_amount");
                String upiId = result.getData().getStringExtra("upi_id");
                String scannedDetails = result.getData().getStringExtra("scanned_details");

                // Display scanned details immediately as JSON
                String jsonOutput = "";
                if (scannedDetails != null && !scannedDetails.isEmpty()) {
                    jsonOutput = convertToStructuredJson(scannedDetails);
                    txtJsonResponse.setText("Scanned QR Details (JSON):\n" + jsonOutput);
                } else {
                    txtJsonResponse.setText("Error: No scanned details received");
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0 && amount <= balance) {
                        // Insert transaction as PENDING first
                        long transactionId = databaseHelper.insertTransaction(
                                amount,
                                upiId != null ? upiId : "test@example.com", // Fallback UPI ID
                                jsonOutput,
                                "PENDING"
                        );

                        // Deduct money from database
                        if (databaseHelper.deductMoney(amount)) {
                            balance = databaseHelper.getCurrentBalance(); // Refresh balance
                            updateBalanceDisplay();
                            Toast.makeText(this, "Payment of Rs " + String.format("%.2f", amount) + " successful", Toast.LENGTH_SHORT).show();

                            // Send payment to server
                            if (isNetworkAvailable()) {
                                sendPaymentToServer(amount, upiId, scannedDetails, transactionId);
                            } else {
                                databaseHelper.updateTransactionResponse(transactionId, "No network connection", "FAILED_NO_NETWORK");
                                txtJsonResponse.setText("Error: No network connection\n\nScanned Details (JSON):\n" + jsonOutput);
                                Toast.makeText(this, "No network connection", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Rollback transaction if balance deduction fails
                            databaseHelper.updateTransactionResponse(transactionId, "Balance deduction failed", "FAILED");
                            txtJsonResponse.setText("Error: Failed to process payment\n\nScanned Details (JSON):\n" + jsonOutput);
                            Toast.makeText(this, "Failed to process payment", Toast.LENGTH_LONG).show();
                        }

                    } else if (amount > balance) {
                        // Insert failed transaction
                        databaseHelper.insertTransaction(
                                amount,
                                upiId != null ? upiId : "test@example.com",
                                jsonOutput,
                                "FAILED_INSUFFICIENT_BALANCE"
                        );

                        txtJsonResponse.setText("Error: Insufficient balance\n\nScanned Details (JSON):\n" + jsonOutput);
                        Toast.makeText(this, "Insufficient balance", Toast.LENGTH_LONG).show();
                    } else {
                        // Insert failed transaction
                        databaseHelper.insertTransaction(
                                amount,
                                upiId != null ? upiId : "test@example.com",
                                jsonOutput,
                                "FAILED_INVALID_AMOUNT"
                        );

                        txtJsonResponse.setText("Error: Invalid amount scanned\n\nScanned Details (JSON):\n" + jsonOutput);
                        Toast.makeText(this, "Invalid amount scanned", Toast.LENGTH_LONG).show();
                    }
                } catch (NumberFormatException e) {
                    // Insert failed transaction
                    databaseHelper.insertTransaction(
                            0.0,
                            upiId != null ? upiId : "test@example.com",
                            jsonOutput,
                            "FAILED_INVALID_FORMAT"
                    );

                    txtJsonResponse.setText("Error: Invalid amount format\n\nScanned Details (JSON):\n" + jsonOutput);
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_LONG).show();
                }
            } else {
                txtJsonResponse.setText("Error: No valid scan data received");
                Toast.makeText(this, "No valid scan data received", Toast.LENGTH_LONG).show();
            }
        });

        // Initialize Add Money Button
        Button btnAddMoney = findViewById(R.id.btnAddMoney);
        btnAddMoney.setOnClickListener(v -> {
            double addAmount = 1000.00;
            if (databaseHelper.addMoney(addAmount)) {
                balance = databaseHelper.getCurrentBalance(); // Refresh balance
                updateBalanceDisplay();
                txtJsonResponse.setText("Added Rs " + String.format("%.2f", addAmount) + " to balance");
                Toast.makeText(this, "Rs " + String.format("%.2f", addAmount) + " added successfully", Toast.LENGTH_SHORT).show();

                // Log the add money transaction
                try {
                    JSONObject addMoneyJson = new JSONObject();
                    addMoneyJson.put("type", "ADD_MONEY");
                    addMoneyJson.put("amount", addAmount);
                    addMoneyJson.put("new_balance", balance);

                    databaseHelper.insertTransaction(
                            addAmount,
                            "SYSTEM",
                            addMoneyJson.toString(2),
                            "SUCCESS"
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Error logging add money transaction: " + e.getMessage());
                }
            } else {
                txtJsonResponse.setText("Failed to add money to balance");
                Toast.makeText(this, "Failed to add money", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Scan QR Button
        Button btnScanQR = findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, Scanner.class);
            scannerLauncher.launch(intent);
        });

        // Initialize View Transactions Button (optional)
        Button btnViewTransactions = findViewById(R.id.btnViewTransactions);
        if (btnViewTransactions != null) {
            btnViewTransactions.setOnClickListener(v -> {
                showRecentTransactions();
            });
        }

        // Initialize SearchView
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTransactions(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    searchTransactions(newText);
                } else if (newText.isEmpty()) {
                    txtJsonResponse.setText("Enter search term to find transactions");
                }
                return true;
            }
        });

        searchView.setOnSearchClickListener(v -> {});
        searchView.setOnCloseListener(() -> {
            txtJsonResponse.setText("Search closed");
            return false;
        });

        // Run test for JSON conversion (for debugging purposes)
        testJsonConversion();
    }

    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Network available: " + isConnected);
        return isConnected;
    }

    // Test server reachability
    private void testServerReachability() {
        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName("192.168.1.9");
                boolean reachable = address.isReachable(5000); // 5-second timeout
                runOnUiThread(() -> {
                    if (reachable) {
                        Log.d(TAG, "Server at 192.168.1.9 is reachable");
                        Toast.makeText(this, "Server is reachable", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Server at 192.168.1.9 is NOT reachable");
                        Toast.makeText(this, "Cannot reach server", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error checking server reachability: " + e.getMessage());
                    Toast.makeText(this, "Error checking server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Load balance from database on app start
    private void loadBalanceFromDatabase() {
        balance = databaseHelper.getCurrentBalance();
        updateBalanceDisplay();
        Log.d(TAG, "Balance loaded from database: " + balance);
    }

    // Enhanced method to convert scanned QR details to structured JSON
    private String convertToStructuredJson(String scannedDetails) {
        try {
            JSONObject jsonObject = new JSONObject();
            String[] lines = scannedDetails.split("\n");
            JSONArray productsArray = new JSONArray();

            boolean inItemsSection = false;

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("UPI ID:")) {
                    jsonObject.put("upiId", line.replace("UPI ID:", "").trim());
                }
                else if (line.startsWith("Payee Name:")) {
                    jsonObject.put("payeeName", line.replace("Payee Name:", "").trim());
                }
                else if (line.startsWith("Amount:")) {
                    try {
                        double amount = Double.parseDouble(line.replace("Amount:", "").trim());
                        jsonObject.put("total_amount", amount);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing amount: " + e.getMessage());
                        jsonObject.put("total_amount", 0.0);
                    }
                }
                else if (line.equals("Items:")) {
                    inItemsSection = true;
                }
                else if (inItemsSection && line.startsWith("-")) {
                    String item = line.replace("-", "").trim();

                    // Skip if it's just the total amount repeated
                    try {
                        Double.parseDouble(item);
                        continue; // Skip this line as it's likely the total amount
                    } catch (NumberFormatException e) {
                        // Not a number, so it's a product line
                    }

                    if (!item.isEmpty()) {
                        JSONObject product = parseProductLine(item);
                        if (product != null) {
                            productsArray.put(product);
                        }
                    }
                }
            }

            jsonObject.put("products", productsArray);
            return jsonObject.toString(2); // Pretty print with indent

        } catch (Exception e) {
            Log.e(TAG, "Error converting to structured JSON: " + e.getMessage());
            return "Error converting to JSON: " + e.getMessage();
        }
    }

    // Helper method to parse individual product lines
    private JSONObject parseProductLine(String productLine) {
        try {
            JSONObject product = new JSONObject();

            // Pattern 1: "BillWT2025092263525: 1x BluetoothHeadphones"
            // Pattern 2: "1x PhoneCase"
            // Pattern 3: "ProductName - Price" or "ProductName: Price"

            if (productLine.contains(":")) {
                // Handle format like "BillWT2025092263525: 1x BluetoothHeadphones"
                String[] parts = productLine.split(":", 2);
                if (parts.length >= 2) {
                    String rightPart = parts[1].trim();

                    // Extract quantity and product name
                    if (rightPart.matches("\\d+x\\s+.*")) {
                        // Format: "1x BluetoothHeadphones"
                        String[] quantityAndName = rightPart.split("x\\s+", 2);
                        if (quantityAndName.length >= 2) {
                            String quantity = quantityAndName[0].trim();
                            String productName = quantityAndName[1].trim();

                            product.put("name", quantity + "x " + productName);
                            product.put("price", JSONObject.NULL); // No price specified
                        } else {
                            product.put("name", rightPart);
                            product.put("price", JSONObject.NULL);
                        }
                    } else {
                        product.put("name", rightPart);
                        product.put("price", JSONObject.NULL);
                    }
                }
            }
            else if (productLine.matches("\\d+x\\s+.*")) {
                // Handle format like "1x PhoneCase"
                String[] quantityAndName = productLine.split("x\\s+", 2);
                if (quantityAndName.length >= 2) {
                    String quantity = quantityAndName[0].trim();
                    String productName = quantityAndName[1].trim();

                    product.put("name", quantity + "x " + productName);
                    product.put("price", JSONObject.NULL);
                } else {
                    product.put("name", productLine);
                    product.put("price", JSONObject.NULL);
                }
            }
            else if (productLine.contains(" - ")) {
                // Handle format like "ProductName - 150.00"
                String[] parts = productLine.split(" - ");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String priceStr = parts[parts.length - 1].trim();

                    try {
                        double price = Double.parseDouble(priceStr);
                        product.put("name", name);
                        product.put("price", price);
                    } catch (NumberFormatException e) {
                        product.put("name", productLine);
                        product.put("price", JSONObject.NULL);
                    }
                } else {
                    product.put("name", productLine);
                    product.put("price", JSONObject.NULL);
                }
            }
            else {
                // Default: treat as product name without price
                product.put("name", productLine);
                product.put("price", JSONObject.NULL);
            }

            return product;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing product line '" + productLine + "': " + e.getMessage());

            // Return a basic product object as fallback
            try {
                JSONObject fallback = new JSONObject();
                fallback.put("name", productLine);
                fallback.put("price", JSONObject.NULL);
                return fallback;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // Test method to verify the conversion with sample data
    private void testJsonConversion() {
        String sampleInput = "Scanned QR Details:\n" +
                "UPI ID: sbragul26@okicici\n" +
                "Payee Name: WalletTracker\n" +
                "Amount: 354.00\n" +
                "Items:\n" +
                "- BillWT2025092263525: 1x BluetoothHeadphones\n" +
                "- 1x PhoneCase\n" +
                "- 354.00";

        String result = convertToStructuredJson(sampleInput);
        Log.d(TAG, "Converted JSON: " + result);
        txtJsonResponse.setText("Converted JSON:\n" + result);
    }

    // Helper method to send payment details to server
    private void sendPaymentToServer(double amount, String upiId, String scannedDetails, long transactionId) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("amount", amount);
            String effectiveUpiId = (upiId != null && !upiId.isEmpty() && !upiId.equals("unknown")) ? upiId : "test@example.com";
            jsonBody.put("upi_id", effectiveUpiId);

            // Log the request payload
            Log.d(TAG, "Sending payment request to " + SERVER_URL + ": " + jsonBody.toString(2));

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    SERVER_URL,
                    jsonBody,
                    response -> {
                        // Success: Update transaction with server response
                        try {
                            String formattedJson = response.toString(2);
                            String scannedJson = convertToStructuredJson(scannedDetails);

                            // Update transaction in database
                            databaseHelper.updateTransactionResponse(transactionId, formattedJson, "SUCCESS");

                            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + scannedJson + "\n\nServer Response:\n" + formattedJson);
                            Toast.makeText(this, "Payment confirmed on server!", Toast.LENGTH_SHORT).show();

                            Log.d(TAG, "Payment successful, transaction ID: " + transactionId + ", Response: " + formattedJson);
                        } catch (Exception e) {
                            String errorMsg = "Error formatting JSON: " + e.getMessage();
                            databaseHelper.updateTransactionResponse(transactionId, errorMsg, "SUCCESS_JSON_ERROR");
                            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + convertToStructuredJson(scannedDetails) + "\n\n" + errorMsg);
                            Toast.makeText(this, "Payment sent but response error", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "JSON formatting error: " + e.getMessage());
                        }
                    },
                    error -> {
                        // Error: Update transaction with error response
                        StringBuilder errorMsg = new StringBuilder("Failed to notify server: " +
                                (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                        if (error.networkResponse != null) {
                            errorMsg.append("\nStatus Code: ").append(error.networkResponse.statusCode);
                            try {
                                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                                errorMsg.append("\nResponse: ").append(responseBody);
                            } catch (Exception e) {
                                errorMsg.append("\nNo response body");
                            }
                            // Log headers
                            if (error.networkResponse.headers != null) {
                                errorMsg.append("\nHeaders: ").append(error.networkResponse.headers.toString());
                            }
                        }
                        databaseHelper.updateTransactionResponse(transactionId, errorMsg.toString(), "SERVER_ERROR");
                        txtJsonResponse.setText("Scanned QR Details (JSON):\n" + convertToStructuredJson(scannedDetails) + "\n\n" + errorMsg);
                        Toast.makeText(this, "Payment processed but server error", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Server error for transaction " + transactionId + ": " + errorMsg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };

            // Set retry policy
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000, // 30-second timeout
                    3,     // Number of retries
                    1.5f   // Backoff multiplier
            ));

            requestQueue.add(request);
        } catch (Exception e) {
            String errorMsg = "Error preparing payment data: " + e.getMessage();
            databaseHelper.updateTransactionResponse(transactionId, errorMsg, "PREPARATION_ERROR");
            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + convertToStructuredJson(scannedDetails) + "\n\n" + errorMsg);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Payment preparation error: " + e.getMessage());
        }
    }

    // Helper method to update balance display
    private void updateBalanceDisplay() {
        txtBalance.setText(String.format("Rs %.2f", balance));
    }

    // Show recent transactions
    private void showRecentTransactions() {
        try {
            var recentTransactions = databaseHelper.getRecentTransactions(10);
            if (recentTransactions.isEmpty()) {
                txtJsonResponse.setText("No transactions found");
                return;
            }

            StringBuilder transactionsList = new StringBuilder();
            transactionsList.append("Recent Transactions:\n\n");

            for (var transaction : recentTransactions) {
                transactionsList.append("ID: ").append(transaction.id).append("\n");
                transactionsList.append("Amount: Rs ").append(String.format("%.2f", transaction.amount)).append("\n");
                transactionsList.append("Status: ").append(transaction.status).append("\n");
                transactionsList.append("Time: ").append(transaction.timestamp).append("\n");
                if (transaction.upiId != null && !transaction.upiId.equals("SYSTEM")) {
                    transactionsList.append("UPI ID: ").append(transaction.upiId).append("\n");
                }
                transactionsList.append("-------------------\n");
            }

            txtJsonResponse.setText(transactionsList.toString());
        } catch (Exception e) {
            txtJsonResponse.setText("Error loading transactions: " + e.getMessage());
            Log.e(TAG, "Error loading transactions: " + e.getMessage());
        }
    }

    // Search transactions
    private void searchTransactions(String query) {
        try {
            var allTransactions = databaseHelper.getAllTransactions();
            var filteredTransactions = new java.util.ArrayList<DatabaseHelper.TransactionData>();

            String lowerQuery = query.toLowerCase();
            for (var transaction : allTransactions) {
                if ((transaction.upiId != null && transaction.upiId.toLowerCase().contains(lowerQuery)) ||
                        transaction.status.toLowerCase().contains(lowerQuery) ||
                        String.valueOf(transaction.amount).contains(query) ||
                        (transaction.jsonData != null && transaction.jsonData.toLowerCase().contains(lowerQuery))) {
                    filteredTransactions.add(transaction);
                }
            }

            if (filteredTransactions.isEmpty()) {
                txtJsonResponse.setText("No transactions found matching: " + query);
                return;
            }

            StringBuilder searchResults = new StringBuilder();
            searchResults.append("Search results for '").append(query).append("':\n\n");

            for (var transaction : filteredTransactions) {
                searchResults.append("ID: ").append(transaction.id).append("\n");
                searchResults.append("Amount: Rs ").append(String.format("%.2f", transaction.amount)).append("\n");
                searchResults.append("Status: ").append(transaction.status).append("\n");
                searchResults.append("Time: ").append(transaction.timestamp).append("\n");
                if (transaction.upiId != null && !transaction.upiId.equals("SYSTEM")) {
                    searchResults.append("UPI ID: ").append(transaction.upiId).append("\n");
                }
                searchResults.append("-------------------\n");
            }

            txtJsonResponse.setText(searchResults.toString());
        } catch (Exception e) {
            txtJsonResponse.setText("Error searching transactions: " + e.getMessage());
            Log.e(TAG, "Error searching transactions: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh balance when activity resumes
        loadBalanceFromDatabase();
        // Retest server reachability
        testServerReachability();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close database helper
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}