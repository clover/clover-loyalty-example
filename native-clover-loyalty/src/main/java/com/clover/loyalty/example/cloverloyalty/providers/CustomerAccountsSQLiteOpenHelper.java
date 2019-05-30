package com.clover.loyalty.example.cloverloyalty.providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CustomerAccountsSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "CloverCustomer Loyalty";
    private static final String DATABASE_TABLE_CUSTOMER_ACCOUNTS = "CUSTOMER_ACCOUNTS";
    private static final String[] CUSTOMER_ACCOUNTS_COLUMNS = {
        CUSTOMER_ACCOUNT_COLUMN.ID.columnName(),
        CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(),
        CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(),
        CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(),
        CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(),
        CUSTOMER_ACCOUNT_COLUMN.EMAIL.columnName()
    };

    public CustomerAccountsSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create tables if they do not exist...
        String createSql = String.format("CREATE TABLE %s ( " +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "%s TEXT, " +
                "%s TEXT, " +
                "%s TEXT, " +
                "%s INTEGER," +
                "%s TEXT) ",
            DATABASE_TABLE_CUSTOMER_ACCOUNTS,
            CUSTOMER_ACCOUNT_COLUMN.ID,
            CUSTOMER_ACCOUNT_COLUMN.UUID,
            CUSTOMER_ACCOUNT_COLUMN.NAME,
            CUSTOMER_ACCOUNT_COLUMN.PHONE,
            CUSTOMER_ACCOUNT_COLUMN.POINTS,
            CUSTOMER_ACCOUNT_COLUMN.EMAIL
        );

        db.execSQL(createSql);
        getCustomerAccountsCount();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // if version number is different call this method
        if (oldVersion == 1) {
            if (newVersion == 2) {
                // added EMAIL column.
                String addEmailSql = String.format( "ALTER TABLE %s ADD %s TEXT", DATABASE_TABLE_CUSTOMER_ACCOUNTS, CUSTOMER_ACCOUNT_COLUMN.EMAIL);

                db.execSQL(addEmailSql);
            }
        }
    }

    public CustomerAccounts.CustomerAccount addCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
        // log
        Log.d("addCustomerAccount", customerAccount.toString());

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        //        values.put(CUSTOMER_ACCOUNT_COLUMN.ID.columnName(), customerAccount.id);
        values.put(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(), customerAccount.uuid);
        values.put(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(), customerAccount.name);
        values.put(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(), customerAccount.phone);
        values.put(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(), customerAccount.points);
        values.put(CUSTOMER_ACCOUNT_COLUMN.EMAIL.columnName(), customerAccount.email);

        db.insert(DATABASE_TABLE_CUSTOMER_ACCOUNTS, // table
            null, //nullColumnHack
            values);

        String sql = "SELECT MAX(ID) FROM " + DATABASE_TABLE_CUSTOMER_ACCOUNTS;
        Cursor cursor = db.rawQuery(sql, null);

        CustomerAccounts.CustomerAccount copyAccount = null;

        if (cursor != null) {
            cursor.moveToFirst();
            int val = cursor.getInt(CUSTOMER_ACCOUNT_COLUMN.ID.getColumnIndex());
            copyAccount = new CustomerAccounts.CustomerAccount(val, customerAccount.uuid, customerAccount.name, customerAccount.phone, customerAccount.points, customerAccount.email, customerAccount.accountTransactions);
        }
        db.close();

        return copyAccount;
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN column, String value) {

        int count = getCustomerAccountsCount();
        if(count > 0) {
            // 1. get reference to readable DB
            SQLiteDatabase db = this.getReadableDatabase();

            SQLiteException sqlle = null;
            for (int attempts=0;attempts<2;attempts++) {
                try {
                    // 2. build query
                    // SELECT ID, TITLE, AUTHOR FROM BOOK WHERE ID = id"
                    Cursor cursor =
                        db.query(DATABASE_TABLE_CUSTOMER_ACCOUNTS, // a. table
                            CUSTOMER_ACCOUNTS_COLUMNS, // b. column names
                            column.columnName() + " like ?", // c. selections
                            new String[]{value}, // d. selections args
                            null, // e. group by
                            null, // f. having
                            null, // g. order by
                            null); // h. limit
                    return convertCursorToCustomerAccounts(cursor);
                } catch (SQLiteException e) {
                    sqlle = e;
                    // if "no such column: EMAIL"
                    if (sqlle.getMessage().contains("no such column: EMAIL")) {
                        onUpgrade(db, 1, 2);
                    }
                }
            }
            throw sqlle;
        }
        return Collections.EMPTY_LIST;
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByUUID(String uuid) {
        return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.UUID, uuid);
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByPhone(String phone) {
        return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.PHONE, "%" + phone + "%");
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByEmail(String email) {
        return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.EMAIL, email);
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsById(String id) {
        return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.ID, id);
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByName(String name) {
        return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.NAME, name);
    }

    public List<CustomerAccounts.CustomerAccount> getCustomerAccounts() {

        // 1. get reference to readable DB
        SQLiteDatabase db = this.getReadableDatabase();

        // 2. build query
        // SELECT ID, TITLE, AUTHOR FROM BOOK WHERE ID = id"
        String selectAllSQL = "SELECT * FROM " + DATABASE_TABLE_CUSTOMER_ACCOUNTS;
        Cursor cursor = db.rawQuery(selectAllSQL, null);

        return convertCursorToCustomerAccounts(cursor);

    }

    public int getCustomerAccountsCount() {
        // 1. get reference to readable DB
        SQLiteDatabase db = this.getReadableDatabase();

        // 2. build query
        // SELECT ID, TITLE, AUTHOR FROM BOOK WHERE ID = id"
        String selectAllSQL = "SELECT COUNT(*) FROM " + DATABASE_TABLE_CUSTOMER_ACCOUNTS;
        Cursor cursor = db.rawQuery(selectAllSQL, null);

        int count = 0;

        if (cursor != null) {
            cursor.moveToFirst();

            count = Integer.parseInt(cursor.getString(0));
        }

        if (count == 0) {
            addCustomerAccount(new CustomerAccounts.CustomerAccount(1, UUID.randomUUID().toString().replace("-", ""), "Fred Beebe", "2173453736", 12, "fred.beebe@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(2, UUID.randomUUID().toString().replace("-", ""), "Joe Tinker", "3035248859", 10, "Joe.Tinker@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(3, UUID.randomUUID().toString().replace("-", ""), "Johnny Evers", "5055642957", 22, "Johnny.Evers@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(4, UUID.randomUUID().toString().replace("-", ""), "Frank Chance", "7205436141", 16, "Frank.Chance@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(5, UUID.randomUUID().toString().replace("-", ""), "Johnny Kling", "3125269970", 31, "Johnny.Kling@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(6, UUID.randomUUID().toString().replace("-", ""), "Mordecai Brown", "3126509369", 19, "Mordecai.Brown@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(7, UUID.randomUUID().toString().replace("-", ""), "Bob Wicker", "7083415713", 8, "Bob.Wicker@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            addCustomerAccount(new CustomerAccounts.CustomerAccount(8, UUID.randomUUID().toString().replace("-", ""), "Pete Noonan", "6034635110", 3, "Pete.Noonan@invalid.com", new ArrayList<CustomerAccounts.AccountTransaction>()));
            return getCustomerAccountsCount();
        }
        return count;

    }

    public int getCustomerAccountsCountByPhone(String phone) {
        // 1. get reference to readable DB
        SQLiteDatabase db = this.getReadableDatabase();


        // 2. build query
        // SELECT ID, TITLE, AUTHOR FROM BOOK WHERE ID = id"
        String selectAllSQL = "SELECT COUNT(*) FROM " + DATABASE_TABLE_CUSTOMER_ACCOUNTS + " where phone like ?";
        Cursor cursor = db.rawQuery(selectAllSQL, new String[]{"%" + phone + "%"});

        int count = 0;

        if (cursor != null) {
            cursor.moveToFirst();

            count = Integer.parseInt(cursor.getString(0));
        }
        return count;

    }

    // Updating single book
    public int updateCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(), customerAccount.uuid);
        values.put(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(), customerAccount.name);
        values.put(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(), customerAccount.phone);
        values.put(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(), customerAccount.points);

        // 3. updating row
        // "UPDATE BOOK SET TITLE = book.title, AUTHOR = book.author WHERE ID = book.id"
        int i = db.update(DATABASE_TABLE_CUSTOMER_ACCOUNTS, //table
            values, // column/value
            CUSTOMER_ACCOUNT_COLUMN.ID.columnName() + " = ?", // selections
            new String[]{String.valueOf(customerAccount.id)}); //selection args

        // 4. close
        db.close();

        // log
        Log.d("update CustomerAccount", customerAccount.toString());
        return i;

    }

    public CustomerAccounts.CustomerAccount createCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
        // log
        Log.d("add CustomerAccount", customerAccount.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        //        values.put(CUSTOMER_ACCOUNT_COLUMN.ID.columnName(), customerAccount.id);
        values.put(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(), customerAccount.uuid);
        values.put(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(), customerAccount.name);
        values.put(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(), customerAccount.phone);
        values.put(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(), customerAccount.points);

        // 3. insert
        // "INSERT INTO BOOK (TITLE, AUTHOR) VALUES (book.title, book.author)"

        long result = db.insert(DATABASE_TABLE_CUSTOMER_ACCOUNTS, // table
            null, //nullColumnHack
            values); // key/value -> keys = column names/ values = column values

        // result == 2 for successful insert...?

        // 4. close
        db.close();

        List<CustomerAccounts.CustomerAccount> customerAccountsByUUID = getCustomerAccountsByUUID(customerAccount.uuid);
        if (customerAccountsByUUID.size() == 1) {
            return customerAccountsByUUID.get(0);
        }
        return null;
    }

    public boolean deleteCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        // "DELETE BOOK WHERE ID = book.id"
        int result = db.delete(DATABASE_TABLE_CUSTOMER_ACCOUNTS,
            CUSTOMER_ACCOUNT_COLUMN.ID + " = ?",
            new String[]{String.valueOf(customerAccount.id)});

        // 3. close
        db.close();

        // log
        Log.d("delete CustomerAccount", customerAccount.toString());

        return result == 0;
    }

    private List<CustomerAccounts.CustomerAccount> convertCursorToCustomerAccounts(Cursor cursor) {
        // 3. go over each row, build book and add it to list
        List<CustomerAccounts.CustomerAccount> accounts = new LinkedList<>();
        CustomerAccounts.CustomerAccount customerAccount = null;
        if (cursor.moveToFirst()) {
            do {
                customerAccount = new CustomerAccounts.CustomerAccount(
                    cursor.getInt(CUSTOMER_ACCOUNT_COLUMN.ID.getColumnIndex()),
                    cursor.getString(CUSTOMER_ACCOUNT_COLUMN.UUID.getColumnIndex()),
                    cursor.getString(CUSTOMER_ACCOUNT_COLUMN.NAME.getColumnIndex()),
                    cursor.getString(CUSTOMER_ACCOUNT_COLUMN.PHONE.getColumnIndex()),
                    cursor.getInt(CUSTOMER_ACCOUNT_COLUMN.POINTS.getColumnIndex()),
                    cursor.getString(CUSTOMER_ACCOUNT_COLUMN.EMAIL.getColumnIndex()),

                    Collections.<CustomerAccounts.AccountTransaction>emptyList());

                accounts.add(customerAccount);
            } while (cursor.moveToNext());
        }

        return accounts;
    }


    enum CUSTOMER_ACCOUNT_COLUMN {
        ID {
            @Override
            public String columnName() {
                return "ID";
            }

            @Override
            public int getColumnIndex() {
                return 0;
            }
        },
        UUID {
            @Override
            public String columnName() {
                return "UUID";
            }

            @Override
            public int getColumnIndex() {
                return 1;
            }

        },
        NAME {
            @Override
            public String columnName() {
                return "NAME";
            }

            @Override
            public int getColumnIndex() {
                return 2;
            }

        },
        PHONE {
            @Override
            public String columnName() {
                return "PHONE";
            }

            @Override
            public int getColumnIndex() {
                return 3;
            }

        },
        POINTS {
            @Override
            public String columnName() {
                return "POINTS";
            }

            @Override
            public int getColumnIndex() {
                return 4;
            }
        },
        EMAIL {
            @Override
            public String columnName() {
                return "EMAIL";
            }

            @Override
            public int getColumnIndex() {
                return 5;
            }
        };

        public String columnName() {
            return null;
        }

        public int getColumnIndex() {
            return -1;
        }
    }
}