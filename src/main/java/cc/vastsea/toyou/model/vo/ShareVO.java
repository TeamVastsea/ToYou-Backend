package cc.vastsea.toyou.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class ShareVO implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private String sid;
    private Long id;
    private Long uid;
    private Boolean password;
    private Long downloads;
    private Integer shareMode;
    private Long expiry;
}
