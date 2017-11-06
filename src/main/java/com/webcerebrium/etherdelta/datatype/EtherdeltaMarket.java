package com.webcerebrium.etherdelta.datatype;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class EtherdeltaMarket {

    String tokenSymbol = "UNKNOWN";
    String tokenAddr;

    long decimals = 18;

    EtherdeltaOrderBook orderbook = new EtherdeltaOrderBook();
    List<EtherdeltaTrade> trades = new LinkedList<>();
    List<EtherdeltaReturnTicker> tickers = new LinkedList<>();
    JsonObject jsonOriginal = null;

    public BigDecimal getMakerFee() { return BigDecimal.ZERO; }
    public BigDecimal getTakerFee() { return BigDecimal.valueOf(0.003); } // 0.3%
    public BigDecimal getTransactionGasCost() { return BigDecimal.valueOf(0.001); }

    private void initFromJson(EtherdeltaConfig config, JsonObject obj) throws EtherdeltaApiException {
        this.jsonOriginal = obj;
        if (obj.has("trades")) {
            trades.clear();
            JsonArray trades1 = obj.get("trades").getAsJsonArray();
            for (JsonElement elem: trades1) {
                trades.add(new EtherdeltaTrade(config, elem.getAsJsonObject()));
            }
            if (trades.size() > 0) {
                EtherdeltaTrade trade = trades.get(0);
                tokenAddr = trade.getTokenAddr(); // save address of token from the first trade in the market
                if (!config.getTokens().containsKey(getTokenAddr())) {
                    config.discoverToken(getTokenAddr());
                }
                EthereumToken ethereumToken = config.getTokens().get(getTokenAddr());
                tokenSymbol = ethereumToken.getName();
                decimals = ethereumToken.getDecimals();
            } else {
                log.warn("There were no trades on market. Token decimals might not be detected correctly");
            }
        }
        if (obj.has("returnTicker")) {
            JsonObject returnTicker = obj.get("returnTicker").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry: returnTicker.entrySet()) {
                String symbol = entry.getKey();
                JsonObject marketStat = entry.getValue().getAsJsonObject();
                tickers.add(new EtherdeltaReturnTicker(config, symbol, marketStat));
            }
        }
        if (obj.has("orders")) {
            JsonObject orders = obj.get("orders").getAsJsonObject();
            orderbook = new EtherdeltaOrderBook(config, orders);
        }
        // myFunds, myTrades, myOrders
    }

    public EtherdeltaMarket() {
    }

    public EtherdeltaMarket(EtherdeltaConfig config, JsonObject obj) throws EtherdeltaApiException {
        initFromJson(config, obj);
    }

    public EtherdeltaMarket(EtherdeltaConfig config, File file) throws EtherdeltaApiException {
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(file));
            JsonElement object = new JsonParser().parse(jsonReader);
            if (object == null) throw new EtherdeltaApiException("Cannot parse JSON in file " + file.getAbsolutePath());
            initFromJson(config, object.getAsJsonObject());
        } catch (FileNotFoundException e) {
            throw new EtherdeltaApiException(e.getMessage());
        }
    }

    public void dumpJson(File file) {
        if (file == null || jsonOriginal == null) { return; }
        BufferedWriter bw = null;
        FileWriter fw = null;
        log.warn("Trying to save market JSON to {}", file.getAbsoluteFile());
        try {
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(jsonOriginal.toString());
            bw.close();
        } catch (IOException e) {
            log.error("Market JSON not saved {}", e.getMessage());
        }
    }

    public void dump(File file, int nOrderBookLimit) {
        if (file == null) { return; }
        BufferedWriter bw = null;
        FileWriter fw = null;
        log.warn("Trying to save market to {}", file.getAbsoluteFile());
        try {
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write("\r\nTOKEN " + getFullName() + "\r\n");
            bw.write(orderbook.getPlainText( nOrderBookLimit ));
//            bw.write("\r\nTICKERS\r\n==============================\r\n");
//            for (EtherdeltaReturnTicker ticker: tickers) {
//                bw.write(ticker.getPlainText() + "\r\n");
//            }
            bw.close();
            log.warn("Market saved to {}", file.getAbsoluteFile());
        } catch (IOException e) {
            log.error("Market not saved {}", e.getMessage());
        }
    }

    public String getFullName() {
        if (tokenSymbol.equals("UNKNOWN")) {
            return new StringBuffer().append(tokenAddr).toString();
        }
        return "ETH-" + tokenSymbol + "." + getDecimals();
    }

}
