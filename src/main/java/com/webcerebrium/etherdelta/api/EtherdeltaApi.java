package com.webcerebrium.etherdelta.api;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.contract.EtherdeltaContract;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.web3j.protocol.Web3j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

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
    public EtherdeltaMainConfig mainConfig = null;

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
    public EtherdeltaApi() throws EtherdeltaApiException {

        try {
            InputStream resource = this.getClass().getClassLoader().getResourceAsStream("main.json");
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(new InputStreamReader(resource), JsonObject.class);
            this.mainConfig = new EtherdeltaMainConfig(jsonObject);
        } catch (Exception e) {
            log.error("ERROR {}", e.getMessage());
        }

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
}