package com.hartle_klug.mhartle.sacon2019.control_plane.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.envoyproxy.envoy.service.tap.v2alpha.StreamTapsRequest;
import io.envoyproxy.envoy.service.tap.v2alpha.StreamTapsResponse;
import io.envoyproxy.envoy.service.tap.v2alpha.TapSinkServiceGrpc.TapSinkServiceImplBase;
import io.grpc.stub.StreamObserver;

public class TapSinkService extends TapSinkServiceImplBase {
	private static final Logger LOG = LoggerFactory.getLogger(TapSinkService.class);

	@Override
	public StreamObserver<StreamTapsRequest> streamTaps(StreamObserver<StreamTapsResponse> responseObserver) {
		return new StreamObserver<StreamTapsRequest>() {
			@Override
			public void onNext(StreamTapsRequest message) {
				// LOG.info(message.toString());
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
