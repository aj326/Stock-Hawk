package com.sam_chordas.android.stockhawk.rest;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by ahmed on 4/10/16.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({PlotErrorHandling.PLOT_STATUS_OK, PlotErrorHandling.PLOT_STATUS_SERVER_DOWN, PlotErrorHandling.PLOT_STATUS_SERVER_INVALID, PlotErrorHandling.PLOT_STATUS_UNKNOWN})
public @interface PlotErrorHandling {
    public static final int PLOT_STATUS_OK = 0;
    public static final int PLOT_STATUS_SERVER_DOWN = 1;
    public static final int PLOT_STATUS_SERVER_INVALID = 2;
    public static final int PLOT_STATUS_UNKNOWN = 3;
}
