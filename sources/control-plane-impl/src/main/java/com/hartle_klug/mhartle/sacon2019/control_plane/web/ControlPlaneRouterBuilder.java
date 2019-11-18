package com.hartle_klug.mhartle.sacon2019.control_plane.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.hartle_klug.mhartle.sacon2019.control_plane.usecases.HttpFrontProxyUseCase;
import com.hartle_klug.mhartle.sacon2019.control_plane.usecases.HttpsFrontProxyUseCase;
import com.hartle_klug.mhartle.sacon2019.control_plane.usecases.IncrementalDeploymentUseCase;
import com.hartle_klug.mhartle.sacon2019.control_plane.usecases.NoOpUseCase;
import com.hartle_klug.mhartle.sacon2019.control_plane.usecases.UseCase;

import io.envoyproxy.controlplane.cache.SnapshotCache;
import reactor.core.publisher.Mono;

public class ControlPlaneRouterBuilder {
	private final SnapshotCache<String> snapshotCache;
	private final BouncyCastleProvider provider;
	private UseCase useCase;
	
	public ControlPlaneRouterBuilder(SnapshotCache<String> snapshotCache, BouncyCastleProvider provider) {
		this.snapshotCache = snapshotCache;
		this.provider = provider;
		this.useCase = new NoOpUseCase();
	}

	public RouterFunction<ServerResponse> build() {
		final RouterFunction<ServerResponse> result =
				route(POST("/noop"), request -> this.pickUseCase(request, new NoOpUseCase()))
				.andRoute(POST("/http-front-proxy"), request -> this.pickUseCase(request, new HttpFrontProxyUseCase()))
				.andRoute(POST("/https-front-proxy"), request -> this.pickUseCase(request, new HttpsFrontProxyUseCase(this.provider)))
				.andRoute(POST("/incremental-deployment"), request -> this.pickUseCase(request, new IncrementalDeploymentUseCase()))
				.andRoute(all(), this::badRequest);
		
		return result;
	}
	
	public Mono<ServerResponse> pickUseCase(final ServerRequest request, final UseCase newUseCase) {				
		final Mono<ServerResponse> result =
				Mono.create(sink -> {
					try {
						updateUseCase(newUseCase, request.queryParams());
						sink.success();
					} catch (Exception exception) {
						sink.error(exception);
					}
				})
				.flatMap(ignored -> ServerResponse.ok().build())
				.onErrorResume(exception -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).syncBody(exception));
		
		return result;
	}	
		
	public Mono<ServerResponse> badRequest(final ServerRequest request) {
		final Mono<ServerResponse> result =
				ServerResponse.badRequest().build();
		
		return result;
	}
	
	private void updateUseCase(final UseCase newUseCase, final MultiValueMap<String, String> queryParameters) throws Exception {
		final Map<String, List<String>> parameters = queryParameters.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		if (this.useCase != null) {
			this.useCase.unapply(snapshotCache, System.currentTimeMillis());
		}
		this.useCase = newUseCase;
		this.useCase.apply(parameters, this.snapshotCache, System.currentTimeMillis());
	}
}
