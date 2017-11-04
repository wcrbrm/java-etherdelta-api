package com.webcerebrium.etherdelta.api;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.webcerebrium.etherdelta.datatype.EthereumToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
public class EthereumTokenEtherscanInfo {

    private String symbol;
    private Long decimals = -1L;
    private String name;
    private String error;

    private String address;
    private Long netId = 0L;

    private String image = "";
    private String reputation = "";
    private Long totalSupply = -1L;

    public EthereumTokenEtherscanInfo(String address) {
        this.address = address;
    }

    private String getEtherscanRoot() {
        if (netId == 4) return "https://rinkeby.etherscan.io";
        if (netId == 3) return "https://ropsten.etherscan.io";
        if (netId == 2) return "https://kovan.etherscan.io";
        return "https://etherscan.io";
    }

    public void parse(String content) {
        Pattern patternDecimals = Pattern.compile("Decimals..nbsp;\\s*</td>\\s*<td>\\s*(.+)\\s*<", Pattern.MULTILINE);
        Matcher matcherDecimals = patternDecimals.matcher(content);
        if (matcherDecimals.find()) {
            decimals = Long.valueOf(matcherDecimals.group(1).trim());
        } else {
            log.error("Decimals not found for token at address {}", this.getAddress());
        }

        Pattern patternImage = Pattern.compile("<img src='/token/images/([^']+)' style='margin-top: -3px' /> TOKEN", Pattern.MULTILINE);
        Matcher matcherImage = patternImage.matcher(content);
        if (matcherImage.find()) {
            image = getEtherscanRoot() + "/token/images/" + matcherImage.group(1).trim();
        } else {
            log.debug("Image not found for token at address {}", this.getAddress());
        }

        Pattern patternName = Pattern.compile("<li class=\"active\">([^<]+)</li>", Pattern.MULTILINE);
        Matcher matcherName = patternName.matcher(content);
        if (matcherName.find()) {
            name = matcherName.group(1);
        }
        Pattern patternTotalSupply = Pattern.compile("<td>Total Supply:\\s*</td>\\s*<td>\\s*(.+)\\s*<", Pattern.MULTILINE);
        Matcher matcherTotalSupply = patternTotalSupply.matcher(content);
        if (matcherTotalSupply.find()) {
            String totalSupplyValue = matcherTotalSupply.group(1);
            Pattern patternSymbol = Pattern.compile("(\\w+)$");
            Matcher matcherSymbol = patternSymbol.matcher(totalSupplyValue);
            if (matcherSymbol.find()) {
                symbol = matcherSymbol.group(1);
            }
            totalSupplyValue = totalSupplyValue.replaceAll("\\((.+)\\)", "").replaceAll(",", "").replaceAll(" " + symbol, "").trim();
            log.debug("Total Supply {}", totalSupplyValue);
        }

        Pattern patternReputation = Pattern.compile("etherscan-token-reputation-system'([^>]+)>(.+)<i ", Pattern.MULTILINE);
        Matcher matcherReputation = patternReputation.matcher(content);
        if (matcherReputation.find()) {
            reputation = matcherReputation.group(2).replaceAll("\\<.*?\\>", "").trim();
        }
        if (Strings.isNullOrEmpty(name) && !Strings.isNullOrEmpty(symbol)) {
            name = symbol;
        }
    }


    public String getRemoteContent(String remoteUrl) {
        try {
            return new EtherdeltaRequest(remoteUrl).setLog(false).read().getLastResponse();
        } catch( EtherdeltaApiException e ) {
            this.error = e.getMessage();
            return "";
        }
    }

    public void discover(EtherdeltaEnvConfig envConfig) {
        String cacheDir = envConfig.getTokenCacheDir();
        File cacheFile = new File(cacheDir + "/" + this.getAddress() + ".html");
        if (cacheFile.exists()) {
            try {
                parse(Files.toString(cacheFile, Charsets.UTF_8));
            } catch (IOException e) {
                log.info("Token file reading error {}", e.getMessage());
            }
        } else {
            String sUrl = getEtherscanRoot() + "/token/" + this.getAddress();
            String content = getRemoteContent(sUrl);
            try {
                Files.write(content.getBytes(), cacheFile);
            } catch (IOException e) {
                log.info("Token caching Error {}", e.getMessage());
            }
            parse(content);
        }
    }

    public EthereumToken getToken() {
        EthereumToken ethereumToken = new EthereumToken();
        ethereumToken.setAddress(this.getAddress());
        ethereumToken.setDecimals(this.getDecimals().intValue());
        ethereumToken.setName(this.getName());
        return ethereumToken;
    }
}
