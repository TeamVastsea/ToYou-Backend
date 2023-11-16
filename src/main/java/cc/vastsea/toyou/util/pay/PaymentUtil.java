package cc.vastsea.toyou.util.pay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author xLikeWATCHDOG
 */
public class PaymentUtil {
	/**
	 * 生成订单号
	 */
	public static String generateTradeNumber() {
		// 获取当前日期时间
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String timestamp = dateFormat.format(now);

		// 生成随机数，保证订单号的唯一性
		Random random = new Random();
		int randomNumber = random.nextInt(10000); // 在0~9999之间生成随机数

		// 组合订单号
		return timestamp + String.format("%04d", randomNumber);
	}

	// 格式化输入的金额，若金额正常，则返回格式化后的金额。否则返回null
	public static String formatAmount(String amount) {
		// 判断金额是否为数字
		if (!amount.matches("\\d+(\\.\\d+)?")) {
			return null;
		}

		// 判断金额是否为整数或小数
		if (amount.indexOf(".") > 0) {
			// 若为小数，则判断小数位数是否正确
			if (amount.split("\\.")[1].length() != 2) {
				return null;
			}
		}

		// 判断金额是否为负数
		if (Double.parseDouble(amount) < 0) {
			return null;
		}

		// 格式化金额
		return String.format("%.2f", Double.parseDouble(amount));
	}


	/**
	 * 将分转换为元
	 */
	public static String changeF2Y(int amount) {
		return String.format("%.2f", (double) amount / 100);
	}

	/**
	 * 将元转换为分
	 */
	public static int changeY2F(String amount) {
		int index = amount.indexOf(".");
		int length = amount.length();
		int amInt;
		if (index == -1) {
			amInt = Integer.parseInt(amount + "00");
		} else if (length - index >= 3) {
			amInt = Integer.parseInt((amount.substring(0, index + 3)).replace(".", ""));
		} else if (length - index == 2) {
			amInt = Integer.parseInt((amount.substring(0, index + 2)).replace(".", "") + 0);
		} else {
			amInt = Integer.parseInt((amount.substring(0, index + 1)).replace(".", "") + "00");
		}
		return amInt;
	}
}
