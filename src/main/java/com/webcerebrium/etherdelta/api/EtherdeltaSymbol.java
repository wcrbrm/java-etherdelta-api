package com.webcerebrium.etherdelta.api;

import com.google.common.base.Strings;

public class EtherdeltaSymbol {

    String symbol = "";

    public EtherdeltaSymbol(String symbol)  throws EtherdeltaApiException  {
        // sanitizing symbol, preventing from common user-input errors
        if (Strings.isNullOrEmpty(symbol)) {
            throw new EtherdeltaApiException ("Symbol cannot be empty. Example: BQXETH");
        }
        if (symbol.contains(" ")) {
            throw new EtherdeltaApiException ("Symbol cannot contain spaces. Example: BQXETH");
        }
        if (!symbol.endsWith("ETH")) {
            throw new EtherdeltaApiException("Market Symbol should be ending with ETH. Example: BQXETH");
        }
        this.symbol = symbol.replace("_", "").replace("-", "").toUpperCase();
    }

    public String get(){ return this.symbol; }

    public String getSymbol(){ return this.symbol; }

    public String toString() { return this.get(); }

    public static EtherdeltaSymbol valueOf(String s) throws EtherdeltaApiException {
        return new EtherdeltaSymbol(s);
    }
}
