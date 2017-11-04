package com.webcerebrium.etherdelta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.webcerebrium.etherdelta.datatype.EthereumToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EtherdeltaMainConfig  extends EtherdeltaConfig {

    @Getter @Setter
    public JsonObject jsonConfig = null;

    @Getter @Setter
    EtherdeltaEnvConfig envConfig = new EtherdeltaEnvConfig();

    /**
     * Map of Tokens, parsed from JSON file
     * Key of that map is token Ethereum address
     */
    @Getter
    @Setter
    public Map<String, EthereumToken> tokens = null;

    public EtherdeltaMainConfig() {
    }
    /**
     * Initializing from JSON configuration
     * @param jsonConfig configuration to read
     */
    public EtherdeltaMainConfig(JsonObject jsonConfig) {
        this.jsonConfig = jsonConfig;
        this.initTokens();
    }

    private EtherdeltaConfig initTokens() {
        if (this.jsonConfig.has("tokens") && this.jsonConfig.get("tokens").isJsonArray()) {
            JsonArray arrTokens =  this.jsonConfig.get("tokens").getAsJsonArray();
            tokens = new HashMap<>();

            for (JsonElement entry: arrTokens) {
                JsonObject asJsonObject = entry.getAsJsonObject();
                EthereumToken ethereumToken = new EthereumToken(asJsonObject);
                tokens.put(ethereumToken.getAddress(), ethereumToken);
            }
        }
        return this;
    }

    @Override
    public String getSocketServer() throws EtherdeltaApiException  {
        if (!jsonConfig.has("socketServer") || !jsonConfig.get("socketServer").isJsonArray()) {
            throw new EtherdeltaApiException("Expected to have socketServer in main config");
        }
        JsonArray urls = jsonConfig.get("socketServer").getAsJsonArray();
        if (urls.size() < 1) {
            throw new EtherdeltaApiException("Expected at least one socketServer listed");
        }
        // we could actually choose random one from this array
        String timestamp = String.valueOf(new Date().getTime());
        return urls.get(0).getAsString()
                 .replaceAll("https://", "wss://")
                + "/socket.io/?EIO=3&transport=websocket&t=" + timestamp;
    }

    @Override
    public String getLastAddress() throws EtherdeltaApiException {
        if (!jsonConfig.has("contractEtherDeltaAddrs") || !jsonConfig.get("contractEtherDeltaAddrs").isJsonArray()) {
            throw new EtherdeltaApiException("Expected to have contractEtherDeltaAddrs in main config");
        }
        JsonArray addresses = jsonConfig.get("contractEtherDeltaAddrs").getAsJsonArray();
        if (addresses.size() < 1) {
            throw new EtherdeltaApiException("Expected at least one address contractEtherDeltaAddrs");
        }
        JsonObject first = addresses.get(0).getAsJsonObject();
        if (first != null && first.has("addr") && !first.get("addr").isJsonPrimitive()) {
            throw new EtherdeltaApiException("first address in contractEtherDeltaAddrs should contain addr");
        }
        return first.get("addr").getAsString();
    }



    /**
     * Getting Gas price from stored JSON configuration
     * @return ethereum gas price
     */
    public BigInteger getGasPrice() {
        return jsonConfig.get("ethGasPrice").getAsBigInteger();
    }

    /**
     * Getting Gas limit from stored JSON configuration
     * @return gas limit
     */
    public BigInteger getGasOrder() {
        return jsonConfig.get("gasOrder").getAsBigInteger();
    }

    /**
     *
     * @param resourcePath name of the resource
     * @return this object for fluent interface
     */
    public EtherdeltaConfig fromResource(String resourcePath) throws EtherdeltaApiException {
        EtherdeltaConfig config = new EtherdeltaMainConfig();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) throw new EtherdeltaApiException("Resource could not be found " + resourcePath);
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        JsonElement configObject = (new JsonParser()).parse(reader);
        this.jsonConfig = configObject.getAsJsonObject();
        return this.initTokens();
    }
}
