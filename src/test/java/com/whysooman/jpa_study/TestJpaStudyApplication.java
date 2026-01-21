package com.whysooman.jpa_study;

import org.springframework.boot.SpringApplication;

public class TestJpaStudyApplication {

	public static void main(String[] args) {
		SpringApplication.from(JpaStudyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
