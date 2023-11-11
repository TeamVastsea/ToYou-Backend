package cc.vastsea.toyou.model.enums;

import lombok.Getter;

@Getter
public enum Group {
    DEFAULT(0,"默认",0,2048,50);
    /**
     * 数字越大越优先
     */
    final int priority;
    /**
     * 组名
     */
    final String name;
    /**
     * 多少钱/月
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
     Group(int priority,String name, float price, float storage, float restrictions){
         this.priority=priority;
        this.name = name;
        this.price = price;
        this.storage = storage;
        this.restrictions = restrictions;
    }

    public static long toByte(float mb) {
        return (long) (mb * 1024 * 1024);
    }
}
