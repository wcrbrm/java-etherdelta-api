package com.webcerebrium.etherdelta.api;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

@Slf4j
public class EtherdeltaSocketAdapter extends WebSocketAdapter {
    @Override
    public void onWebSocketConnect(Session sess) {
        log.debug("onWebSocketConnect: {}", sess);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("onWebSocketError: {}", cause);
    }

    @Override
    public void onWebSocketText(String message) {
        // 42["orders", onOrders(JsonObject
        log.debug("onWebSocketText message={}", message);
    }
    // public abstract void onMessage(BinanceEventAggTrade event) throws BinanceApiException;
}
