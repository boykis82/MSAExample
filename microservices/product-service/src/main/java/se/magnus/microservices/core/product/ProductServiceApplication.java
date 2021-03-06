package se.magnus.microservices.core.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan("se.magnus")
@Slf4j
public class ProductServiceApplication {
	private final Integer connectionPoolSize;

	public ProductServiceApplication(
			@Value("${spring.datasource.maximum-pool-size:10}")
					Integer connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	@Bean
	public Scheduler jdbcScheduler() {
		log.info("create a jdbcscheduler. conn pool size = " + connectionPoolSize);
		return Schedulers.fromExecutor(Executors.newFixedThreadPool(connectionPoolSize));
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);
		String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
		log.info("connected to mysql: " + mysqlUri);
	}
}
