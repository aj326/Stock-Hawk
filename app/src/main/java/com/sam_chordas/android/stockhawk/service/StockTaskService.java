package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.QueryMulti;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Query_;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Quote;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Results;
import com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Query;
import com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.QuerySingular;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.retrofit.QuoteFetchService;
import com.sam_chordas.android.stockhawk.retrofit.QuotesFetchService;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

//    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate, isInit, isAdd;
    private StringBuilder symbols;

    final Retrofit retrofit = new Retrofit.Builder().baseUrl(
            "https://query.yahooapis.com").addConverterFactory(
            GsonConverterFactory.create(new GsonBuilder().disableHtmlEscaping().create())).build();
    static  HttpUrl url = HttpUrl.parse("http://ichart.finance.yahoo.com/table.csv");
    private final OkHttpClient client = new OkHttpClient();

    public StockTaskService() {
    }



    public StockTaskService(Context context) {
        mContext = context;
    }

    private void plotStock(String symbol) throws GSMFail {

        DateTime dt = new DateTime(new Date());

        HttpUrl myUrl = url.newBuilder()
//        s 	Ticker symbol (YHOO in the example)
//        a     The "from month"
//        b 	The "from day"
//        c 	The "from year"
//        d 	The "to month"
//        e 	The "to day"
//        f 	The "to year"
//        g 	d for day, m for month, y for yearly
                .addQueryParameter("s", symbol)
//                .addQueryParameter("a","1")
//                .addQueryParameter("b", "1")
//                .addQueryParameter("c", "2010")
//                .addQueryParameter("d", String.valueOf(dt.getMonthOfYear()))
//                .addQueryParameter("e", String.valueOf(dt.getDayOfMonth()))
//                .addQueryParameter("f",String.valueOf(dt.getYear()))
                .addQueryParameter("g", "m")
                .addQueryParameter("ignore", ".csv")
                .build();
        Log.d(LOG_TAG,myUrl.toString());

        Request request = new Request.Builder().url(myUrl).build();


        final Gson gson = new Gson();
        client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
            }

            @Override
            public void onResponse(Response response) throws IOException {
//                StockHistory stock= gson.fromJson(response.body().charStream(), StockHistory.class);
//                Parcelable wrapped = Parcels.wrap(stock.getQuery().getResults().getQuote());

//                result.getQuote()

                BufferedReader reader = new BufferedReader(
                                response.body().charStream()
                        );
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);
                ArrayList<String> dates = new ArrayList<String>();
                ArrayList<String> values = new ArrayList<String>();
                for (CSVRecord record : records) {
                    Log.d(LOG_TAG, (record.get("Date")));
                    dates.add(record.get("Date"));
                    values.add(record.get("Close"));
                }
                Intent intent = new Intent("plot");
                intent.putStringArrayListExtra("date", dates);
                intent.putStringArrayListExtra("val", values);

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                        intent);


            }
        });
//        options.put("format", "json");
//        options.put("env", "store://datatables.org/alltableswithkeys");
//        StockHisService stockHisService = retrofit.create(StockHisService.class);
//        String s = urlString.replace("%3D", "=");
//        Log.d(LOG_TAG,"Grabbing Historical Stock info "+ s);
//        Call<StockHistory> call = stockHisService.queryDetails(s, options);
//
//        call.enqueue(
//                new Callback<StockHistory>() {
//                    @Override
//                    public void onResponse(
//                            retrofit.Response<StockHistory> response,
//                            Retrofit retrofit) {
//                        StockHistory stock = response.body();
//                        if(stock!=null)
//                        {
//                            Parcelable wrapped = Parcels.wrap(stock.getQuery().getResults().getQuote());
//                            Intent intent = new Intent("plot");
//                            intent.putExtra("data",wrapped);
//                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(
//                                    intent);
//
//
//                        }
//
//                    }
//
//                    @Override
//                    public void onFailure(Throwable t) {
//                    }
//                });



    }
