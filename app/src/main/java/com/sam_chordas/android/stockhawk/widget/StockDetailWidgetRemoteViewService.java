package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by ahmed on 3/30/16.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockDetailWidgetRemoteViewService extends RemoteViewsService {
    public final String LOG_TAG = StockDetailWidgetRemoteViewService.class.getSimpleName();


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                  null,
                                                  null,
                                                  null,
                                                  null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                    data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                                                    R.layout.list_item);
                String symbol = data.getString(data.getColumnIndex("symbol"));
                views.setTextViewText(R.id.stock_symbol, data.getString(data.getColumnIndex("symbol")));
                views.setTextViewText(R.id.bid_price,data.getString(data.getColumnIndex("bid_price")));
                if (data.getInt(data.getColumnIndex("is_up")) == 1){
                        views.setInt(R.id.change, "setBackgroundResource",
                                     R.drawable.percent_change_pill_green);
                    }
                else{
                    views.setInt(R.id.change, "setBackgroundResource",
                                 R.drawable.percent_change_pill_red);
                }
                if (Utils.showPercent){
                    views.setTextViewText(R.id.change,data.getString(data.getColumnIndex("percent_change")));
                } else{
                    views.setTextViewText(R.id.change,
                                          data.getString(data.getColumnIndex("change")));
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.setData(QuoteProvider.History.withSymbol(symbol));
//                fillInIntent.
                Log.d(LOG_TAG,fillInIntent.toString());
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }
//
//            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
//            private void setRemoteContentDescription(RemoteViews views, String description) {
//                views.setContentDescription(R.id.widget_icon, description);
//            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(0);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
