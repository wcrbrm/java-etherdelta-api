package com.webcerebrium.etherdelta.api;

import com.webcerebrium.etherdelta.datatype.EthereumToken;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class EtherdeltaDefaultConfig extends EtherdeltaConfig {

    @Getter
    @Setter
    EtherdeltaEnvConfig envConfig = new EtherdeltaEnvConfig();

    @Getter
    @Setter
    public Map<String, EthereumToken> tokens = new HashMap<>();

    public EtherdeltaDefaultConfig() {
    }

    @Override
    public String getSocketServer() throws EtherdeltaApiException {
        return "https://socket.etherdelta.com";
    }

    @Override
    public String getLastAddress() throws EtherdeltaApiException {
        return "0x8d12a197cb00d4747a1fe03395095ce2a5cc6819";
    }

    @Override
    public BigInteger getGasPrice() {
        return BigInteger.valueOf(4000000000L);
    }

    @Override
    public BigInteger getGasOrder() {
        return BigInteger.valueOf(250000L);
    }

//    "gasApprove": 250000,
//    "gasDeposit": 250000,
//    "gasWithdraw": 250000,
//    "gasTrade": 250000,
//    "gasOrder": 250000,
//    "minOrderSize": 0.001,



}
