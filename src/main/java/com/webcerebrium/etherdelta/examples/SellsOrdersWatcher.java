package com.webcerebrium.etherdelta.examples;

import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaSocketAdapter;
import com.webcerebrium.etherdelta.datatype.EtherdeltaFund;
import com.webcerebrium.etherdelta.datatype.EtherdeltaMarket;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrder;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrderSide;
import com.webcerebrium.etherdelta.datatype.EtherdeltaTrade;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SellsOrdersWatcher extends EtherdeltaSocketAdapter {

    String user = "";
    public void setUser(String user) { this.user = user; };

    Set<String> marketsRequested = new HashSet<>();
    Map<String, EtherdeltaMarket> marketsCache = new HashMap<>();

    @Override
    protected void onOrder(String command, EtherdeltaOrder order) {
        if (order.getSide() == EtherdeltaOrderSide.SELL) {
            log.info("onOrder {}", order.toString());

            if (!marketsRequested.contains(order.getTokenGive())) {
                JsonObject payload = new JsonObject();
                payload.addProperty("token", order.getTokenGive());
                payload.addProperty("user", user);

                marketsRequested.add(order.getTokenGive());
                this.emitEvent("getMarket", payload);
            }
        }
    }

    @Override
    protected void onFunding(String command, EtherdeltaFund fund) {
        // log.info("onFunding {}", fund.toString());
        // do nothing
    }

    @Override
    protected void onTrade(String command, EtherdeltaTrade trade) {
        // log.info("onTrade {}", trade.toString());
        // do nothing
    }

    @Override
    protected void onMarket(String tokenAddr, EtherdeltaMarket market) {
        marketsCache.put(tokenAddr, market);
    }

    private void reset() {
        // on any error or socket closing - we need to do that once again...
        marketsRequested.clear();
        marketsCache.clear();
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        reset();
        super.onWebSocketClose(statusCode, reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        reset();
        super.onWebSocketError(cause);
    }
}

