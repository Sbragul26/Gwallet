package com.example.gwallet;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private static final String TAG = "PdfListActivity";
    private ListView listView;
    private DatabaseHelper databaseHelper;
    private PdfAdapter adapter;
    private List<DatabaseHelper.TransactionData> transactionsWithPdfs;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = new DatabaseHelper(this);

        // Create main container with gradient background
        ConstraintLayout mainLayout = new ConstraintLayout(this);
        mainLayout.setId(View.generateViewId());

        // Create gradient background
        GradientDrawable gradientBackground = new GradientDrawable();
        gradientBackground.setShape(GradientDrawable.RECTANGLE);
        gradientBackground.setColors(new int[]{
                Color.parseColor("#667eea"), // Light blue
                Color.parseColor("#764ba2")  // Purple
        });
        gradientBackground.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        mainLayout.setBackground(gradientBackground);

        // Create Status Bar Spacer
        View statusBarSpacer = new View(this);
        statusBarSpacer.setId(View.generateViewId());
        ConstraintLayout.LayoutParams spacerParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
        );
        statusBarSpacer.setLayoutParams(spacerParams);
        mainLayout.addView(statusBarSpacer);

        // Create Header Layout
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
        mainLayout.addView(headerLayout);

        // Back Button (simulated with arrow icon)
        Button backButton = new Button(this);
        backButton.setText("←");
        backButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        backButton.setTextColor(Color.WHITE);
        backButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        GradientDrawable backButtonBg = new GradientDrawable();
        backButtonBg.setShape(GradientDrawable.OVAL);
        backButtonBg.setColor(Color.parseColor("#FFFFFF30"));
        backButton.setBackground(backButtonBg);
        LinearLayout.LayoutParams backButtonParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        backButtonParams.setMargins(0, 0, dpToPx(16), 0);
        backButton.setLayoutParams(backButtonParams);
        backButton.setOnClickListener(v -> {
            animateButtonClick(backButton);
            finish();
        });
        headerLayout.addView(backButton);

        // Header Title
        TextView headerTitle = new TextView(this);
        headerTitle.setText("Transaction History");
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        headerTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        headerTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        headerTitle.setLayoutParams(titleParams);
        headerLayout.addView(headerTitle);

        // Stats Icon (optional)
        View statsIcon = new View(this);
        GradientDrawable statsBackground = new GradientDrawable();
        statsBackground.setShape(GradientDrawable.OVAL);
        statsBackground.setColor(Color.parseColor("#FFFFFF20"));
        statsIcon.setBackground(statsBackground);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        statsIcon.setLayoutParams(statsParams);
        headerLayout.addView(statsIcon);

        // Create Content Container
        CardView contentCard = new CardView(this);
        contentCard.setId(View.generateViewId());
        contentCard.setRadius(dpToPx(20));
        contentCard.setCardElevation(dpToPx(8));
        contentCard.setCardBackgroundColor(Color.WHITE);
        ConstraintLayout.LayoutParams contentParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0 // Will be set to match constraint
        );
        contentParams.setMargins(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        contentCard.setLayoutParams(contentParams);
        mainLayout.addView(contentCard);

        // Create List View with modern styling
        listView = new ListView(this);
        listView.setId(View.generateViewId());
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        listView.setClipToPadding(false);
        listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        contentCard.addView(listView);

        // Create Empty State Layout
        emptyStateLayout = new LinearLayout(this);
        emptyStateLayout.setId(View.generateViewId());
        emptyStateLayout.setOrientation(LinearLayout.VERTICAL);
        emptyStateLayout.setGravity(Gravity.CENTER);
        emptyStateLayout.setPadding(dpToPx(40), dpToPx(40), dpToPx(40), dpToPx(40));
        emptyStateLayout.setVisibility(View.GONE);
        contentCard.addView(emptyStateLayout);

        // Empty State Icon (simulated with colored circle)
        View emptyIcon = new View(this);
        GradientDrawable emptyIconBg = new GradientDrawable();
        emptyIconBg.setShape(GradientDrawable.OVAL);
        emptyIconBg.setColor(Color.parseColor("#E0E0E0"));
        emptyIcon.setBackground(emptyIconBg);
        LinearLayout.LayoutParams emptyIconParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
        emptyIconParams.setMargins(0, 0, 0, dpToPx(20));
        emptyIcon.setLayoutParams(emptyIconParams);
        emptyStateLayout.addView(emptyIcon);

        // Empty State Text
        emptyStateText = new TextView(this);
        emptyStateText.setText("No PDF transactions available");
        emptyStateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        emptyStateText.setTextColor(Color.parseColor("#757575"));
        emptyStateText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        emptyStateText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emptyTextParams.setMargins(0, 0, 0, dpToPx(10));
        emptyStateText.setLayoutParams(emptyTextParams);
        emptyStateLayout.addView(emptyStateText);

        // Empty State Subtitle
        TextView emptySubtitle = new TextView(this);
        emptySubtitle.setText("Complete transactions to see them here");
        emptySubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        emptySubtitle.setTextColor(Color.parseColor("#9E9E9E"));
        emptySubtitle.setGravity(Gravity.CENTER);
        emptyStateLayout.addView(emptySubtitle);

        // Set up constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);

        // Status bar spacer at top
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.TOP, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(statusBarSpacer.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Header below status bar spacer
        constraintSet.connect(headerLayout.getId(), ConstraintSet.TOP, statusBarSpacer.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(headerLayout.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(headerLayout.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);

        // Content card filling remaining space
        constraintSet.connect(contentCard.getId(), ConstraintSet.TOP, headerLayout.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(contentCard.getId(), ConstraintSet.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(contentCard.getId(), ConstraintSet.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(contentCard.getId(), ConstraintSet.BOTTOM, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintSet.BOTTOM);

        constraintSet.applyTo(mainLayout);
        setContentView(mainLayout);

        // Add entrance animations
        animateEntranceAnimations(contentCard);

        // Load PDFs
        loadPdfs();
    }

    private void loadPdfs() {
        transactionsWithPdfs = databaseHelper.getTransactionsWithPdfs();

        if (transactionsWithPdfs.isEmpty()) {
            showEmptyState();
            Toast.makeText(this, "No PDF transactions available", Toast.LENGTH_SHORT).show();
        } else {
            hideEmptyState();
            adapter = new PdfAdapter();
            listView.setAdapter(adapter);
            Log.d(TAG, "Loaded " + transactionsWithPdfs.size() + " transactions with PDFs");
        }
    }

    private void showEmptyState() {
        listView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);

        // Animate empty state appearance
        emptyStateLayout.setAlpha(0f);
        emptyStateLayout.setTranslationY(50f);
        emptyStateLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    private void animateEntranceAnimations(CardView contentCard) {
        // Animate content card
        contentCard.setAlpha(0f);
        contentCard.setTranslationY(100f);
        contentCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void animateButtonClick(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        scaleX.start();
        scaleY.start();
    }

    private void openPdf(String pdfPath) {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            showStyledToast("PDF file not found", false);
            Log.e(TAG, "PDF file not found at: " + pdfPath);
            return;
        }

        try {
            Uri pdfUri = FileProvider.getUriForFile(this, "com.example.gwallet.fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showStyledToast("No PDF viewer app found. Please install one.", false);
                Log.w(TAG, "No activity found to handle PDF intent");
            }
        } catch (Exception e) {
            showStyledToast("Error opening PDF: " + e.getMessage(), false);
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
        }
    }

    private void showJsonContent(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            showStyledToast("No JSON data available", false);
            Log.d(TAG, "No JSON data available");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String payeeName = jsonObject.optString("payeeName", "Transaction");
            String upiId = jsonObject.optString("upiId", "N/A");
            String totalAmount = jsonObject.optString("total_amount", "N/A");
            JSONArray productsArray = jsonObject.optJSONArray("products");
            StringBuilder products = new StringBuilder();
            if (productsArray != null) {
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject product = productsArray.getJSONObject(i);
                    String name = product.optString("name", "Unknown Product");
                    String price = product.optString("price", "N/A");
                    products.append((i + 1) + ". " + name + " (Price: " + price + ")\n");
                }
            } else {
                products.append("No products listed");
            }

            String pdfPath = createPdfFromJson(payeeName, upiId, totalAmount, products.toString());
            if (pdfPath != null) {
                openPdf(pdfPath);
                Log.d(TAG, "Successfully generated and opened PDF at: " + pdfPath);
            } else {
                showStyledToast("Failed to generate PDF. Check logs.", false);
                Log.e(TAG, "PDF generation failed");
            }
        } catch (Exception e) {
            showStyledToast("Error processing JSON: " + e.getMessage(), false);
            Log.e(TAG, "Error processing JSON: " + e.getMessage(), e);
        }
    }

    private String createPdfFromJson(String payeeName, String upiId, String totalAmount, String products) {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size in points
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(14);

            int y = 40;
            canvas.drawText("Transaction Receipt", 40, y, paint);
            y += 30;

            canvas.drawText("Payee Name: " + payeeName, 40, y, paint);
            y += 20;
            canvas.drawText("UPI ID: " + upiId, 40, y, paint);
            y += 30;

            canvas.drawText("Item No. | Product Name | Price", 40, y, paint);
            y += 20;
            canvas.drawLine(40, y, 555, y, paint); // Horizontal line
            y += 10;

            String[] productLines = products.split("\n");
            for (String line : productLines) {
                if (y > 800) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 40;
                    canvas.drawText("Item No. | Product Name | Price", 40, y, paint);
                    y += 20;
                    canvas.drawLine(40, y, 555, y, paint);
                    y += 10;
                }
                canvas.drawText(line, 40, y, paint);
                y += 20;
            }
            y += 30;

            canvas.drawText("Total Amount: Rs " + totalAmount, 40, y, paint);

            document.finishPage(page);

            String sanitizedPayeeName = payeeName.replaceAll("[^a-zA-Z0-9]", "_");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = sanitizedPayeeName + "_" + timeStamp + ".pdf";
            File file = new File(getExternalFilesDir(null), fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.writeTo(fos);
                Log.d(TAG, "PDF successfully written to: " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Failed to write PDF to file: " + e.getMessage(), e);
                return null;
            }
            document.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error creating PDF: " + e.getMessage(), e);
            return null;
        }
    }

    private void deletePdf(long transactionId, int position) {
        // Create modern styled AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Delete PDF")
                .setMessage("Are you sure you want to delete this PDF?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = GenPdf.deletePdfForTransaction(transactionId, this);
                    if (deleted) {
                        databaseHelper.clearTransactionPdfPath(transactionId);
                        transactionsWithPdfs.remove(position);
                        adapter.notifyDataSetChanged();

                        // Check if list is now empty
                        if (transactionsWithPdfs.isEmpty()) {
                            showEmptyState();
                        }

                        showStyledToast("PDF deleted successfully", true);
                        Log.d(TAG, "Deleted PDF for transaction ID: " + transactionId);
                    } else {
                        showStyledToast("Failed to delete PDF", false);
                        Log.e(TAG, "Failed to delete PDF for transaction ID: " + transactionId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStyledToast(String message, boolean isSuccess) {
        Toast toast = Toast.makeText(this, message, isSuccess ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    // Modern List Adapter
    private class PdfAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return transactionsWithPdfs.size();
        }

        @Override
        public Object getItem(int position) {
            return transactionsWithPdfs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DatabaseHelper.TransactionData transaction = transactionsWithPdfs.get(position);

            // Create modern card for each transaction
            CardView itemCard = new CardView(PdfListActivity.this);
            itemCard.setRadius(dpToPx(16));
            itemCard.setCardElevation(dpToPx(4));
            itemCard.setCardBackgroundColor(Color.WHITE);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dpToPx(20), 0, dpToPx(32)); // Increased margins for more gap
            itemCard.setLayoutParams(cardParams);

            // Main content layout
            LinearLayout contentLayout = new LinearLayout(PdfListActivity.this);
            contentLayout.setOrientation(LinearLayout.HORIZONTAL);
            contentLayout.setPadding(dpToPx(20), dpToPx(16), dpToPx(16), dpToPx(16));
            contentLayout.setGravity(Gravity.CENTER_VERTICAL);
            itemCard.addView(contentLayout);

            // Transaction Icon
            View transactionIcon = new View(PdfListActivity.this);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(Color.parseColor("#4CAF50"));
            transactionIcon.setBackground(iconBg);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
            iconParams.setMargins(0, 0, dpToPx(16), 0);
            transactionIcon.setLayoutParams(iconParams);
            contentLayout.addView(transactionIcon);

            // Transaction Details Layout
            LinearLayout detailsLayout = new LinearLayout(PdfListActivity.this);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            detailsLayout.setLayoutParams(detailsParams);
            contentLayout.addView(detailsLayout);

            // Payee Name
            TextView txtPayeeName = new TextView(PdfListActivity.this);
            txtPayeeName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            txtPayeeName.setTextColor(Color.parseColor("#212121"));
            txtPayeeName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            detailsLayout.addView(txtPayeeName);

            // Amount
            TextView txtTotalAmount = new TextView(PdfListActivity.this);
            txtTotalAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            txtTotalAmount.setTextColor(Color.parseColor("#4CAF50"));
            txtTotalAmount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            amountParams.setMargins(0, dpToPx(4), 0, 0);
            txtTotalAmount.setLayoutParams(amountParams);
            detailsLayout.addView(txtTotalAmount);

            // Status indicator
            TextView statusIndicator = new TextView(PdfListActivity.this);
            statusIndicator.setText("PDF Available");
            statusIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            statusIndicator.setTextColor(Color.parseColor("#757575"));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.setMargins(0, dpToPx(2), 0, 0);
            statusIndicator.setLayoutParams(statusParams);
            detailsLayout.addView(statusIndicator);

            // Delete Button
            Button btnDelete = new Button(PdfListActivity.this);
            btnDelete.setText("X");
            btnDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            btnDelete.setTextColor(Color.parseColor("#F44336"));

            GradientDrawable deleteBg = new GradientDrawable();
            deleteBg.setShape(GradientDrawable.RECTANGLE);
            deleteBg.setColor(Color.parseColor("#FFEBEE"));
            deleteBg.setCornerRadius(dpToPx(12));
            btnDelete.setBackground(deleteBg);

            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            btnDelete.setLayoutParams(deleteParams);
            contentLayout.addView(btnDelete);

            // Parse and set data
            try {
                JSONObject jsonObject = new JSONObject(transaction.jsonData);
                String payeeName = jsonObject.optString("payeeName", "Unknown Payee");
                String totalAmount = jsonObject.optString("total_amount", "N/A");

                txtPayeeName.setText(payeeName);
                txtTotalAmount.setText("₹ " + totalAmount);

                // Set different icon colors based on transaction type
                if (payeeName.equals("SYSTEM")) {
                    iconBg.setColor(Color.parseColor("#2196F3")); // Blue for add money
                } else {
                    iconBg.setColor(Color.parseColor("#FF5722")); // Orange for payments
                }

            } catch (Exception e) {
                txtPayeeName.setText("Error parsing data");
                txtTotalAmount.setText("₹ N/A");
                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            }

            // Set click listeners
            itemCard.setOnClickListener(v -> {
                animateButtonClick(itemCard);
                showJsonContent(transaction.jsonData);
            });

            btnDelete.setOnClickListener(v -> {
                animateButtonClick(btnDelete);
                deletePdf(transaction.id, position);
            });

            // Add entrance animation for list items
            itemCard.setAlpha(0f);
            itemCard.setTranslationX(100f);
            itemCard.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setStartDelay(position * 50)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            return itemCard;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPdfs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}