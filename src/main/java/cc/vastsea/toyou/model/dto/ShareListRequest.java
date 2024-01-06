package cc.vastsea.toyou.model.dto;

import cc.vastsea.toyou.common.PageRequest;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShareListRequest extends PageRequest implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private Long uid;
    private String pid;
    private String fileName;
    private Long downloads;
    private Integer shareMode;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Boolean available;
}
