package cc.vastsea.toyou.model.dto.verify;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RealNameVerifyResponse {

    @SerializedName("code")
    private String code;

    @SerializedName("msg")
    private String message;

    @SerializedName("certify_id")
    private String certifyId;

    @SerializedName("certify_url")
    private String certifyUrl;

}
