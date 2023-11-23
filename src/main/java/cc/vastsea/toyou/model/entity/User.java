package cc.vastsea.toyou.model.entity;

import cc.vastsea.toyou.model.vo.UserVO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "user")
@Data
public class User implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long uid;
	private String username;
	private String password;
	private String email;
	private String emailRaw;
	private String phone;
	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;

	public UserVO toUserVO() {
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(this, userVO);
		return userVO;
	}
}
