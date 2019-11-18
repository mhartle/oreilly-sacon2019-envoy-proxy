package com.hartle_klug.mhartle.sacon2019.control_plane.util;

import io.envoyproxy.envoy.service.accesslog.v2.AccessLogServiceGrpc.AccessLogServiceImplBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.envoyproxy.envoy.service.accesslog.v2.StreamAccessLogsMessage;
import io.envoyproxy.envoy.service.accesslog.v2.StreamAccessLogsResponse;
import io.grpc.stub.StreamObserver;

public class AccessLogService extends AccessLogServiceImplBase {
	private static final Logger LOG = LoggerFactory.getLogger(AccessLogService.class);

	@Override
	public StreamObserver<StreamAccessLogsMessage> streamAccessLogs(
			StreamObserver<StreamAccessLogsResponse> responseObserver) {
		return new StreamObserver<StreamAccessLogsMessage>() {
			@Override
			public void onNext(StreamAccessLogsMessage message) {
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
