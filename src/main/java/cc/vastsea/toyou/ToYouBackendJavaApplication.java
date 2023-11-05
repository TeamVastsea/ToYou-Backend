package cc.vastsea.toyou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cc.vastsea.toyou.mapper")
@EnableScheduling
@EnableAsync
@EnableCaching
public class ToYouBackendJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToYouBackendJavaApplication.class, args);
	}

}
