package cc.vastsea.toyou.model.enums;

import lombok.Getter;

@Getter
public enum Group {
	DEFAULT(0, "默认", 0, 2048, 50),
	STARTED(1, "入门", 30, 10240, 50),
	ADVANCED(2, "进阶", 50, 51200, 100),
	PROFESSIONAL(3, "专业", 150, 102400, 999999);

	/**
	 * 数字越大越优先
	 */
	final int priority;
	/**
	 * 组名
	 */
	final String name;
	/**
	 * 多少钱/月(单位:元CNY)
	 */
	final float price;
	/**
	 * 存储空间(单位:MB)
	 */
	final float storage;
	/**
	 * 单图片体积限制(单位:MB)
	 */
	final float restrictions;

	Group(int priority, String name, float price, float storage, float restrictions) {
		this.priority = priority;
		this.name = name;
		this.price = price;
		this.storage = storage;
		this.restrictions = restrictions;
	}

	public static long toByte(float mb) {
		return (long) (mb * 1024 * 1024);
	}

	public long getStorageByte() {
		return toByte(storage);
	}
}
