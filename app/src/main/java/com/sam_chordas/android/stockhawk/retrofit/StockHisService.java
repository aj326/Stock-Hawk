package com.sam_chordas.android.stockhawk.retrofit;

import com.sam_chordas.android.stockhawk.json_pojo.stock_history.StockHistory;

import java.util.Map;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;


/**
 * Created by ahmed on 3/12/16.
 */
public interface StockHisService {
    @GET("/v1/public/yql")
    Call<StockHistory> queryDetails(
            @Query(value = "q")
            String query, @QueryMap Map<String, String> options);
}
