package com.example.gwallet;

import android.content.Intent;
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
import java.io.File;
import java.util.List;
import android.util.Log;

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
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No PDF viewer app found");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
        }
    }

    private void showJsonContent(String jsonData) {
        new AlertDialog.Builder(this)
                .setTitle("Transaction Details (JSON)")
                .setMessage(jsonData != null ? jsonData : "No JSON data available")
                .setPositiveButton("OK", null)
                .show();
        Log.d(TAG, "Displayed JSON content: " + (jsonData != null ? jsonData : "null"));
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

    // Custom Adapter for ListView
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

            TextView txtTransactionInfo = convertView.findViewById(R.id.txtTransactionInfo);
            Button btnDelete = convertView.findViewById(R.id.btnDeletePdf);

            String info = "ID: " + transaction.id +
                    "\nAmount: Rs " + String.format("%.2f", transaction.amount) +
                    "\nStatus: " + transaction.status +
                    "\nTime: " + transaction.timestamp +
                    "\nUPI: " + (transaction.upiId != null ? transaction.upiId : "N/A");
            txtTransactionInfo.setText(info);

            // Click transaction info to show JSON content
            txtTransactionInfo.setOnClickListener(v -> showJsonContent(transaction.jsonData));

            // Click entire item to open PDF
            convertView.setOnClickListener(v -> openPdf(transaction.pdfPath));

            // Delete button
            btnDelete.setOnClickListener(v -> deletePdf(transaction.id, position));

            return convertView;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPdfs(); // Refresh PDF list
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}