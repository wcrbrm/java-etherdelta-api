package com.webcerebrium.etherdelta.api;

import com.webcerebrium.etherdelta.contract.EtherdeltaContract;
import com.webcerebrium.etherdelta.datatype.EthereumToken;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Map;

@Slf4j
public abstract class EtherdeltaConfig {

    public abstract String getSocketServer() throws EtherdeltaApiException;
    public abstract String getLastAddress() throws EtherdeltaApiException;
    public abstract BigInteger getGasPrice();
    public abstract BigInteger getGasOrder();
    public abstract Map<String, EthereumToken> getTokens();

    public EtherdeltaContract getSmartContract(Web3j web3j, EthereumWallet wallet) throws EtherdeltaApiException {
        log.info("Loading smart contract at address {} gasPrice={} gasOrder={}", this.getLastAddress(), getGasPrice(), getGasOrder());
        return EtherdeltaContract.load(
                this.getLastAddress(), web3j, wallet.getWeb3Credentials(),
                getGasPrice(), getGasOrder());
    }
}
