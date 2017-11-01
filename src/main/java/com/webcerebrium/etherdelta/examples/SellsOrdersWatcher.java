package com.webcerebrium.etherdelta.examples;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.api.EtherdeltaEnvConfig;
import com.webcerebrium.etherdelta.api.EtherdeltaSocketAdapter;
import com.webcerebrium.etherdelta.datatype.EtherdeltaFund;
import com.webcerebrium.etherdelta.datatype.EtherdeltaMarket;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrder;
import com.webcerebrium.etherdelta.datatype.EtherdeltaOrderSide;
import com.webcerebrium.etherdelta.datatype.EtherdeltaTrade;
import com.webcerebrium.slack.Notification;
import com.webcerebrium.slack.NotificationException;
import com.webcerebrium.slack.SlackMessage;
import com.webcerebrium.slack.SlackMessageAttachment;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SellsOrdersWatcher extends EtherdeltaSocketAdapter {

    EtherdeltaEnvConfig env = new EtherdeltaEnvConfig();

    String user = "";
    public void setUser(String user) { this.user = user; };

    Set<String> marketsRequested = new HashSet<>();
    Set<String> marketsUnderObservation = new HashSet<>();

    private void requestUpdateOn(String tradedToken) {
        if (!marketsRequested.contains(tradedToken)) {
            JsonObject payload = new JsonObject();
            payload.addProperty("token", tradedToken);
            payload.addProperty("user", user);

            marketsRequested.add(tradedToken);
            this.emitEvent("getMarket", payload);
        }
    }

    @Override
    protected void onOrder(String command, EtherdeltaOrder order) {
        String tradedToken = order.getSide() == EtherdeltaOrderSide.SELL ? order.getTokenGive() : order.getTokenGet();
        requestUpdateOn(tradedToken);
    }

    @Override
    protected void onFunding(String command, EtherdeltaFund fund) {
        // log.info("onFunding {}", fund.toString());
        // do nothing
    }

    @Override
    protected void onTrade(String command, EtherdeltaTrade trade) {
        String tradedToken = trade.getTokenAddr();
        requestUpdateOn(tradedToken);
    }

    @Override
    protected void onMarket(EtherdeltaMarket market) {
        if (!Strings.isNullOrEmpty(market.getTokenAddr())) {

            market.dump(new File("/tmp/" + market.getFullName() + ".txt"), 10);
            market.dumpJson(new File("/tmp/" + market.getFullName() + ".json"));

            marketsRequested.remove(market.getTokenAddr());

            BigDecimal bestSell = market.getOrderbook().getBestSellPrice();
            BigDecimal bestBuy = market.getOrderbook().getBestBuyPrice();
            if (bestBuy != null && bestSell != null && bestBuy.compareTo(bestSell) > 0 ) {

                BigDecimal bestSellQty = market.getOrderbook().getBestSellQty();
                BigDecimal bestBuyQty = market.getOrderbook().getBestBuyQty();

                SlackMessage message = new SlackMessage();
                StringBuffer title =  new StringBuffer()
                        .append("Etherdelta ").append(market.getFullName())
                        .append(" SELL: ").append(bestSellQty).append("@`").append(bestSell).append("`")
                        .append(" BUY: ").append(bestBuyQty).append("@`").append(bestBuy).append("`");

                String body = "```" + market.getOrderbook().getPlainText(3) + "```";
                SlackMessageAttachment attach = new SlackMessageAttachment(title.toString(), body, "#c0FFF0");
                attach.addMarkdown(ImmutableSet.of("title", "text"));
                message.getAttachments().add(attach);
                log.info("Sending notification {}", title);
                try {
                    String channelUrl = env.getVariable("CHANNEL_BELOW_AVERAGE");
                    (new Notification(channelUrl)).send(message);
                } catch (NotificationException e) {
                    log.error("Notification error {}", e.getMessage());
                }
            } else {
                // we do not have open opportunity on that market anymore

            }
        }
    }

    private void reset() {
        // on any error or socket closing - we need to do that once again...
        marketsRequested.clear();
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

