package cc.vastsea.toyou.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SharePictureRequest implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	private Integer shareMode;
	private String password;
}
