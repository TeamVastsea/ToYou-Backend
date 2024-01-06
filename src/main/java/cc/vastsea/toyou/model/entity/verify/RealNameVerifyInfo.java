package cc.vastsea.toyou.model.entity.verify;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RealNameVerifyInfo {
    @SerializedName("identity_type")
    private String identityType = "CERT_INFO";
    @SerializedName("cert_type")
    private String certType = "IDENTITY_CARD";
    @SerializedName("cert_name")
    private String certName;
    @SerializedName("cert_no")
    private String certNo;
    @SerializedName("phone_no")
    private String phoneNo;

    private String url;
}
