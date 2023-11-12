package cc.vastsea.toyou.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureMetaGetResponse implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	private String uuid;
	private Long size;
	private String fileName;
	private Long downloads;
	private Integer shareMode;
}
