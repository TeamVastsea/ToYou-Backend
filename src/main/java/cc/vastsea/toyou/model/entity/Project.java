package cc.vastsea.toyou.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("project")
@Data
public class Project {
    private Long prid;
    private Long uid;
    private String child;
    private String parent;
    private String name;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean available;
}