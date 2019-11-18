package com.hartle_klug.mhartle.sacon2019.echo.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public class EchoRouterBuilder {
	private final String id;
	
	public EchoRouterBuilder(final String id) {
		this.id = id;
	}
	
	public RouterFunction<ServerResponse> build() {
		final RouterFunction<ServerResponse> result =
				route(all(), this::echoRequest);
		
		return result;
	}
	
	public Mono<ServerResponse> echoRequest(ServerRequest serverRequest) {
		// Prepare headers
		final Map<String, Object> headers = new LinkedHashMap<String, Object>();
		for(final Entry<String, List<String>> entry : serverRequest.headers().asHttpHeaders().entrySet()) {
			headers.put(entry.getKey(), entry.getValue());
		}

		final Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("method", serverRequest.methodName());
		request.put("path", serverRequest.path());
		request.put("uri", serverRequest.uri().toString());
		request.put("headers", headers);

		final Map<String, Object> representation = new LinkedHashMap<String, Object>();
		representation.put("id", this.id);
		representation.put("remote", serverRequest.remoteAddress().orElse(null));
		representation.put("request", request);

		final Mono<ServerResponse> result =
				ServerResponse.ok()
				.syncBody(representation);
		
		return result;
	}
}
