package br.ufmg.plataforma;

import org.springframework.boot.SpringApplication;

public class TestPlataformaApplication {

	public static void main(String[] args) {
		SpringApplication.from(PlataformaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
