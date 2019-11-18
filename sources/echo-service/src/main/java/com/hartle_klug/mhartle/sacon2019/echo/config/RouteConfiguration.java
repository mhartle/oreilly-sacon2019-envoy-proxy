package com.hartle_klug.mhartle.sacon2019.echo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.hartle_klug.mhartle.sacon2019.echo.web.EchoRouterBuilder;

@Configuration
public class RouteConfiguration {
	private final String id;
	
	public RouteConfiguration(final @Value("${server.id:}") String id) {
		this.id = id;
	}
	
	@Bean
	public RouterFunction<ServerResponse> routes() {
		return new EchoRouterBuilder(this.id).build();
	}
}
