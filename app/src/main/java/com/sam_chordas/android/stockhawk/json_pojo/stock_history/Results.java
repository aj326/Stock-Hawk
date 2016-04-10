
package com.sam_chordas.android.stockhawk.json_pojo.stock_history;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;

import javax.annotation.Generated;

@Parcel
@Generated("org.jsonschema2pojo")
public class Results {

    @SerializedName("quote")
    @Expose
    ArrayList<Quote> quote = new ArrayList<Quote>();

    /**
     * 
     * @return
     *     The quote
     */
    public ArrayList<Quote> getQuote() {
        return quote;
    }

    /**
     * 
     * @param quote
     *     The quote
     */
    public void setQuote(ArrayList<Quote> quote) {
        this.quote = quote;
    }

}
