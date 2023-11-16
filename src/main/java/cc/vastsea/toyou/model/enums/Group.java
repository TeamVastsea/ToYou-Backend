package cc.vastsea.toyou.model.enums;

import lombok.Getter;

@Getter
public enum Group {
	DEFAULT(0, "默认", 0, 2048, 50, 0),
	STARTED(1, "入门", 30, 10240, 50, 0),
	ADVANCED(2, "进阶", 50, 51200, 100, 0),
	PROFESSIONAL(3, "专业", 150, 102400, 999999, 0);

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

	/**
	 * 优惠百分比
	 */
	final float discount;

	Group(int priority, String name, float price, float storage, float restrictions, float discount) {
		this.priority = priority;
		this.name = name;
		this.price = price;
		this.storage = storage;
		this.restrictions = restrictions;
		this.discount = discount;
	}

	public static long toByte(float mb) {
		return (long) (mb * 1024 * 1024);
	}

	public long getStorageByte() {
		return toByte(storage);
	}

	/**
	 * 根据月计算价格(单位:分)
	 */
	public int getPriceByMonth(int month) {
		int price = (int) (this.price * 100);
		if (discount > 0) {
			// 计算折扣
			float d = 1 - discount / 100;
			price = (int) (price * d);
		}
		/*
		  3个月95折
		  6个月9折
		  12个月85折
		 */
		if (month >= 3 && month < 6) {
			price = (int) (price * 0.95);
		} else if (month >= 6 && month < 12) {
			price = (int) (price * 0.9);
		} else if (month >= 12) {
			price = (int) (price * 0.85);
		}
		return price;
	}
}
