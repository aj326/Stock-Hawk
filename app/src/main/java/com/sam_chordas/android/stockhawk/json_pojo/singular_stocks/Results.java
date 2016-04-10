
package com.sam_chordas.android.stockhawk.json_pojo.singular_stocks;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Results {

    @SerializedName("quote")
    @Expose
    private Quote quote;

    /**
     * 
     * @return
     *     The quote
     */
    public Quote getQuote() {
        return quote;
    }

    /**
     * 
     * @param quote
     *     The quote
     */
    public void setQuote(Quote quote) {
        this.quote = quote;
    }

}
