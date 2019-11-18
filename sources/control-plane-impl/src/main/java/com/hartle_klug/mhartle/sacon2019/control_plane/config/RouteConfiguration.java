package com.hartle_klug.mhartle.sacon2019.control_plane.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.hartle_klug.mhartle.sacon2019.control_plane.web.ControlPlaneRouterBuilder;

import io.envoyproxy.controlplane.cache.SnapshotCache;

@Configuration
public class RouteConfiguration {
	@Bean
	public RouterFunction<ServerResponse> routes(SnapshotCache<String> cache, BouncyCastleProvider provider) {
		return new ControlPlaneRouterBuilder(cache, provider).build();
	}
}
