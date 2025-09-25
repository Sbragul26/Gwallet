package com.example.gwallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "GWallet.db";
    private static final int DATABASE_VERSION = 2; // Incremented version due to schema change

    // Table names
    private static final String TABLE_BALANCE = "balance";
    private static final String TABLE_TRANSACTIONS = "transactions";

    // Balance table columns
    private static final String BALANCE_ID = "id";
    private static final String BALANCE_AMOUNT = "amount";
    private static final String BALANCE_UPDATED_AT = "updated_at";

    // Transactions table columns
    private static final String TRANS_ID = "id";
    private static final String TRANS_AMOUNT = "amount";
    private static final String TRANS_UPI_ID = "upi_id";
    private static final String TRANS_JSON_DATA = "json_data";
    private static final String TRANS_SERVER_RESPONSE = "server_response";
    private static final String TRANS_TIMESTAMP = "timestamp";
    private static final String TRANS_STATUS = "status"; // SUCCESS, FAILED, PENDING
    private static final String TRANS_PDF_PATH = "pdf_path"; // New column for PDF path

    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create balance table
        String createBalanceTable = "CREATE TABLE " + TABLE_BALANCE + " (" +
                BALANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BALANCE_AMOUNT + " REAL NOT NULL, " +
                BALANCE_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";

        // Create transactions table with pdf_path column
        String createTransactionsTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                TRANS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TRANS_AMOUNT + " REAL NOT NULL, " +
                TRANS_UPI_ID + " TEXT, " +
                TRANS_JSON_DATA + " TEXT, " +
                TRANS_SERVER_RESPONSE + " TEXT, " +
                TRANS_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                TRANS_STATUS + " TEXT DEFAULT 'PENDING', " +
                TRANS_PDF_PATH + " TEXT" + // Added PDF path column
                ")";

        db.execSQL(createBalanceTable);
        db.execSQL(createTransactionsTable);

        // Insert initial balance
        ContentValues values = new ContentValues();
        values.put(BALANCE_AMOUNT, 5000.00);
        db.insert(TABLE_BALANCE, null, values);

        Log.d(TAG, "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add pdf_path column to transactions table
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + TRANS_PDF_PATH + " TEXT");
            Log.d(TAG, "Added pdf_path column to transactions table");
        }
        // Future schema upgrades can be handled here
    }

    // Balance operations
    public double getCurrentBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        double balance = 5000.00; // Default balance

        Cursor cursor = db.query(TABLE_BALANCE,
                new String[]{BALANCE_AMOUNT},
                null, null, null, null,
                BALANCE_ID + " DESC", "1");

        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(cursor.getColumnIndexOrThrow(BALANCE_AMOUNT));
        }
        cursor.close();

        Log.d(TAG, "Current balance retrieved: " + balance);
        return balance;
    }

    public boolean updateBalance(double newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BALANCE_AMOUNT, newBalance);
        values.put(BALANCE_UPDATED_AT, "datetime('now')");

        long result = db.insert(TABLE_BALANCE, null, values);

        Log.d(TAG, "Balance updated to: " + newBalance);
        return result != -1;
    }

    public boolean addMoney(double amount) {
        double currentBalance = getCurrentBalance();
        double newBalance = currentBalance + amount;
        return updateBalance(newBalance);
    }

    public boolean deductMoney(double amount) {
        double currentBalance = getCurrentBalance();
        if (currentBalance >= amount) {
            double newBalance = currentBalance - amount;
            return updateBalance(newBalance);
        }
        return false;
    }

    // Transaction operations
    public long insertTransaction(double amount, String upiId, String jsonData, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRANS_AMOUNT, amount);
        values.put(TRANS_UPI_ID, upiId);
        values.put(TRANS_JSON_DATA, jsonData);
        values.put(TRANS_STATUS, status);
        values.put(TRANS_PDF_PATH, (String) null); // Initially no PDF path

        long transactionId = db.insert(TABLE_TRANSACTIONS, null, values);

        Log.d(TAG, "Transaction inserted with ID: " + transactionId);
        return transactionId;
    }

    public boolean updateTransactionResponse(long transactionId, String serverResponse, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRANS_SERVER_RESPONSE, serverResponse);
        values.put(TRANS_STATUS, status);

        int rowsAffected = db.update(TABLE_TRANSACTIONS, values,
                TRANS_ID + " = ?", new String[]{String.valueOf(transactionId)});

        Log.d(TAG, "Transaction " + transactionId + " updated with response");
        return rowsAffected > 0;
    }

    public boolean updateTransactionPdfPath(long transactionId, String pdfPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRANS_PDF_PATH, pdfPath);

        int rowsAffected = db.update(TABLE_TRANSACTIONS, values,
                TRANS_ID + " = ?", new String[]{String.valueOf(transactionId)});

        Log.d(TAG, "Transaction " + transactionId + " updated with PDF path: " + pdfPath);
        return rowsAffected > 0;
    }

    public boolean clearTransactionPdfPath(long transactionId) {
        return updateTransactionPdfPath(transactionId, null);
    }

    public List<TransactionData> getAllTransactions() {
        List<TransactionData> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null,
                TRANS_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                TransactionData transaction = new TransactionData();
                transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow(TRANS_ID));
                transaction.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TRANS_AMOUNT));
                transaction.upiId = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_UPI_ID));
                transaction.jsonData = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_JSON_DATA));
                transaction.serverResponse = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_SERVER_RESPONSE));
                transaction.timestamp = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_TIMESTAMP));
                transaction.status = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_STATUS));
                transaction.pdfPath = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_PDF_PATH));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return transactions;
    }

    public List<TransactionData> getRecentTransactions(int limit) {
        List<TransactionData> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null,
                TRANS_TIMESTAMP + " DESC", String.valueOf(limit));

        if (cursor.moveToFirst()) {
            do {
                TransactionData transaction = new TransactionData();
                transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow(TRANS_ID));
                transaction.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TRANS_AMOUNT));
                transaction.upiId = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_UPI_ID));
                transaction.jsonData = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_JSON_DATA));
                transaction.serverResponse = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_SERVER_RESPONSE));
                transaction.timestamp = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_TIMESTAMP));
                transaction.status = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_STATUS));
                transaction.pdfPath = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_PDF_PATH));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return transactions;
    }

    public List<TransactionData> getTransactionsWithPdfs() {
        List<TransactionData> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, TRANS_PDF_PATH + " IS NOT NULL", null, null, null,
                TRANS_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                TransactionData transaction = new TransactionData();
                transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow(TRANS_ID));
                transaction.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TRANS_AMOUNT));
                transaction.upiId = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_UPI_ID));
                transaction.jsonData = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_JSON_DATA));
                transaction.serverResponse = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_SERVER_RESPONSE));
                transaction.timestamp = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_TIMESTAMP));
                transaction.status = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_STATUS));
                transaction.pdfPath = cursor.getString(cursor.getColumnIndexOrThrow(TRANS_PDF_PATH));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return transactions;
    }

    // Clear all data (for testing purposes)
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, null, null);
        db.delete(TABLE_BALANCE, null, null);

        // Reset to initial balance
        ContentValues values = new ContentValues();
        values.put(BALANCE_AMOUNT, 5000.00);
        db.insert(TABLE_BALANCE, null, values);

        Log.d(TAG, "All data cleared and reset to initial state");
    }

    // Data class for transactions
    public static class TransactionData {
        public long id;
        public double amount;
        public String upiId;
        public String jsonData;
        public String serverResponse;
        public String timestamp;
        public String status;
        public String pdfPath; // New field for PDF path

        @Override
        public String toString() {
            return "Transaction{" +
                    "id=" + id +
                    ", amount=" + amount +
                    ", upiId='" + upiId + '\'' +
                    ", status='" + status + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", pdfPath='" + pdfPath + '\'' +
                    '}';
        }
    }
}