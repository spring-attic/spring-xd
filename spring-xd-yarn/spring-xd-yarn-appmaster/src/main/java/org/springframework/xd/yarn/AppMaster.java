package org.springframework.xd.yarn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Map;

@EnableAutoConfiguration
public class AppMaster {

	public static void main(String[] args) {
		SpringApplication.run(AppMaster.class, args);
	}

}
