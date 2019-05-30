package com.clover.loyalty.example.pos.accounts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomerAccountsSQLiteOpenHelper extends SQLiteOpenHelper {

  private static final String TAG = "CustAcctSQLhtlpr";

  private static final int DATABASE_VERSION = 4;
  private static final String DATABASE_NAME = "CloverLoyalty";
  private static final String DATABASE_TABLE_CUSTOMER_ACCOUNTS = "CUSTOMER_ACCOUNTS";
  private static final String DATABASE_TABLE_CUSTOMER_ACCOUNTS_CARDS = "CUSTOMER_ACCOUNTS_CARDS";

  private static final String FULL_QUERY =
      "SELECT * from " + DATABASE_TABLE_CUSTOMER_ACCOUNTS + " a INNER JOIN "
      + DATABASE_TABLE_CUSTOMER_ACCOUNTS_CARDS
      + " WHERE " + CUSTOMER_ACCOUNT_COLUMN.UUID.columnName() + " = "
      + CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CUSTOMER_UUID.columnName();

  private static final String[] CUSTOMER_ACCOUNTS_COLUMNS = {
      CUSTOMER_ACCOUNT_COLUMN.ID.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.EMAIL.columnName(),
      CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID.columnName(),
  };

  public CustomerAccountsSQLiteOpenHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // create tables if they do not exist...
    createCustomerAccountsTable(db);
    createLinkedCardsTable(db);
  }

  private void createCustomerAccountsTable(SQLiteDatabase db) {
    String createSql = String.format("CREATE TABLE %s ( " +
                                     "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                     "%s TEXT, " +
                                     "%s TEXT, " +
                                     "%s TEXT, " +
                                     "%s INTEGER," +
                                     "%s TEXT," +
                                     "%s TEXT) ",
        DATABASE_TABLE_CUSTOMER_ACCOUNTS,
        CUSTOMER_ACCOUNT_COLUMN.ID,
        CUSTOMER_ACCOUNT_COLUMN.UUID,
        CUSTOMER_ACCOUNT_COLUMN.NAME,
        CUSTOMER_ACCOUNT_COLUMN.PHONE,
        CUSTOMER_ACCOUNT_COLUMN.POINTS,
        CUSTOMER_ACCOUNT_COLUMN.EMAIL,
        CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID
    );
    Log.d(TAG, String.format("createDb %s", createSql));
    db.execSQL(createSql);
  }

  private void createLinkedCardsTable(SQLiteDatabase db) {
    String createSql2 = String.format("CREATE TABLE %s ( " +
                                      "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                      "%s TEXT, " +
                                      "%s TEXT, " +
                                      "%s TEXT) ",
        DATABASE_TABLE_CUSTOMER_ACCOUNTS_CARDS,
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_ID,
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CUSTOMER_UUID,
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE,
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4
    );

    Log.d(TAG, String.format("createDb %s", createSql2));
    db.execSQL(createSql2);
  }

  private String[] upgradeStatements = new String[] {
      String.format( "ALTER TABLE %s ADD %s TEXT", DATABASE_TABLE_CUSTOMER_ACCOUNTS, CUSTOMER_ACCOUNT_COLUMN.EMAIL), // 1 to 2
      String.format( "ALTER TABLE %s ADD %s TEXT", DATABASE_TABLE_CUSTOMER_ACCOUNTS, CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID) // 2 to 3
  };

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d(TAG, String.format("onUpgrade oldVersion %s, newVersion %s", oldVersion, newVersion));

    // if version number is different call this method
    for(int incrementalVer = oldVersion; incrementalVer < newVersion; incrementalVer++) {
      int idx = (oldVersion + incrementalVer) - 1;
      if (idx < upgradeStatements.length) {
        String upgradeStatement = upgradeStatements[oldVersion + incrementalVer];
        if (null != upgradeStatement) {
          db.execSQL(upgradeStatement);
        }
      }
    }
    // Major revision.  New table.
    if (oldVersion < 4 && newVersion == 4) {
      createLinkedCardsTable(db);
    }
  }

  public CustomerAccounts.CustomerAccount addCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
    Log.d(TAG, String.format("addCustomerAccount %s", customerAccount.toString()));

    SQLiteDatabase db = this.getWritableDatabase();

    CustomerAccounts.CustomerAccount copyAccount = null;

    Cursor cursor = null;
    db.beginTransaction();
    try {
      insertBaseCustomer(customerAccount, db);

      String sql = "SELECT MAX(ID) FROM " + DATABASE_TABLE_CUSTOMER_ACCOUNTS;
      cursor = db.rawQuery(sql, null);

      if (cursor != null) {
        cursor.moveToFirst();
        int val = cursor.getInt(CUSTOMER_ACCOUNT_COLUMN.ID.getColumnIndex());
        copyAccount = new CustomerAccounts.CustomerAccount(
            val,
            customerAccount.uuid,
            customerAccount.name,
            customerAccount.phone,
            customerAccount.points,
            customerAccount.email,
            customerAccount.externalId,
            customerAccount.accountTransactions,
            customerAccount.customerCards);
        // add in the cards
        if (customerAccount.customerCards != null && customerAccount.customerCards.size() > 0) {
          for (CustomerAccounts.CustomerCard card : customerAccount.customerCards) {
            insertCustomerCard(customerAccount, db, card);
          }
        }
      }
      db.setTransactionSuccessful();
    } finally {
      if (cursor!=null) {
        try {
          cursor.close();
        } catch(Exception e) {
          Log.w(TAG, "Error closing cursor", e);
        }
      }
      db.endTransaction();
    }
    db.close();

    return copyAccount;
  }

  private void insertCustomerCard(CustomerAccounts.CustomerAccount customerAccount, SQLiteDatabase db, CustomerAccounts.CustomerCard card) {
    ContentValues cardValues = new ContentValues();
    cardValues.put(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CUSTOMER_UUID.columnName(), customerAccount.uuid);
    cardValues.put(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE.columnName(), card.type);
    cardValues.put(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4.columnName(), card.last4);
    db.insert(DATABASE_TABLE_CUSTOMER_ACCOUNTS_CARDS, // table
        null, //nullColumnHack
        cardValues);
  }

  private void insertBaseCustomer(CustomerAccounts.CustomerAccount customerAccount, SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(), customerAccount.uuid);
    values.put(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(), customerAccount.name);
    values.put(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(), customerAccount.phone);
    values.put(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(), customerAccount.points);
    values.put(CUSTOMER_ACCOUNT_COLUMN.EMAIL.columnName(), customerAccount.email);
    values.put(CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID.columnName(), customerAccount.externalId);

    db.insert(DATABASE_TABLE_CUSTOMER_ACCOUNTS, // table
        null, //nullColumnHack
        values);
  }

  private List<CustomerAccounts.CustomerAccount> getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN column, String value) {
    return getCustomerAccountsByColumn(column.toString(), value);
  }

  private List<CustomerAccounts.CustomerAccount> getCustomerAccountsLikeColumn(String column, String value) {
    String baseSingleItemQuery = String.format("%s %s", FULL_QUERY, " AND %s like ?");
    String rawQuery = String.format(baseSingleItemQuery, column);
    return getCustomerAccountsQuery(rawQuery, new String[]{value});
  }

  private List<CustomerAccounts.CustomerAccount> getCustomerAccountsLike2Columns(String column1, String value1, String column2, String value2) {
    String baseSingleItemQuery = String.format("%s %s", FULL_QUERY,
        " AND %s like ? AND %s like ?");
    String rawQuery = String.format(baseSingleItemQuery, column1, column2);
    return getCustomerAccountsQuery(rawQuery, new String[]{value1, value2});
  }

  private List<CustomerAccounts.CustomerAccount> getCustomerAccountsByColumn(String column, String value) {
    String baseSingleItemQuery = String.format("%s %s", FULL_QUERY, " AND %s = ?");
    String rawQuery = String.format(baseSingleItemQuery, column);
    return getCustomerAccountsQuery(rawQuery, new String[]{value});
  }

  private List<CustomerAccounts.CustomerAccount> getCustomerAccountsQuery(String rawQuery, String[] values) {
    // 1. get reference to readable DB
    int count = getCustomerAccountsCount();
    if(count > 0) {
      SQLiteDatabase db = this.getReadableDatabase();

      SQLiteException sqlle = null;
      for (int attempts = 0; attempts < 2; attempts++) {
        try {
          Cursor cursor =
              db.rawQuery(rawQuery, values);
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
    return Collections.emptyList();
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
    return getCustomerAccountsByColumn(CUSTOMER_ACCOUNT_COLUMN.ID.toString(), id);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByName(String name) {
    return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.NAME, name);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByExternalId(String extId) {
    return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID, extId);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByCardType(String cardType) {
    return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE.toString(), cardType);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByCardLast4(String cardLast4) {
    return getCustomerAccountsLikeColumn(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4.toString(), cardLast4);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccountsByCard(String cardType, String cardLast4) {
    return getCustomerAccountsLike2Columns(
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE.toString(), cardType,
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4.toString(), cardLast4);
  }

  public List<CustomerAccounts.CustomerAccount> getCustomerAccounts() {
    return getCustomerAccountsQuery(FULL_QUERY, null);
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
      try {
        cursor.close();
      } catch(Exception e) {
        Log.w(TAG, "Error closing cursor", e);
      }
    }

    if (count == 0) {
      // Add some test data
      addCustomerAccount(new CustomerAccounts.CustomerAccount(1, UUID.randomUUID().toString().replace("-", ""), "Fred Beebe", "2173453736", 12, "fred.beebe@invalid.com", "2173453736", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(2, UUID.randomUUID().toString().replace("-", ""), "Joe Tinker", "3035248859", 10, "Joe.Tinker@invalid.com", "3035248859", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(3, UUID.randomUUID().toString().replace("-", ""), "Johnny Evers", "5055642957", 22, "Johnny.Evers@invalid.com", "5055642957", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(4, UUID.randomUUID().toString().replace("-", ""), "Frank Chance", "7205436141", 16, "Frank.Chance@invalid.com", "7205436141", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(5, UUID.randomUUID().toString().replace("-", ""), "Johnny Kling", "3125269970", 31, "Johnny.Kling@invalid.com", "3125269970", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(6, UUID.randomUUID().toString().replace("-", ""), "Mordecai Brown", "3126509369", 19, "Mordecai.Brown@invalid.com", "3126509369", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(7, UUID.randomUUID().toString().replace("-", ""), "Bob Wicker", "7083415713", 8, "Bob.Wicker@invalid.com", "7083415713", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(8, UUID.randomUUID().toString().replace("-", ""), "Pete Noonan", "6034635110", 3, "Pete.Noonan@invalid.com", "6034635110", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(9, UUID.randomUUID().toString().replace("-", ""), "Michael Angelo", "1234567890", 46, "Michael.Angelo@invalid.com", "1234567890", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      addCustomerAccount(new CustomerAccounts.CustomerAccount(10, UUID.randomUUID().toString().replace("-", ""), "Ettu Brutus", "1118675309", 46, "Ettu.Brutus@invalid.com", "1118675309", new ArrayList<CustomerAccounts.AccountTransaction>(), new ArrayList<CustomerAccounts.CustomerCard>()));
      return getCustomerAccountsCount();
    }
    return count;
  }

  public int updateCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
    // 1. get reference to writable DB
    SQLiteDatabase db = this.getWritableDatabase();

    db.beginTransaction();

    int i;
    try {
      CustomerAccounts.CustomerAccount currentCustomerAccount = getCustomerAccount(customerAccount);
      i = updateCustomerAccountBase(customerAccount, db);

      if (currentCustomerAccount.customerCards != customerAccount.customerCards) {
        // They might both be null...if they are they will not get here
        // now we have to do additional work.

        // Are they both non-null and or empty?  If one is null and the other is empty,
        // then we will consider them to be equal
        if (currentCustomerAccount.customerCards == null &&
            customerAccount.customerCards != null && customerAccount.customerCards.size() == 0) {
          // We will consider these equal
          Log.d(TAG, "Customer cards have not changed");
        } else if (customerAccount.customerCards == null &&
                   currentCustomerAccount.customerCards != null && currentCustomerAccount.customerCards.size() == 0) {
          // We will consider these equal
          Log.d(TAG, "Customer cards have not changed");
        } else if (customerAccount.customerCards != null &&
                   currentCustomerAccount.customerCards != null &&
                   customerAccount.customerCards.size() == 0 &&
                   currentCustomerAccount.customerCards.size() == 0) {
          // We will consider these equal
          Log.d(TAG, "Customer cards have not changed");
        } else {
          updateContainedCards(customerAccount, db, currentCustomerAccount);
        }
      }
      // 4. close
      db.close();

      // log
      Log.d(TAG, String.format("update CustomerAccount %s", customerAccount.toString()));
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return i;
  }

  private void updateContainedCards(CustomerAccounts.CustomerAccount customerAccount, SQLiteDatabase db, CustomerAccounts.CustomerAccount currentCustomerAccount) {
    // There is a difference in the lists.  This gets nasty.
    // We may have additions as well as deletions at the same time.
    // For these objects we do not do modifications, they are either there or not
    List<CustomerAccounts.CustomerCard> deletions = new ArrayList<>(currentCustomerAccount.customerCards);
    List<CustomerAccounts.CustomerCard> additions = new ArrayList<>(customerAccount.customerCards);
    for (CustomerAccounts.CustomerCard existingCard : currentCustomerAccount.customerCards) {
      // we do not need to add it, we already have it.
      additions.remove(existingCard);
    }
    for (CustomerAccounts.CustomerCard addedCard : customerAccount.customerCards) {
      // we do not need to delete it, we are keeping it
      deletions.remove(addedCard);
    }
    if (additions.size() > 0) {
      for (CustomerAccounts.CustomerCard addedCard : additions) {
        insertCustomerCard(customerAccount, db, addedCard);
      }
    }
    if (deletions.size() > 0) {
      for (CustomerAccounts.CustomerCard deletedCard : deletions) {
        deleteCustomerCard(customerAccount, db, deletedCard);
      }
    }
  }

  private void deleteCustomerCard(CustomerAccounts.CustomerAccount customerAccount, SQLiteDatabase db, CustomerAccounts.CustomerCard deletedCard) {
    String whereClause =
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CUSTOMER_UUID.columnName() + " = ?" +
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE.columnName() + " = ?" +
        CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4.columnName() + " = ?";
    db.delete(DATABASE_TABLE_CUSTOMER_ACCOUNTS_CARDS,
        whereClause,
        new String[]{customerAccount.uuid, deletedCard.type, deletedCard.last4});
  }

  private int updateCustomerAccountBase(CustomerAccounts.CustomerAccount customerAccount, SQLiteDatabase db) {
    // 2. create ContentValues to add key "column"/value
    ContentValues values = new ContentValues();
    values.put(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName(), customerAccount.uuid);
    values.put(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName(), customerAccount.name);
    values.put(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName(), customerAccount.phone);
    values.put(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName(), customerAccount.points);

    return db.update(DATABASE_TABLE_CUSTOMER_ACCOUNTS, //table
        values, // column/value
        CUSTOMER_ACCOUNT_COLUMN.ID.columnName() + " = ?", // selections
        new String[]{String.valueOf(customerAccount.id)});
  }

  private CustomerAccounts.CustomerAccount getCustomerAccount(CustomerAccounts.CustomerAccount customerAccount) {
    List<CustomerAccounts.CustomerAccount> currentCustomerAccountList = getCustomerAccountsById(String.valueOf(customerAccount.id));
    CustomerAccounts.CustomerAccount currentCustomerAccount = null;
    if (currentCustomerAccountList.size() > 0) {
      currentCustomerAccount = currentCustomerAccountList.get(0);
      if (currentCustomerAccountList.size() > 1) {
        // Something is wrong, there should only be one.
        Log.d(TAG, "Found multiple accounts with id = " + customerAccount.id + ".  Using first one.");
      }
    }
    return currentCustomerAccount;
  }

  private List<CustomerAccounts.CustomerAccount> convertCursorToCustomerAccounts(Cursor cursor) {
    // 3. go over each row, build book and add it to list
    Map<Integer, CustomerAccounts.CustomerAccount> accountMap = new HashMap<>();
    CustomerAccounts.CustomerAccount customerAccount;
    if (cursor.moveToFirst()) {
      do {
        int customerId = cursor.getInt(CUSTOMER_ACCOUNT_COLUMN.ID.getColumnIndex());
        customerAccount = accountMap.get(customerId);
        if (customerAccount == null) {
          customerAccount = new CustomerAccounts.CustomerAccount(
              cursor.getInt(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.ID.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.UUID.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.NAME.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.PHONE.columnName())),
              cursor.getInt(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.POINTS.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.EMAIL.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_COLUMN.EXTERNAL_ID.columnName())),

              Collections.<CustomerAccounts.AccountTransaction>emptyList(),
              new ArrayList<CustomerAccounts.CustomerCard>()); // note the difference hwere, this should NOT be immutable
          accountMap.put(customerId, customerAccount);
        }

        if (!cursor.isNull(cursor.getColumnIndex(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CUSTOMER_UUID.columnName()))) {
          CustomerAccounts.CustomerCard card = new CustomerAccounts.CustomerCard(
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_TYPE.columnName())),
              cursor.getString(cursor.getColumnIndex(CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN.CARD_LAST4.columnName()))
          );
          if (customerAccount.customerCards == null) { // should never be null, but make sure.
            customerAccount.customerCards = new ArrayList<>();
          }
          customerAccount.customerCards.add(card);
        }
      } while (cursor.moveToNext());
      return new ArrayList<>(accountMap.values());
    }
    return Collections.emptyList();
  }

  enum CUSTOMER_ACCOUNT_LINKED_CARDS_COLUMN {
    CARD_ID {
      @Override
      public String columnName() {
        return "CARD_ID";
      }

      @Override
      public int getColumnIndex() {
        return 0;
      }
    },
    CUSTOMER_UUID {
      @Override
      public String columnName() {
        return "CUSTOMER_UUID";
      }

      @Override
      public int getColumnIndex() {
        return 1;
      }

    },
    CARD_TYPE {
      @Override
      public String columnName() {
        return "CARD_TYPE";
      }

      @Override
      public int getColumnIndex() {
        return 2;
      }
    },
    CARD_LAST4 {
      @Override
      public String columnName() {
        return "CARD_LAST4";
      }

      @Override
      public int getColumnIndex() {
        return 3;
      }
    };

    public String columnName() {
      return null;
    }

    public int getColumnIndex() {
      return -1;
    }
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
    },
    EXTERNAL_ID {
      @Override
      public String columnName() {
        return "EXTERNAL_ID";
      }

      @Override
      public int getColumnIndex() {
        return 6;
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