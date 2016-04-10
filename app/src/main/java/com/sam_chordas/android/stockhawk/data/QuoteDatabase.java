package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
I highly doubt I'll use this library ever again! It's not too developer friendly
 I had to manually delete the db to fix a few errors (in root!)
 *  */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
  private QuoteDatabase(){}


  public static final int VERSION = 1;
  @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
  @Table(HistColumns.class)  public static final String HISTORY = "history_upgraded";
  }

