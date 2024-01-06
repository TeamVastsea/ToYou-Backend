package cc.vastsea.toyou.util.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class AliPayClientUtil {

    private String privateKey;
    private String publicKey;

    private final AlipayClient alipayClient;

    private AlipayConfig alipayConfig(String url){

        try {
            this.privateKey = new String(Files.readAllBytes(Paths.get("./profile/alipay/privateKey.txt")));
            this.publicKey = new String(Files.readAllBytes(Paths.get("./profile/alipay/publicKey.txt")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(url);
        alipayConfig.setAppId("To-You");
        alipayConfig.setPrivateKey(privateKey);
        alipayConfig.setFormat("json");
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setAlipayPublicKey(publicKey);
        alipayConfig.setSignType("RSA2");

        return alipayConfig;
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
