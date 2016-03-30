package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

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
                                                    R.layout.widget_collection_item);
//                value.put(QuoteColumns.SYMBOL, quote.getSymbol());
//                value.put(QuoteColumns.BIDPRICE,
//                          Utils.truncateBidPrice(quote.getBid()));
//                value.put(QuoteColumns.PERCENT_CHANGE, Utils.truncateChange(
//                        quote.getChangeinPercent(), true));
//                value.put(QuoteColumns.ISUP,
//                          quote.getChange().charAt(0) == '-' ? 0 : 1);
//                value.put(QuoteColumns.CHANGE,
//                          Utils.truncateChange(quote.getChange(), false));
//                value.put(QuoteColumns.ISCURRENT, 1);
//                Uri s = mContext.getContentResolver().insert(
//                        QuoteProvider.Quotes.CONTENT_URI, value);
                String symbol = data.getString(1);
                String change = data.getString(2);

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    setRemoteContentDescription(views, description);
//                }
                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.change, change);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra("symbol",symbol);
//                fillInIntent.setData(QuoteProvider.Quotes.withSymbol(symbol));
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
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
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
