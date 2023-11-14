package cc.vastsea.toyou.model.entity;

import cc.vastsea.toyou.model.enums.Group;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "permission")
@Data
public class Permission implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long uid;
	private String permission;
	private Long expiry;
	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;

	public Group getGroup() {
		try {
			return Group.valueOf(permission.substring(6).toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return Group.DEFAULT;
		}
	}
}
