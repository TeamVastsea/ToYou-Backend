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

@TableName(value = "share")
@Data
public class Share implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private String sid;
	private Long id;
	private Long uid;
	private String password;
	private Long downloads;
	private Integer shareMode;
	private Long expiry;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
