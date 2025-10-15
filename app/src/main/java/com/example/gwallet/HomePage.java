package com.example.gwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
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
    private CardView balanceCard;
    private Button btnAddMoney, btnScanQR, btnViewTransactions;
    private LinearLayout buttonContainer;

    private static final String TAG = "HomePage";
    //private static final String SERVER_URL = "http://192.168.1.9:5000/payment";
    private static final String SERVER_URL = "http://10.133.207.115:5000/payment";

    // For emulator testing, uncomment the line below and comment the above SERVER_URL
    // private static final String SERVER_URL = "http://10.0.2.2:5000/payment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the main ConstraintLayout with gradient background
        ConstraintLayout constraintLayout = new ConstraintLayout(this);
        constraintLayout.setId(View.generateViewId());

        // Create gradient background
        GradientDrawable gradientBackground = new GradientDrawable();
        gradientBackground.setShape(GradientDrawable.RECTANGLE);
        gradientBackground.setColors(new int[]{
                Color.parseColor("#667eea"), // Light blue
                Color.parseColor("#764ba2")  // Purple
        });
        gradientBackground.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        constraintLayout.setBackground(gradientBackground);

        // Create Status Bar Spacer (for modern look)
        View statusBarSpacer = new View(this);
        statusBarSpacer.setId(View.generateViewId());
        ConstraintLayout.LayoutParams spacerParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
        );
        statusBarSpacer.setLayoutParams(spacerParams);
        constraintLayout.addView(statusBarSpacer);

        // Create Modern Header Layout
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setId(View.generateViewId());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        ConstraintLayout.LayoutParams headerParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerParams);
        constraintLayout.addView(headerLayout);

        // App Logo/Icon (simulated with colored circle)
        View appIcon = new View(this);
        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setShape(GradientDrawable.OVAL);
        iconBackground.setColor(Color.WHITE);
        appIcon.setBackground(iconBackground);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        iconParams.setMargins(0, 0, dpToPx(16), 0);
        appIcon.setLayoutParams(iconParams);
        headerLayout.addView(appIcon);

        // App Title
        TextView appTitle = new TextView(this);
        appTitle.setText("G-Wallet");
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        appTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        appTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        appTitle.setLayoutParams(titleParams);
        headerLayout.addView(appTitle);

        // Profile Icon (simulated)
        View profileIcon = new View(this);
        GradientDrawable profileBackground = new GradientDrawable();
        profileBackground.setShape(GradientDrawable.OVAL);
        profileBackground.setColor(Color.parseColor("#FFFFFF40"));
        profileIcon.setBackground(profileBackground);
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        profileIcon.setLayoutParams(profileParams);
        headerLayout.addView(profileIcon);

        // Create Enhanced Balance Card
        balanceCard = new CardView(this);
        balanceCard.setId(View.generateViewId());
        balanceCard.setRadius(dpToPx(20));
        balanceCard.setCardElevation(dpToPx(8));
        balanceCard.setCardBackgroundColor(Color.WHITE);
        ConstraintLayout.LayoutParams balanceCardParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        balanceCardParams.setMargins(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(16));
        balanceCard.setLayoutParams(balanceCardParams);
        constraintLayout.addView(balanceCard);

        // Balance Card Content
        LinearLayout balanceContent = new LinearLayout(this);
        balanceContent.setOrientation(LinearLayout.VERTICAL);
        balanceContent.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        balanceCard.addView(balanceContent);

        // Balance Label
        TextView balanceLabel = new TextView(this);
        balanceLabel.setText("Your Balance");
        balanceLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        balanceLabel.setTextColor(Color.parseColor("#666666"));
        balanceLabel.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        balanceContent.addView(balanceLabel);

        // Create Balance TextView with enhanced styling
        txtBalance = new TextView(this);
        txtBalance.setId(R.id.txtBalance);
        txtBalance.setText(String.format("₹ %.2f", balance));
        txtBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        txtBalance.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        txtBalance.setTextColor(Color.parseColor("#1a237e"));
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        balanceParams.setMargins(0, dpToPx(8), 0, 0);
        txtBalance.setLayoutParams(balanceParams);
        balanceContent.addView(txtBalance);

        // Create Modern SearchView
        SearchView searchView = new SearchView(this);
        searchView.setId(R.id.searchView);
        ConstraintLayout.LayoutParams searchViewParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                dpToPx(50)
        );
        searchViewParams.setMargins(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        searchView.setLayoutParams(searchViewParams);
        searchView.setQueryHint("Search transactions...");

        // Style SearchView with rounded background
        GradientDrawable searchBackground = new GradientDrawable();
        searchBackground.setShape(GradientDrawable.RECTANGLE);
        searchBackground.setColor(Color.parseColor("#FFFFFF90"));
        searchBackground.setCornerRadius(dpToPx(25));
        searchView.setBackground(searchBackground);

        constraintLayout.addView(searchView);

        // Create Action Buttons Container
        buttonContainer = new LinearLayout(this);
        buttonContainer.setId(View.generateViewId());
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        ConstraintLayout.LayoutParams buttonContainerParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        buttonContainer.setLayoutParams(buttonContainerParams);
        constraintLayout.addView(buttonContainer);

        // Create Add Money Button with modern styling
        btnAddMoney = createModernButton("Add Money", "💰", Color.parseColor("#4CAF50"));
        btnAddMoney.setId(R.id.btnAddMoney);
        buttonContainer.addView(btnAddMoney);

        // Create Scan QR Button
        btnScanQR = createModernButton("Scan QR", "📱", Color.parseColor("#FF5722"));
        btnScanQR.setId(R.id.btnScanQR);
        buttonContainer.addView(btnScanQR);

        // Create View Transactions Button
        btnViewTransactions = createModernButton("View Transactions", "📋", Color.parseColor("#2196F3"));
        btnViewTransactions.setId(R.id.btnViewTransactions);
        buttonContainer.addView(btnViewTransactions);

        // Create Response Card with modern styling
        CardView responseCard = new CardView(this);
        responseCard.setId(View.generateViewId());
        responseCard.setRadius(dpToPx(20));
        responseCard.setCardElevation(dpToPx(6));
        responseCard.setCardBackgroundColor(Color.parseColor("#FAFAFA"));
        ConstraintLayout.LayoutParams responseCardParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0 // Will be set to MATCH_CONSTRAINT for height
        );
        responseCardParams.setMargins(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(24));
        responseCard.setLayoutParams(responseCardParams);
        constraintLayout.addView(responseCard);

        // Create JSON Response TextView
        txtJsonResponse = new TextView(this);
        txtJsonResponse.setId(R.id.txtJsonResponse);
        txtJsonResponse.setText("Ready to scan or search transactions");
        txtJsonResponse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        txtJsonResponse.setTextColor(Color.parseColor("#424242"));
        txtJsonResponse.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        txtJsonResponse.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        txtJsonResponse.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        responseCard.addView(txtJsonResponse);

        // Set up ConstraintSet for layout
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        // Status bar spacer at top
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.TOP, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Header below status bar spacer
        constraintSet.connect(headerLayout.getId(), ConstraintSet.TOP, statusBarSpacer.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(headerLayout.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(headerLayout.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Balance Card below Header
        constraintSet.connect(balanceCard.getId(), ConstraintSet.TOP, headerLayout.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(balanceCard.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(balanceCard.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // SearchView below Balance Card
        constraintSet.connect(searchView.getId(), ConstraintSet.TOP, balanceCard.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(searchView.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(searchView.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Button Container below SearchView
        constraintSet.connect(buttonContainer.getId(), ConstraintSet.TOP, searchView.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(buttonContainer.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(buttonContainer.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Response Card below Button Container, filling remaining space
        constraintSet.connect(responseCard.getId(), ConstraintSet.TOP, buttonContainer.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(responseCard.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(responseCard.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(responseCard.getId(), ConstraintSet.BOTTOM, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.BOTTOM);

        constraintSet.applyTo(constraintLayout);

        // Set the content view
        setContentView(constraintLayout);

        // Initialize Database Helper
        databaseHelper = new DatabaseHelper(this);

        // Load balance from database
        loadBalanceFromDatabase();

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Test server reachability on startup
        testServerReachability();

        // Add entrance animations
        animateEntranceAnimations();

        // Initialize ActivityResultLauncher for Scanner activity (keeping original logic)
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

                        // Generate PDF for the transaction
                        String pdfPath = GenPdf.generatePdfFromJson(jsonOutput, transactionId, this);
                        if (pdfPath != null) {
                            databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                            Log.d(TAG, "PDF generated and stored for transaction ID: " + transactionId);
                        } else {
                            Log.e(TAG, "Failed to generate PDF for transaction ID: " + transactionId);
                        }

                        // Deduct money from database
                        if (databaseHelper.deductMoney(amount)) {
                            balance = databaseHelper.getCurrentBalance(); // Refresh balance
                            updateBalanceDisplay();
                            showSuccessToast("Payment of ₹ " + String.format("%.2f", amount) + " successful");

                            // Send payment to server
                            if (isNetworkAvailable()) {
                                sendPaymentToServer(amount, upiId, scannedDetails, transactionId);
                            } else {
                                databaseHelper.updateTransactionResponse(transactionId, "No network connection", "FAILED_NO_NETWORK");
                                txtJsonResponse.setText("Error: No network connection\n\nScanned Details (JSON):\n" + jsonOutput);
                                showErrorToast("No network connection");
                            }
                        } else {
                            // Rollback transaction if balance deduction fails
                            databaseHelper.updateTransactionResponse(transactionId, "Balance deduction failed", "FAILED");
                            txtJsonResponse.setText("Error: Failed to process payment\n\nScanned Details (JSON):\n" + jsonOutput);
                            showErrorToast("Failed to process payment");
                        }

                    } else if (amount > balance) {
                        // Insert failed transaction
                        long transactionId = databaseHelper.insertTransaction(
                                amount,
                                upiId != null ? upiId : "test@example.com",
                                jsonOutput,
                                "FAILED_INSUFFICIENT_BALANCE"
                        );

                        // Generate PDF for failed transaction
                        String pdfPath = GenPdf.generatePdfFromJson(jsonOutput, transactionId, this);
                        if (pdfPath != null) {
                            databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                            Log.d(TAG, "PDF generated for failed transaction ID: " + transactionId);
                        }

                        txtJsonResponse.setText("Error: Insufficient balance\n\nScanned Details (JSON):\n" + jsonOutput);
                        showErrorToast("Insufficient balance");
                    } else {
                        // Insert failed transaction
                        long transactionId = databaseHelper.insertTransaction(
                                amount,
                                upiId != null ? upiId : "test@example.com",
                                jsonOutput,
                                "FAILED_INVALID_AMOUNT"
                        );

                        // Generate PDF for failed transaction
                        String pdfPath = GenPdf.generatePdfFromJson(jsonOutput, transactionId, this);
                        if (pdfPath != null) {
                            databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                            Log.d(TAG, "PDF generated for failed transaction ID: " + transactionId);
                        }

                        txtJsonResponse.setText("Error: Invalid amount scanned\n\nScanned Details (JSON):\n" + jsonOutput);
                        showErrorToast("Invalid amount scanned");
                    }
                } catch (NumberFormatException e) {
                    // Insert failed transaction
                    long transactionId = databaseHelper.insertTransaction(
                            0.0,
                            upiId != null ? upiId : "test@example.com",
                            jsonOutput,
                            "FAILED_INVALID_FORMAT"
                    );

                    // Generate PDF for failed transaction
                    String pdfPath = GenPdf.generatePdfFromJson(jsonOutput, transactionId, this);
                    if (pdfPath != null) {
                        databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                        Log.d(TAG, "PDF generated for failed transaction ID: " + transactionId);
                    }

                    txtJsonResponse.setText("Error: Invalid amount format\n\nScanned Details (JSON):\n" + jsonOutput);
                    showErrorToast("Invalid amount format");
                }
            } else {
                txtJsonResponse.setText("Error: No valid scan data received");
                showErrorToast("No valid scan data received");
            }
        });

        // Initialize Add Money Button Listener (keeping original logic)
        btnAddMoney.setOnClickListener(v -> {
            animateButtonClick(btnAddMoney);

            double addAmount = 1000.00;
            if (databaseHelper.addMoney(addAmount)) {
                balance = databaseHelper.getCurrentBalance(); // Refresh balance
                updateBalanceDisplay();
                txtJsonResponse.setText("Added ₹ " + String.format("%.2f", addAmount) + " to balance");
                showSuccessToast("₹ " + String.format("%.2f", addAmount) + " added successfully");

                // Log the add money transaction
                try {
                    JSONObject addMoneyJson = new JSONObject();
                    addMoneyJson.put("type", "ADD_MONEY");
                    addMoneyJson.put("amount", addAmount);
                    addMoneyJson.put("new_balance", balance);

                    long transactionId = databaseHelper.insertTransaction(
                            addAmount,
                            "SYSTEM",
                            addMoneyJson.toString(2),
                            "SUCCESS"
                    );

                    // Generate PDF for add money transaction
                    String pdfPath = GenPdf.generatePdfFromJson(addMoneyJson.toString(2), transactionId, this);
                    if (pdfPath != null) {
                        databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                        Log.d(TAG, "PDF generated for add money transaction ID: " + transactionId);
                    } else {
                        Log.e(TAG, "Failed to generate PDF for add money transaction ID: " + transactionId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error logging add money transaction: " + e.getMessage());
                }
            } else {
                txtJsonResponse.setText("Failed to add money to balance");
                showErrorToast("Failed to add money");
            }
        });

        // Initialize Scan QR Button Listener
        btnScanQR.setOnClickListener(v -> {
            animateButtonClick(btnScanQR);
            Intent intent = new Intent(HomePage.this, Scanner.class);
            scannerLauncher.launch(intent);
        });

        // Initialize View Transactions Button Listener
        btnViewTransactions.setOnClickListener(v -> {
            animateButtonClick(btnViewTransactions);
            Intent intent = new Intent(HomePage.this, PdfListActivity.class);
            startActivity(intent);
        });

        // Initialize SearchView Listener (keeping original logic)
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

    // Create modern styled button
    private Button createModernButton(String text, String emoji, int color) {
        Button button = new Button(this);
        button.setText(emoji + "  " + text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setAllCaps(false);

        // Create gradient background
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setShape(GradientDrawable.RECTANGLE);
        buttonBackground.setColor(color);
        buttonBackground.setCornerRadius(dpToPx(16));
        button.setBackground(buttonBackground);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
        );
        buttonParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        button.setLayoutParams(buttonParams);

        return button;
    }

    // Animate entrance animations
    private void animateEntranceAnimations() {
        // Animate balance card
        balanceCard.setAlpha(0f);
        balanceCard.setTranslationY(-100f);
        balanceCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Animate buttons with staggered delay
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            View button = buttonContainer.getChildAt(i);
            button.setAlpha(0f);
            button.setTranslationX(200f);
            button.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(500)
                    .setStartDelay(200 + (i * 100))
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    // Animate button click
    private void animateButtonClick(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        scaleX.start();
        scaleY.start();
    }

    // Show success toast with green background
    private void showSuccessToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Show error toast with red background
    private void showErrorToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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
                        showSuccessToast("Server is reachable");
                    } else {
                        Log.e(TAG, "Server at 192.168.1.9 is NOT reachable");
                        showErrorToast("Cannot reach server");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error checking server reachability: " + e.getMessage());
                    showErrorToast("Error checking server: " + e.getMessage());
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
        txtJsonResponse.setText("Ready to scan or search transactions");
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

                            // Combine scanned JSON and server response for PDF
                            String finalJson = "Scanned Details:\n" + scannedJson + "\n\nServer Response:\n" + formattedJson;

                            // Update transaction in database
                            databaseHelper.updateTransactionResponse(transactionId, formattedJson, "SUCCESS");

                            // Generate PDF with combined JSON
                            String pdfPath = GenPdf.generatePdfFromJson(finalJson, transactionId, this);
                            if (pdfPath != null) {
                                databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                                Log.d(TAG, "PDF generated for transaction ID: " + transactionId);
                            } else {
                                Log.e(TAG, "Failed to generate PDF for transaction ID: " + transactionId);
                            }

                            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + scannedJson + "\n\nServer Response:\n" + formattedJson);
                            showSuccessToast("Payment confirmed on server!");

                            Log.d(TAG, "Payment successful, transaction ID: " + transactionId + ", Response: " + formattedJson);
                        } catch (Exception e) {
                            String errorMsg = "Error formatting JSON: " + e.getMessage();
                            databaseHelper.updateTransactionResponse(transactionId, errorMsg, "SUCCESS_JSON_ERROR");
                            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + convertToStructuredJson(scannedDetails) + "\n\n" + errorMsg);
                            showSuccessToast("Payment sent but response error");
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

                        // Generate PDF for failed server response
                        String scannedJson = convertToStructuredJson(scannedDetails);
                        String finalJson = "Scanned Details:\n" + scannedJson + "\n\nError:\n" + errorMsg;
                        String pdfPath = GenPdf.generatePdfFromJson(finalJson, transactionId, this);
                        if (pdfPath != null) {
                            databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                            Log.d(TAG, "PDF generated for failed transaction ID: " + transactionId);
                        }

                        txtJsonResponse.setText("Scanned QR Details (JSON):\n" + scannedJson + "\n\n" + errorMsg);
                        showErrorToast("Payment processed but server error");
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

            // Generate PDF for preparation error
            String scannedJson = convertToStructuredJson(scannedDetails);
            String finalJson = "Scanned Details:\n" + scannedJson + "\n\nError:\n" + errorMsg;
            String pdfPath = GenPdf.generatePdfFromJson(finalJson, transactionId, this);
            if (pdfPath != null) {
                databaseHelper.updateTransactionPdfPath(transactionId, pdfPath);
                Log.d(TAG, "PDF generated for failed transaction ID: " + transactionId);
            }

            txtJsonResponse.setText("Scanned QR Details (JSON):\n" + scannedJson + "\n\n" + errorMsg);
            showErrorToast(errorMsg);
            Log.e(TAG, "Payment preparation error: " + e.getMessage());
        }
    }

    // Helper method to update balance display with animation
    private void updateBalanceDisplay() {
        // Animate balance change
        ValueAnimator balanceAnimator = ValueAnimator.ofFloat(0f, 1f);
        balanceAnimator.setDuration(400);
        balanceAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        balanceAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            txtBalance.setScaleX(0.9f + (0.1f * animatedValue));
            txtBalance.setScaleY(0.9f + (0.1f * animatedValue));
            txtBalance.setAlpha(0.7f + (0.3f * animatedValue));
        });
        balanceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                txtBalance.setText(String.format("₹ %.2f", balance));
                txtBalance.setScaleX(1f);
                txtBalance.setScaleY(1f);
                txtBalance.setAlpha(1f);
            }
        });
        balanceAnimator.start();
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
                searchResults.append("Amount: ₹ ").append(String.format("%.2f", transaction.amount)).append("\n");
                searchResults.append("Status: ").append(transaction.status).append("\n");
                searchResults.append("Time: ").append(transaction.timestamp).append("\n");
                if (transaction.upiId != null && !transaction.upiId.equals("SYSTEM")) {
                    searchResults.append("UPI ID: ").append(transaction.upiId).append("\n");
                }
                if (transaction.pdfPath != null) {
                    searchResults.append("PDF: Available\n");
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

        // Animate resume
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (balanceCard != null) {
                balanceCard.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            balanceCard.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(200)
                                    .start();
                        })
                        .start();
            }
        }, 100);
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