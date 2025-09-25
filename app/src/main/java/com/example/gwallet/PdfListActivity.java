package com.example.gwallet;

import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class PdfListActivity extends AppCompatActivity {

    private static final String TAG = "PdfListActivity";
    private ListView listView;
    private DatabaseHelper databaseHelper;
    private PdfAdapter adapter;
    private List<DatabaseHelper.TransactionData> transactionsWithPdfs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_list);

        databaseHelper = new DatabaseHelper(this);
        listView = findViewById(R.id.listViewPdfs);
        loadPdfs();
    }

    private void loadPdfs() {
        transactionsWithPdfs = databaseHelper.getTransactionsWithPdfs();
        if (transactionsWithPdfs.isEmpty()) {
            Toast.makeText(this, "No PDF transactions available", Toast.LENGTH_SHORT).show();
            TextView emptyView = new TextView(this);
            emptyView.setText("No PDF transactions found");
            emptyView.setTextSize(16);
            emptyView.setPadding(16, 16, 16, 16);
            listView.setEmptyView(emptyView);
        } else {
            adapter = new PdfAdapter(this, transactionsWithPdfs);
            listView.setAdapter(adapter);
            Log.d(TAG, "Loaded " + transactionsWithPdfs.size() + " transactions with PDFs");
        }
    }

    private void openPdf(String pdfPath) {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            Toast.makeText(this, "PDF file not found at " + pdfPath, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "No PDF viewer app found. Please install one.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "No activity found to handle PDF intent");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
        }
    }

    private void showJsonContent(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            Toast.makeText(this, "No JSON data available", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Failed to generate PDF. Check logs.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "PDF generation failed");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error processing JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        new AlertDialog.Builder(this)
                .setTitle("Delete PDF")
                .setMessage("Are you sure you want to delete this PDF?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    boolean deleted = GenPdf.deletePdfForTransaction(transactionId, this);
                    if (deleted) {
                        databaseHelper.clearTransactionPdfPath(transactionId);
                        transactionsWithPdfs.remove(position);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "PDF deleted successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Deleted PDF for transaction ID: " + transactionId);
                    } else {
                        Toast.makeText(this, "Failed to delete PDF", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to delete PDF for transaction ID: " + transactionId);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private class PdfAdapter extends ArrayAdapter<DatabaseHelper.TransactionData> {
        public PdfAdapter(PdfListActivity context, List<DatabaseHelper.TransactionData> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_pdf_transaction, parent, false);
            }

            DatabaseHelper.TransactionData transaction = getItem(position);

            TextView txtPayeeName = convertView.findViewById(R.id.txtPayeeName);
            TextView txtTotalAmount = convertView.findViewById(R.id.txtTotalAmount);
            Button btnDelete = convertView.findViewById(R.id.btnDeletePdf);

            try {
                JSONObject jsonObject = new JSONObject(transaction.jsonData);
                String payeeName = jsonObject.optString("payeeName", "Unknown Payee");
                String totalAmount = jsonObject.optString("total_amount", "N/A");
                txtPayeeName.setText(payeeName);
                txtTotalAmount.setText("Rs " + totalAmount);
            } catch (Exception e) {
                txtPayeeName.setText("Error");
                txtTotalAmount.setText("N/A");
                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            }

            convertView.setOnClickListener(v -> showJsonContent(transaction.jsonData));
            btnDelete.setOnClickListener(v -> deletePdf(transaction.id, position));

            return convertView;
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