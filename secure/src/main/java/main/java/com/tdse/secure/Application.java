package main.java.com.tdse.secure;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	public static void main(String[] args) {
 		SpringApplication app = new SpringApplication(Application.class);
 		app.setDefaultProperties(Collections.singletonMap("server.port", "5000"));
 		app.run(args);
 	}
}
