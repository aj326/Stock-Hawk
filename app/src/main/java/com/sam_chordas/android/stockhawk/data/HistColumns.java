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
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT)
  public static final String SYMBOL =
          "symbol";
  @DataType(DataType.Type.TEXT) @Unique(onConflict = ConflictResolutionType.REPLACE)
  public static final String DATE = "date";
  @DataType(DataType.Type.REAL)
  public static final String VALUE = "value";


}
