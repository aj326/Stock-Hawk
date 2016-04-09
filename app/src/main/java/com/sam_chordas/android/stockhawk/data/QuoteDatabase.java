package com.sam_chordas.android.stockhawk.data;

import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

/**
* due to using simonvt and altering a table, I found the best solution to alter a table is
 * to drop the old one and create a new one with a different name
 * this explains why my version is 15 and my onUpgrade is hacky!
 *  */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
  private QuoteDatabase(){}


  public static final int VERSION = 15;
  @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
  @Table(HistColumns.class)  public static final String HISTORY = "history_upgraded";
  @OnUpgrade
  public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      if(newVersion >= 14)
      db.beginTransaction();
      try {
       db.execSQL("DROP TABLE IF EXISTS history");
      }
      finally {
        db.endTransaction();
      }
    }

  }

