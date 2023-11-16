package cc.vastsea.toyou.pay;

import cc.vastsea.toyou.util.pay.PaymentUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PaymentTest {
	@Test
	void test() {
		int f = PaymentUtil.changeY2F("888");
		System.out.println(f);
		String y = PaymentUtil.changeF2Y(f);
		System.out.println(y);
	}

	@Test
	void formatAmount() {
		String amount = "-1";
		String formatAmount = PaymentUtil.formatAmount(amount);
		System.out.println(formatAmount);
	}
}
