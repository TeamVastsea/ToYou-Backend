package cc.vastsea.toyou.model.entity.verify;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MerchantConfig {
    @SerializedName("return_url")
    private String returnUrl = "";
    @SerializedName("face_reserve_strategy")
    private String faceReserveStrategy;
}
