package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

/**
 * Created by sam_chordas on 10/5/15.
 */
public class HistColumns {
  @DataType(DataType.Type.INTEGER)
  @PrimaryKey
  @AutoIncrement
  public static final String _ID = "_id";

  @DataType(DataType.Type.TEXT)
  @Unique(onConflict = ConflictResolutionType.REPLACE)
  public static final String SYMBOL = "symbol";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_0 = "date_0";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_0 = "value_0";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_1 = "date_1";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_1 = "value_1";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_2 = "date_2";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_2 = "value_2";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_3 = "date_3";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_3 = "value_3";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_4 = "date_4";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_4 = "value_4";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_5 = "date_5";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_5 = "value_5";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_6 = "date_6";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_6 = "value_6";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_7 = "date_7";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_7 = "value_7";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_8 = "date_8";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_8 = "value_8";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_9 = "date_9";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_9 = "value_9";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_10 = "date_10";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_10 = "value_10";

  @DataType(DataType.Type.TEXT)
  public static final String DATE_11 = "date_11";
  @DataType(DataType.Type.REAL)
  public static final String VALUE_11 = "value_11";
}