//    TODO clean up, refactor
    private void insertData(String url) throws GSMFail{
        final Map<String, String> options = new HashMap<>();
        options.put("format", "json");
        options.put("env", "store://datatables.org/alltableswithkeys");
        QuoteFetchService quoteFetchService = retrofit.create(QuoteFetchService.class);
        Call<QuerySingular> call = quoteFetchService.queryList(url, options);
        Log.d(LOG_TAG, "INSERTING data");
        call.enqueue(new Callback<QuerySingular>() {

                         @Override
                         public void onResponse(
                                 retrofit.Response<QuerySingular> response,
                                 Retrofit retrofit) {
                             QuerySingular queries = response.body();
                             if (queries != null) {
                                 Query query = queries.getQuery();
                                 Log.d(LOG_TAG, "INSERT: date: " + query.getCreated());
                                 com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Results results = query.getResults();
                                 com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Quote quote = results.getQuote();
                                 Log.d(LOG_TAG, "insert" + quote.getSymbol());
                                 if (quote.getAsk() == null) {
                                     Log.d(LOG_TAG, "Stock DNE");
                                     Intent RTReturn = new Intent(MyStocksActivity.INVALID);
                                     LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                                             RTReturn);
                                     return;
                                 }
                                 ContentValues value = new ContentValues();
                                 value.put(QuoteColumns.SYMBOL, quote.getSymbol());
                                 value.put(QuoteColumns.BIDPRICE,
                                           Utils.truncateBidPrice(quote.getBid()));
                                 value.put(QuoteColumns.PERCENT_CHANGE, Utils.truncateChange(
                                         quote.getChangeinPercent(), true));
                                 value.put(QuoteColumns.ISUP,
                                           quote.getChange().charAt(0) == '-' ? 0 : 1);
                                 value.put(QuoteColumns.CHANGE,
                                           Utils.truncateChange(quote.getChange(), false));
                                 value.put(QuoteColumns.ISCURRENT, 1);
                                 Uri s = mContext.getContentResolver().insert(
                                         QuoteProvider.Quotes.CONTENT_URI, value);
                                 Log.d(LOG_TAG, s.toString());
//                                 result[0] = GcmNetworkManager.RESULT_SUCCESS;
                             }
                         }

                         @Override
                         public void onFailure(Throwable t) {
                             try {
                                 throw new GSMFail();
                             } catch (GSMFail gsmFail) {
                                 gsmFail.printStackTrace();
                             }
                         }
                     }
        );
