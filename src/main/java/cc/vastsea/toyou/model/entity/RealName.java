package cc.vastsea.toyou.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealName {

    @TableId(value = "uid")
    private Long uid;

    @TableField("name")
    private String name;

    @TableField("idCard")
    private String idCard;

    @TableField("pass")
    private Boolean pass;

    @TableField(value = "createTime", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updateTime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("available")
    private Boolean available;
}