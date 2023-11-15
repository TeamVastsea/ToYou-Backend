package cc.vastsea.toyou.model.dto;

import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.pay.PayPlatform;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OrderCreationRequest implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	private Group group;
	private PayPlatform payPlatform;
	private Integer amount;
}
