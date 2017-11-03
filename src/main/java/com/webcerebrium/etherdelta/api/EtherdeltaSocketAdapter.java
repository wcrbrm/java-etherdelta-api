package com.webcerebrium.etherdelta.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.webcerebrium.etherdelta.datatype.EtherdeltaFund;
import com.webcerebrium.etherdelta.datatype.EtherdeltaMarket;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrder;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrderBook;
import com.webcerebrium.etherdelta.datatype.EtherdeltaTrade;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.nio.channels.ClosedChannelException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class EtherdeltaSocketAdapter extends WebSocketAdapter {

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

    /**
     * Method for overriding in inherited class
     * @param command 'orders' or 'myOrders'
     * @param order object of the order
     */
    protected abstract void onOrder(String command, EtherdeltaOrder order);

    /**
     * Method for overriding in inherited class
     * @param command 'funding' or 'myFunding'
     * @param fund object of the funding record
     */
    protected abstract void onFunding(String command, EtherdeltaFund fund);

    /**
     * Method for overriding in inherited class
     * @param command 'trade' or 'myTrade'
     * @param trade object of the trading record
     */
    protected abstract void onTrade(String command, EtherdeltaTrade trade);

    /**
     * Method for overriding in inherited class
     * @param market object of the market
     */
    protected abstract void onMarket(EtherdeltaMarket market);

    @Override
    public void onWebSocketText(String message) {
        String noNumberAtStart = message;
        // log.debug("onWebSocketText {}", message);
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
                if (!jsonElement.isJsonArray()) {
                    log.info("RECEIVED {}", jsonElement.toString());
                    if (jsonElement.toString().equals("\"Fail\"")) countdown.countDown();
                    // usually object instead of array comes at the beginning
                    // it has no useful information
                    return;
                }
                jsonElements = jsonElement.getAsJsonArray();
                command = jsonElements.get(0).getAsString();
            } catch (JsonSyntaxException ex) {
                log.warn("JSON SYNTAX ERROR {}", ex.getMessage());
                log.info("MESSAGE='{}'", noNumberAtStart);
                return;
            }

            try {
                onCommand(code, command, jsonElements);
            } catch (EtherdeltaApiException e) {
                log.warn("Etherdelta Data parsing exception: {}", e.getMessage());
            }

        }
    }

    private JsonObject requireObject(String command, JsonArray jsonElements) throws EtherdeltaApiException {
        if (jsonElements == null || jsonElements.size() == 0 || !jsonElements.get(1).isJsonObject()) {
            log.warn("Parsing command error {}, expected json object {}", command, jsonElements.toString());
            throw new EtherdeltaApiException("Parsing command error " + command + ", expected json object");
        }
        return jsonElements.get(1).getAsJsonObject();
    }

    private JsonArray requireArray(String command, JsonArray jsonElements) throws EtherdeltaApiException {
        if (jsonElements == null || jsonElements.size() == 0 || !jsonElements.get(1).isJsonArray()) {
            log.warn("Parsing command error {}, expected json array {}", command, jsonElements.toString());
            throw new EtherdeltaApiException("Parsing command error " + command + ", expected json array");
        }
        return jsonElements.get(1).getAsJsonArray();
    }

    private void onCommand(String messageCode, String command, JsonArray jsonElements) throws EtherdeltaApiException {
        if (command.equals("market")) {
            JsonObject market = requireObject(command, jsonElements);
            Set<String> keys = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : market.entrySet()) {
                keys.add(entry.getKey());
            }
            log.warn("Event: {} {}", command, keys.toString() ); // important to support
            onMarket(new EtherdeltaMarket(config, market));
        } else if (command.equals("returnTicker")) {
            JsonObject mapTickers = requireObject(command, jsonElements);
            log.debug("Event: {}, {} symbols", command, mapTickers.entrySet().size());
            // not supported yet
        } else if (command.equals("funds") || command.equals("myFunds")) {
            JsonArray list = requireArray(command, jsonElements);
            log.debug("Event: {}, {} funding records", command, list.size());
            for (JsonElement elem: list) {
                onFunding(command, new EtherdeltaFund(config, elem.getAsJsonObject()));
            }
        } else if (command.equals("trades") || command.equals("myTrades")) {
            JsonArray list = requireArray(command, jsonElements);
            log.debug("Event: {}, {} trades", command, list.size());
            for (JsonElement elem : list) {
                onTrade(command, new EtherdeltaTrade(config, elem.getAsJsonObject()));
            }
        } else if (command.equals("orders") || command.equals("myOrders")) {
            JsonObject obj = requireObject(command, jsonElements);
            EtherdeltaOrderBook ob = new EtherdeltaOrderBook(config, obj);
            List<EtherdeltaOrder> allOrders = ob.getAllOrders();
            log.debug("Event: {}, {} orders buys:{} sells:{}",
                    command, allOrders.size(), ob.getBuys().size(), ob.getSells().size());
            for (EtherdeltaOrder order: allOrders) {
                onOrder(command, order);
            }
        } else {
            log.warn("Unknown Socket Event, code={} message={}", messageCode, jsonElements.toString());
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
