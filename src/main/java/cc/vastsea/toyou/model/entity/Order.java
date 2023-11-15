package cc.vastsea.toyou.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "order")
@Data
public class Order implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 关联用户UID
	 */
	private Long uid;

	/**
	 * 订单标题
	 */
	private String subject;

	/**
	 * 订单号
	 */
	private String outTradeNo;

	/**
	 * 系统交易号
	 */
	private String tradeNo;

	/**
	 * 订单金额(单位：分)
	 */
	private Long totalAmount;

	/**
	 * 实收金额(单位：分)
	 */
	private Long receiptAmount;

	/**
	 * 支付平台
	 * 使用{@link cc.vastsea.toyou.model.enums.pay.PayPlatform#of(int)}进行转换
	 */
	private Integer payPlatform;

	/**
	 * 交易状态
	 * 使用{@link cc.vastsea.toyou.model.enums.pay.TradeStatus#of(int)}进行转换
	 */
	private Integer tradeStatus;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
