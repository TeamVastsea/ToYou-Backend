package cc.vastsea.toyou.model.vo;

import cc.vastsea.toyou.model.enums.Group;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class UserVO implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long uid;

	private String username;

	private String email;

	private Date createTime;

	private Date updateTime;

	private ExtendUserInformation extend;

	@Data
	public static class ExtendUserInformation {
		private Long storageUsed;
		private Group userGroup;
		private Long groupStartDate;
		private Long groupUpdateDate;
		private Long groupEndDate;
	}
}
