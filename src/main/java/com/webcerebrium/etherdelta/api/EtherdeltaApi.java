package com.webcerebrium.etherdelta.api;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.contract.EtherdeltaContract;
import com.webcerebrium.etherdelta.datatype.EthereumWallet;
import com.webcerebrium.etherdelta.websocket.EtherdeltaSocketAdapter;
import com.webcerebrium.etherdelta.websocket.EtherdeltaEtherscanSocketAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.web3j.protocol.Web3j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class EtherdeltaApi {

    /**
     * Configuration URL - contains smart DDOS protection
     * and cannot be accessed without proper cookies
     * public String configUrl = "https://etherdelta.com/config/main.json";
     */

    /**
     * Configuration Details
     */
    public EtherdeltaConfig mainConfig = null;

    /**
     * Environment configuration
     */
    public EtherdeltaEnvConfig envConfig = new EtherdeltaEnvConfig();

    /**
     * Wallet, connected for trasing
     */
    public EthereumWallet wallet = null;
    /**
     * Guava Class Instance for URL escaping
     */
    private Escaper esc = UrlEscapers.urlFormParameterEscaper();

    /**
     * Main API constructor, starts with remote trequest to main configuration JSON
     * @throws EtherdeltaApiException
     */
    public EtherdeltaApi() {
    }

    /**
     * Connecting wallet for trading operations - using environment variables, VM options or resource file
     * @return same object to maintain same interface
     * @throws EtherdeltaApiException in case of credentials error
     */
    public EtherdeltaApi initWallet() throws EtherdeltaApiException {
        EtherdeltaEnvConfig config = new EtherdeltaEnvConfig();
        this.wallet = new EthereumWallet(
                config.getVariable("ETHERDELTA_WALLET_PRIVATE_KEY"),
                config.getVariable("ETHERDELTA_WALLET_ADDRESS")
        ).validate();
        return this;
    }

    public EtherdeltaContract getSmartContract(Web3j web3j) throws EtherdeltaApiException {
        if (wallet == null) {
            throw new EtherdeltaApiException("Please initialize wallet first");
        }
        return this.mainConfig.getSmartContract(web3j, wallet);
    }

    /**
     * Connecting to web socket
     * @param adapter for listening of socket
     * @throws EtherdeltaApiException in case of any error
     */
    public void connectToSocket(EtherdeltaSocketAdapter adapter) throws EtherdeltaApiException{
        adapter.setConfig(this.getMainConfig());

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        adapter.setCountdown(countDownLatch);

        URI uri = null;
        try {
            uri = new URI(this.mainConfig.getSocketServer());
        } catch (URISyntaxException e) {
            throw new EtherdeltaApiException(e.getMessage());
        }
        log.info("Connecting to {}", uri);

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true); // The magic for proper SSL certificate

        WebSocketClient client = new WebSocketClient(sslContextFactory);
        client.setMaxIdleTimeout(20000);
        client.getPolicy().setMaxTextMessageBufferSize(1024 * 1024 * 20);
        client.getPolicy().setMaxTextMessageSize(1024 * 1024 * 10);

        try {
            client.start();
            Future<Session> fut = client.connect(adapter, uri);
            countDownLatch.await();
            fut.get().close();
            client.stop();
        } catch (Exception e) {
            throw new EtherdeltaApiException( e.getMessage());
        }
    }

    public void connectToEthercanSocket(EtherdeltaEtherscanSocketAdapter adapter) throws EtherdeltaApiException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        adapter.setCountdown(countDownLatch);
        URI uri = null;
        try {
            uri = new URI("wss://socket.etherscan.io/wshandler?EIO=3&transport=websocket");
        } catch (URISyntaxException e) {
            throw new EtherdeltaApiException(e.getMessage());
        }
        log.info("Connecting to {}", uri);

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true); // The magic for proper SSL certificate

        WebSocketClient client = new WebSocketClient(sslContextFactory);
        client.setMaxIdleTimeout(20000);
        client.getPolicy().setMaxTextMessageBufferSize(1024 * 1024 * 20);
        client.getPolicy().setMaxTextMessageSize(1024 * 1024 * 10);

        try {
            client.start();
            Future<Session> fut = client.connect(adapter, uri);

            // keep connection alive, by sending PING every 18 seconds
            Thread.sleep(1000);
            JsonObject jsonSubscribe = new JsonObject();
            jsonSubscribe.addProperty("event", "txlist");
            jsonSubscribe.addProperty("address", mainConfig.getLastAddress());
            adapter.sendJson(jsonSubscribe);

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate((Runnable) () -> {
                JsonObject jsonPing = new JsonObject();
                jsonPing.addProperty("event", "ping");
                adapter.sendJson(jsonPing);
            }, 10, 18, TimeUnit.SECONDS);

            countDownLatch.await();
            fut.get().close();
            client.stop();
        } catch (Exception e) {
            throw new EtherdeltaApiException( e.getMessage());
        }
    }
}
