package com.webcerebrium.etherdelta.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.nio.channels.ClosedChannelException;

@Slf4j
public class EtherdeltaSocketAdapter extends WebSocketAdapter {

    @Override
    public void onWebSocketConnect(Session sess) {
        log.debug("CONNECTED");
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            log.error("DISCONNECTED");
            System.exit(1);
        } else {
            log.error("ON ERROR: {}", cause.getMessage());
            System.exit(1);
        }
    }

    private boolean startWithDigit(String s) {
        if (s.length() == 0) return false;
        char c = s.charAt(0);
        return (c >= '0' && c <= '9');
    }

    @Override
    public void onWebSocketText(String message) {
        String noNumberAtStart = message;
        String code = "";
        while (startWithDigit(noNumberAtStart)) {
            code += noNumberAtStart.charAt(0);
            noNumberAtStart = noNumberAtStart.substring(1);
        }
        if (!Strings.isNullOrEmpty(noNumberAtStart)) {
            Gson gson = new Gson();

            String command = "";
            JsonArray jsonElements = null;
            try {
                JsonElement jsonElement = gson.fromJson(noNumberAtStart, JsonElement.class);
                if (jsonElement.isJsonObject()) {
                    // usually object instead of array comed at the beginning
                    // it has no useful information
                    log.info("RECEIVED {}", jsonElement.toString());
                    return;
                }
                jsonElements = jsonElement.getAsJsonArray();
                command = jsonElements.get(0).getAsString();
            } catch (JsonSyntaxException ex) {
                log.warn("JSON SYNTAX ERROR {}", ex.getMessage());
                log.info("MESSAGE='{}'", noNumberAtStart);
                return;
            }

            if (command.equals("returnTicker")) {
                JsonObject mapTickers = jsonElements.get(1).getAsJsonObject();
                log.debug("on RETURNTICKER, {} symbols", mapTickers.entrySet().size());
            } else if (command.equals("funds")) {
                JsonArray list = jsonElements.get(1).getAsJsonArray();
                log.debug("on FUNDS, {} records", list.size());
            } else if (command.equals("myFunds")) {
                JsonArray list = jsonElements.get(1).getAsJsonArray();
                log.debug("on MYFUNDS, {} records", list.size());
            } else if (command.equals("trades")) {
                JsonArray list = jsonElements.get(1).getAsJsonArray();
                log.debug("on TRADES, {} records", list.size());
            } else if (command.equals("myTrades")) {
                JsonArray list = jsonElements.get(1).getAsJsonArray();
                log.debug("on MYTRADES, {} records", list.size());
            } else if (command.equals("orders")) {
                JsonObject obj = jsonElements.get(1).getAsJsonObject();
                JsonArray buys = obj.get("buys").getAsJsonArray();
                JsonArray sells = obj.get("sells").getAsJsonArray();
                log.debug("on ORDERS, {} buys, {} sells", buys.size(), sells.size());
            } else if (command.equals("myOrders")) {
                JsonObject obj = jsonElements.get(1).getAsJsonObject();
                JsonArray buys = obj.get("buys").getAsJsonArray();
                JsonArray sells = obj.get("sells").getAsJsonArray();
                log.debug("on MYORDERS, {} buys, {} sells", buys.size(), sells.size());
            } else {
                log.warn("Unknown Socket Event, code={} message={}", code, jsonElements.toString());
            }
        }
    }
    // public abstract void onMessage(BinanceEventAggTrade event) throws BinanceApiException;
}
