package com.hartle_klug.mhartle.sacon2019.control_plane.util;

import io.envoyproxy.envoy.service.metrics.v2.MetricsServiceGrpc.MetricsServiceImplBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.envoyproxy.envoy.service.metrics.v2.StreamMetricsMessage;
import io.envoyproxy.envoy.service.metrics.v2.StreamMetricsResponse;
import io.grpc.stub.StreamObserver;

public class MetricsService extends MetricsServiceImplBase {
	private static final Logger LOG = LoggerFactory.getLogger(MetricsService.class);

	@Override
	public StreamObserver<StreamMetricsMessage> streamMetrics(StreamObserver<StreamMetricsResponse> responseObserver) {
		return new StreamObserver<StreamMetricsMessage>() {
			@Override
			public void onNext(StreamMetricsMessage message) {
				LOG.info(message.toString());
			}

			@Override
			public void onError(Throwable t) {
				LOG.error(t.getMessage());
			}

			@Override
			public void onCompleted() {
				LOG.info("Stream closed");
			}
		};		
	}
}