//        return result[0];
    }

    void updateData(String url, final boolean isInit) throws GSMFail{
        final Map<String, String> options = new HashMap<>();
        options.put("format", "json");
        options.put("env", "store://datatables.org/alltableswithkeys");
        Log.d(LOG_TAG, "UPDATE");
        QuotesFetchService quotesFetchService = retrofit.create(QuotesFetchService.class);
        Call<QueryMulti> call = quotesFetchService.queryList(url, options);
        final Integer[] result = new Integer[1];
        call.enqueue(new Callback<QueryMulti>() {

            @Override
            public void onFailure(Throwable t) {
                try {
                    throw new GSMFail();
                } catch (GSMFail gsmFail) {
                    gsmFail.printStackTrace();
                }
            }



            @Override
            public void onResponse(
                    retrofit.Response<QueryMulti> response,
                    Retrofit retrofit) {
                QueryMulti queries = response.body();
                if (queries != null) {
                    Query_ query = queries.getQuery();
                    Log.d(LOG_TAG, query.getCreated() + "");
                    Results results = query.getResults();
                    int numOfQuotes = results.getQuote().size();
                    Log.d(LOG_TAG, numOfQuotes + "num of q");
                    ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();
                    if(isInit)
                    for (Quote quote : results.getQuote()) {
                        contentProviderOperations.add(ContentProviderOperation.newInsert(
                                QuoteProvider.Quotes.withSymbol(quote.getSymbol()))
                                .withValue(QuoteColumns.SYMBOL, quote.getSymbol())
                                                              .withValue(QuoteColumns.BIDPRICE,
                                                                         Utils.truncateBidPrice(
                                                                                 quote.getBid()))
                                                              .withValue(
                                                                      QuoteColumns.PERCENT_CHANGE,
                                                                      Utils.truncateChange(
                                                                              quote.getChangeinPercent(),
                                                                              true))
                                                              .withValue(QuoteColumns.ISUP,
                                                                         quote.getChange().charAt(
                                                                                 0) == '-' ? 0
                                                                                           : 1).withValue(
                                        QuoteColumns.CHANGE,
                                        Utils.truncateChange(quote.getChange(),
                                                             false)).withValue(
                                        QuoteColumns.ISCURRENT, 1).build());

                        Log.d("Initing", quote.getSymbol());
                    }
                    else
                    for (Quote quote : results.getQuote()) {
                        contentProviderOperations.add(ContentProviderOperation.newUpdate(
                                QuoteProvider.Quotes.withSymbol(quote.getSymbol()))
//                                .withValue(QuoteColumns.SYMBOL, quote.getSymbol())
                                                              .withValue(QuoteColumns.BIDPRICE,
                                                                         Utils.truncateBidPrice(
                                                                                 quote.getBid()))
                                                              .withValue(
                                                                      QuoteColumns.PERCENT_CHANGE,
                                                                      Utils.truncateChange(
                                                                              quote.getChangeinPercent(),
                                                                              true))
                                                              .withValue(QuoteColumns.ISUP,
                                                                         quote.getChange().charAt(
                                                                                 0) == '-' ? 0
                                                                                           : 1).withValue(
                                        QuoteColumns.CHANGE,
                                        Utils.truncateChange(quote.getChange(),
                                                             false)).withValue(
                                        QuoteColumns.ISCURRENT, 1).build());

                        Log.d("Updating", quote.getSymbol());
                    }
                    try {
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,contentProviderOperations);
                    } catch (RemoteException | OperationApplicationException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public void grabQuotes() {
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        String urlString;
        symbols = new StringBuilder();
        StringBuilder urlStringBuilder = new StringBuilder();
        if(params.getTag().equals("chart")){


            try {
                plotStock(
                        params.getExtras().getString("symbol"));

            } catch (GSMFail gsmFail) {
                return GcmNetworkManager.RESULT_FAILURE;

        }}
        else
        try {

            // Base URL for the Yahoo query
//      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(
                    URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                                      + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                                  new String[]{"Distinct " + QuoteColumns.SYMBOL},
                                                                  null,
                                                                  null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
//                isUpdate = false;
                isInit = true;

                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    symbols.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    Log.d(LOG_TAG, "init cursor is empty, running init");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                isUpdate = true;
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                                          initQueryCursor.getString(
                                                  initQueryCursor.getColumnIndex(
                                                          "symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                    symbols.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isAdd = true;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
                symbols.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        urlString = urlStringBuilder.toString();
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, urlString);
        if(isAdd)
        {
            try {
                insertData(urlString);
            } catch (GSMFail gsmFail) {
            return GcmNetworkManager.RESULT_FAILURE;
        }

        }
        try {
            updateData(urlString, isInit);
        } catch (GSMFail gsmFail) {
            return GcmNetworkManager.RESULT_FAILURE;
        }
        //      try{
//        getResponse = fetchData(urlString);
//        try {
//          ContentValues contentValues = new ContentValues();
//          // update ISCURRENT to 0 (false) so new data is current
////          if (isUpdate){
////            contentValues.put(QuoteColumns.ISCURRENT, 0);
////            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
////                                                 null, null);
//
//
////          }
////          try{
//          Log.d(LOG_TAG,isUpdate+"");
//          if(isUpdate){
//
//          }
//          if( Utils.quoteJsonToContentVals(getResponse,isUpdate)!= null)
//
//              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
//              Utils.quoteJsonToContentVals(getResponse,isUpdate));
//          else{
//        }
//        }catch (RemoteException | OperationApplicationException e){
//          Log.e(LOG_TAG, "Error applying batch insert", e);
////        }
//      } catch (IOException e){
//        e.printStackTrace();
//      }
//    }
        return GcmNetworkManager.RESULT_SUCCESS;
    }




    private class GSMFail extends Exception {
    }
}