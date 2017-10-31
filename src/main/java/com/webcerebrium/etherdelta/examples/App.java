package com.webcerebrium.etherdelta.examples;

import com.webcerebrium.etherdelta.api.EtherdeltaApi;
import com.webcerebrium.etherdelta.api.EtherdeltaApiException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;

import java.util.concurrent.ExecutionException;

@Slf4j
public class App {
    public static void main(String[] args) throws EtherdeltaApiException, ExecutionException, InterruptedException {

        EtherdeltaApi api = new EtherdeltaApi().initWallet();
        log.info("{} known tokens", api.getMainConfig().getTokens().size());

//      Web3j web3 = Web3j.build(new HttpService("https://mainnet.infura.io/Ky03pelFIxoZdAUsr82w"));  // defaults to http://localhost:8545/
//      Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
//      String clientVersion = web3ClientVersion.getWeb3ClientVersion();
//      log.info("clientVersion = {}", clientVersion);

//      EtherdeltaContract smartContract = api.getSmartContract(web3);
//      log.info("FEE TAKE={}", smartContract.feeTake().get().getValue());
//      log.info("FEE MAKE={}", smartContract.feeMake().get().getValue());
//      log.info("FEE REBATE={}", smartContract.feeRebate().get().getValue());

        // Address tokenAddress = new Address("0x0d88ed6e74bbfd96b831231638b66c05571e824f");
        // log.info("ORDERS OF AVT = {}", smartContract.orders(tokenAddress, new Bytes32("".getBytes())).get());

        log.info("Using socket server {}", api.getMainConfig().getSocketServer());

        WebSocketPolicy.newServerPolicy().setMaxTextMessageSize( 1024*1024*10 );
        SellsOrdersWatcher adapter = new SellsOrdersWatcher();
        adapter.setUser(api.getWallet().getAddress());
        do {
            api.connectToSocket(adapter);
            Thread.sleep(2000); // wait 2 seconds before reconnect
        } while (true);
    }
}
