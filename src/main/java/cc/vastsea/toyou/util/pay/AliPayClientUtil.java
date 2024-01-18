package cc.vastsea.toyou.util.pay;

import com.alipay.api.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class AliPayClientUtil {

    private String privateKey;
    private String appCert;
    private String alipayCert;
    private String alipayRootCert;

    private final AlipayClient alipayClient;

    private CertAlipayRequest alipayConfig(String url){

        try {
            this.privateKey = new String(Files.readAllBytes(Paths.get("./profile/alipay/privateKey.txt")));
            this.appCert = Paths.get("./profile/alipay/appCertPublicKey.crt").toString();
            this.alipayCert  = Paths.get("./profile/alipay/alipayCertPublicKey.crt").toString();
            this.alipayRootCert  = Paths.get("./profile/alipay/alipayRootCert.crt").toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CertAlipayRequest certAlipayRequest = new CertAlipayRequest();
        certAlipayRequest.setServerUrl(url);
        certAlipayRequest.setAppId("2021004125628045");
        certAlipayRequest.setFormat("JSON");
        certAlipayRequest.setCharset("utf-8");
        certAlipayRequest.setSignType("RSA2");
        certAlipayRequest.setPrivateKey(privateKey);
        certAlipayRequest.setCertPath(appCert);
        certAlipayRequest.setAlipayPublicCertPath(alipayCert);
        certAlipayRequest.setRootCertPath(alipayRootCert);

        return certAlipayRequest;
    }

    public AliPayClientUtil(String url){
        try {
            this.alipayClient = new DefaultAlipayClient(this.alipayConfig(url));
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
    }

    public AlipayClient getAlipayClient() {
        return alipayClient;
    }
}
