package com.sam_chordas.android.stockhawk.retrofit;

import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.QueryMulti;

import java.util.Map;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;


/**
 * Created by ahmed on 3/11/16.
 */
public interface QuotesFetchService {
    @GET("/v1/public/yql")
    Call<QueryMulti> queryList(
            @Query("q") String query,
            @QueryMap Map<String, String> options);
}
