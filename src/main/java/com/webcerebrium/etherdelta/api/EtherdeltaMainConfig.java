package com.webcerebrium.etherdelta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.webcerebrium.etherdelta.contract.EtherdeltaContract;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Data
public class EtherdeltaMainConfig {

    public JsonObject jsonConfig = null;

    /**
     * List of Tokens, parsed from JSON file
     */
    public List<EthereumToken> tokens = null;

    /**
     * public String socketURL = "https://socket.etherdelta.com";
     * public Long gasLimit = 150000L;
     * public Long gasPrice = 4000000000L;
     */

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
            tokens = new LinkedList<>();

            for (JsonElement entry: arrTokens) {
                JsonObject asJsonObject = entry.getAsJsonObject();
                if (asJsonObject.get("name").getAsString().equals("ETH")) continue;
                tokens.add(new EthereumToken(asJsonObject));
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
        return urls.get(0).getAsString().replaceAll("https://", "wss://") + "/socket.io/?EIO=3&transport=websocket";
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

    public BigInteger getGasPrice() {
        return jsonConfig.get("ethGasPrice").getAsBigInteger();
    }

    public BigInteger getGasLimit() {
        return jsonConfig.get("gasOrder").getAsBigInteger();
    }
}
