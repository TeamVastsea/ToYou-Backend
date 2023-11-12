package cc.vastsea.toyou.model.enums;

import lombok.Getter;

@Getter
public enum ShareMode {
	// 不公开
	PRIVATE(0),
	// 公开(水印版本)
	PUBLIC(1),
	// 公开(压缩版本)
	PUBLIC_COMPRESS(2),
	// 公开(原图)
	PUBLIC_ORIGINAL(3),
	// 其它
	OTHER(4);
	final int code;

	ShareMode(int code) {
		this.code = code;
	}

	public static ShareMode of(int code) {
		for (ShareMode shareMode : ShareMode.values()) {
			if (shareMode.code == code) {
				return shareMode;
			}
		}
		return OTHER;
	}
}
