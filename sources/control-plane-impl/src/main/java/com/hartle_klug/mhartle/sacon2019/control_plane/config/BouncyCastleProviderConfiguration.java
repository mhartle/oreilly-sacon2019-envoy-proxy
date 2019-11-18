package com.hartle_klug.mhartle.sacon2019.control_plane.config;

import java.security.Security;

import javax.annotation.PostConstruct;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BouncyCastleProviderConfiguration {
	private BouncyCastleProvider bouncyCastleProvider;

	@Bean
	public BouncyCastleProvider bouncyCastleProvider() {
		return bouncyCastleProvider;
	}

	@PostConstruct
	public void addSecurityProvider() {
		this.bouncyCastleProvider = new BouncyCastleProvider();
		Security.addProvider(this.bouncyCastleProvider);
	}
}
