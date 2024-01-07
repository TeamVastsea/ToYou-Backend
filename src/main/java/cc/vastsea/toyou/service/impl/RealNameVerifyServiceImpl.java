package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.mapper.RealNameMapper;
import cc.vastsea.toyou.model.dto.verify.RealNameVerifyRequest;
import cc.vastsea.toyou.model.dto.verify.RealNameVerifyResponse;
import cc.vastsea.toyou.model.entity.RealName;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.entity.verify.MerchantConfig;
import cc.vastsea.toyou.model.entity.verify.RealNameVerifyInfo;
import cc.vastsea.toyou.service.RealNameVerifyService;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.CaffeineFactory;
import cc.vastsea.toyou.util.pay.AliPayClientUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyInitializeResponse;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyQueryResponse;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyVerifyResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RealNameVerifyServiceImpl implements RealNameVerifyService {

    private AliPayClientUtil aliPayClientUtil;


    @Resource
    private RealNameMapper realNameMapper;

    @Resource
    private UserService userService;

    /**
     * 实名信息缓存
     */
    private Cache<String,RealNameVerifyInfo> realNameVerifyInfoMap = CaffeineFactory.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();


    private final Gson gson = new Gson();

    private AlipayClient getAliPayClient(){
        if(aliPayClientUtil==null) {
            this.aliPayClientUtil = new AliPayClientUtil("https://openapi.alipay.com/gateway.do");
        }
        return aliPayClientUtil.getAlipayClient();
    }

    /**
     * 生成标识
     * @return
     */
    private String generateOrderNo(){
        int maxLength = 32;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = dateFormat.format(new Date());

        String randomSequence = UUID.randomUUID().toString().replace("-", "");
        String orderId = "to-you" + timestamp + randomSequence;

        return orderId.substring(0, Math.min(orderId.length(), maxLength));
    }

    /**
     * 初始化
     */
    public RealNameVerifyResponse initFaceVerify(RealNameVerifyInfo realNameVerifyInfo){

        RealNameVerifyRequest realNameVerifyRequest = new RealNameVerifyRequest();

        MerchantConfig merchantConfig = new MerchantConfig();
        merchantConfig.setReturnUrl(realNameVerifyInfo.getUrl());

        realNameVerifyRequest.setMerchantConfig(merchantConfig);
        realNameVerifyRequest.setOrderNo(generateOrderNo());

        // 设置实名信息
        realNameVerifyRequest.setRealNameVerifyInfo(realNameVerifyInfo);

        RealNameVerifyResponse realNameVerifyResponse = new RealNameVerifyResponse();

        DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest datadigitalFincloudGeneralsaasFaceCertifyInitializeRequest
                = new DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest();

        datadigitalFincloudGeneralsaasFaceCertifyInitializeRequest
                .setBizContent(gson.toJson(realNameVerifyRequest));

        try {
            DatadigitalFincloudGeneralsaasFaceCertifyInitializeResponse execute = getAliPayClient().certificateExecute(datadigitalFincloudGeneralsaasFaceCertifyInitializeRequest);
            realNameVerifyResponse.setCode(execute.getCode());
            realNameVerifyResponse.setMessage(execute.getSubMsg());
            // 认证成功
            if(execute.isSuccess()) {
                String certifyId = execute.getCertifyId();
                realNameVerifyResponse.setCertifyId(certifyId);
                // 写入缓存，等待实名流程完成
                realNameVerifyInfoMap.put(certifyId,realNameVerifyInfo);
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        return realNameVerifyResponse;
    }

    /**
     * 开始认证
     */
    @Override
    public RealNameVerifyResponse startVerify(String certifyId,String url){
        RealNameVerifyResponse realNameVerifyResponse = new RealNameVerifyResponse();

        // 如果缓存被清理
        if(!realNameVerifyInfoMap.asMap().containsKey(certifyId)) {
            realNameVerifyResponse.setCode("-200");
            realNameVerifyResponse.setMessage("调用认证接口超时");
            return realNameVerifyResponse;
        }

        DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest request = new DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("certify_id",certifyId);

        request.setBizContent(jsonObject.toString());


        try {
            DatadigitalFincloudGeneralsaasFaceCertifyVerifyResponse execute = getAliPayClient().certificateExecute(request);
            realNameVerifyResponse.setCode(execute.getCode());
            realNameVerifyResponse.setMessage(execute.getSubMsg());
            if(execute.isSuccess()){
                realNameVerifyResponse.setCertifyUrl(execute.getCertifyUrl());
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        return realNameVerifyResponse;
    }

    /**
     * 是否完成认证
     */
    public boolean verifyResultQuery(HttpServletRequest httpServletRequest,String certifyId){

        DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest request = new DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("certify_id",certifyId);

        request.setBizContent(jsonObject.toString());

        DatadigitalFincloudGeneralsaasFaceCertifyQueryResponse response = null;
        try {
            response = getAliPayClient().certificateExecute(request);

            if(response.isSuccess()){
                // 写入实名信息到数据库
                User tokenLogin = userService.getTokenLogin(httpServletRequest);

                RealNameVerifyInfo realNameVerifyInfo = realNameVerifyInfoMap.getIfPresent(certifyId);
                RealName realName = new RealName();
                realName.setUid(tokenLogin.getUid());
                realName.setName(realNameVerifyInfo.getCertName());
                realName.setIdCard(realNameVerifyInfo.getCertNo());
                realName.setPass(true);

                realNameMapper.insert(realName);

                return true;
            }


        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        return false;
    }



}
