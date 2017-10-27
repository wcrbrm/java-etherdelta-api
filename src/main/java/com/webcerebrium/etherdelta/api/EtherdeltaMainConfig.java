package com.webcerebrium.etherdelta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.contract.EtherdeltaContract;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
public class EtherdeltaMainConfig {

    public JsonObject jsonConfig = null;

    /**
     * Map of Tokens, parsed from JSON file
     * Key of that map is token Ethereum address
     */
    public Map<String, EthereumToken> tokens = null;

    /**
     * Initializing from JSON configuration
     * @param jsonConfig configuration to read
     */
    public EtherdeltaMainConfig(JsonObject jsonConfig) {
        this.jsonConfig = jsonConfig;
        this.initTokens();
    }

    /**
     * Iinitializing from Remote URL
     * @param configUrl remote URL to read
     * @throws EtherdeltaApiException in case of any error
     */
    public EtherdeltaMainConfig(String configUrl) throws EtherdeltaApiException {
        this.jsonConfig = new EtherdeltaRequest(configUrl).read().asJsonObject();
        this.initTokens();
    }

    private void initTokens() {
        if (this.jsonConfig.has("tokens") && this.jsonConfig.get("tokens").isJsonArray()) {
            JsonArray arrTokens =  this.jsonConfig.get("tokens").getAsJsonArray();
            tokens = new HashMap<String, EthereumToken>();

            for (JsonElement entry: arrTokens) {
                JsonObject asJsonObject = entry.getAsJsonObject();
                EthereumToken ethereumToken = new EthereumToken(asJsonObject);
                tokens.put(ethereumToken.getAddress(), ethereumToken);
            }
        }
    }

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

    public EtherdeltaContract getSmartContract(Web3j web3j, EthereumWallet wallet) throws EtherdeltaApiException {
        log.info("Loading smart contract at address {} gasPrice={} gasLimit={}", this.getLastAddress(), getGasPrice(), getGasLimit());
        return EtherdeltaContract.load(
                this.getLastAddress(), web3j, wallet.getWeb3Credentials(),
                getGasPrice(), getGasLimit());
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
    public BigInteger getGasLimit() {
        return jsonConfig.get("gasOrder").getAsBigInteger();
    }
}