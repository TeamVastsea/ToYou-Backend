package cc.vastsea.toyou.model.dto.verify;

import cc.vastsea.toyou.model.entity.verify.MerchantConfig;
import cc.vastsea.toyou.model.entity.verify.RealNameVerifyInfo;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RealNameVerifyRequest {
    @SerializedName("outer_order_no")
    private String orderNo;
    @SerializedName("biz_code")
    private String bizCode = "FUTURE_TECH_BIZ_FACE_SDK";
    @SerializedName("identity_param")
    private RealNameVerifyInfo realNameVerifyInfo;
    @SerializedName("merchant_config")
    private MerchantConfig merchantConfig;
}
