package cc.vastsea.toyou;

import cc.vastsea.toyou.task.CleanupEndTask;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Random;

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
