package cc.vastsea.toyou.model.dto;

import cc.vastsea.toyou.model.entity.User;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
public class UserLoginResponse implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	private UUID token;
	private User user;
}
