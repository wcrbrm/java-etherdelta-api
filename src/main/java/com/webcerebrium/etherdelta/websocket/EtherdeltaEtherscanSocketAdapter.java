package com.webcerebrium.etherdelta.websocket;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import com.webcerebrium.etherdelta.api.EtherdeltaConfig;
import com.webcerebrium.etherdelta.datatype.EtherdeltaEtherscanTxlist;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class EtherdeltaEtherscanSocketAdapter extends WebSocketAdapter {

    @Getter
    @Setter
    EtherdeltaConfig config = null;

    @Getter
    @Setter
    CountDownLatch countdown = null;

    @Getter
    @Setter
    Session sess = null;

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.error("ON CLOSE code={}, reason={}", statusCode, reason);
        if (statusCode == 1006) {
            try { Thread.sleep(10*1000); } catch (InterruptedException e) {}
        }
        // see https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent
        // for more status codes
        super.onWebSocketClose(statusCode, reason);
        countdown.countDown();
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        this.sess = sess;
        log.debug("CONNECTED. REMOTE={}", sess.getRemote());
        sess.setIdleTimeout(20000);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof UpgradeException) {
            log.error("UPGRADE ERROR: {}", cause.getMessage());
            countdown.countDown();
        } else if (cause instanceof ClosedChannelException) {
            log.error("DISCONNECTED");
            countdown.countDown();
        } else {
            log.error("ON ERROR: {}", cause.getMessage());
            countdown.countDown();
        }
    }

    private boolean startWithDigit(String s) {
        if (s.length() == 0) return false;
        char c = s.charAt(0);
        return (c >= '0' && c <= '9');
    }

    public abstract void onTxList(EtherdeltaEtherscanTxlist txlist);

    @Override
    public void onWebSocketText(String message) {
        // String noNumberAtStart = message;
        log.debug("onWebSocketText {}", message);
        JsonObject eventJson = new JsonParser().parse(message).getAsJsonObject();
        try {
            String event = eventJson.get("event").getAsString();
            if (event.equals("txlist")) {
                this.onTxList(new EtherdeltaEtherscanTxlist(eventJson.getAsJsonObject()));
            }
        } catch (EtherdeltaApiException e) {
            e.printStackTrace();
        }

    }

    public void sendJson(JsonObject payload) {
        try {
            log.debug("Sending {}", payload.toString());
            RemoteEndpoint remote = this.getSess().getRemote();
            remote.sendString(payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void emitEvent(String message, JsonObject payload) {
        try {
            log.debug("Emitting '{}' {}", message, payload.toString());
            RemoteEndpoint remote = this.getSess().getRemote();
            JsonArray msg = new JsonArray();
            msg.add(message);
            msg.add(payload);
            remote.sendString("42" + msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
