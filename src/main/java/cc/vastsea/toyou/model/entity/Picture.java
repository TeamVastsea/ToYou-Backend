package cc.vastsea.toyou.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "picture")
@Data
public class Picture implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private String pid;
	private Boolean isPublic;
	private Long downloads;
	private Long expiry;
	private Long owner;
	private String path;
	private String realName;
	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
