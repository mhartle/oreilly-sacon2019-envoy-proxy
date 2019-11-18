package com.hartle_klug.mhartle.sacon2019.control_plane.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerStreamTracer.Factory;
import io.grpc.Status;

public class LoggingServerStreamTracerFactory extends Factory {
	private static final Logger LOG = LoggerFactory.getLogger(LoggingServerStreamTracerFactory.class);
	
	public ServerStreamTracer newServerStreamTracer(final String fullMethodName, final Metadata headers) {
		return new LoggingServerStreamTracer(fullMethodName, headers);
	}
	
	public static class LoggingServerStreamTracer extends ServerStreamTracer {
		private final String fullMethodName;
		private final Metadata headers;
		
		public LoggingServerStreamTracer(final String fullMethodName, final Metadata headers) {
			this.fullMethodName = fullMethodName;
			this.headers = headers;
		}

		@Override
		public void serverCallStarted(ServerCallInfo<?, ?> callInfo) {
			LOG.info("serverCallStarted {} {} {}", this.fullMethodName, this.headers, callInfo.toString());
		}

		@Override
		public void outboundMessage(int seqNo) {
			LOG.info("outboundMessage {}", seqNo);
		}

		@Override
		public void inboundMessage(int seqNo) {
			LOG.info("inboundMessage {}", seqNo);
		}

		@Override
		public void outboundMessageSent(int seqNo, long optionalWireSize, long optionalUncompressedSize) {
			LOG.info("outboundMessageSent {}", seqNo);
		}

		@Override
		public void inboundMessageRead(int seqNo, long optionalWireSize, long optionalUncompressedSize) {
			LOG.info("inboundMessageRead {}", seqNo);
		}

		@Override
		public void outboundWireSize(long bytes) {
			LOG.info("outboundWireSize {}", bytes);
		}

		@Override
		public void outboundUncompressedSize(long bytes) {
			LOG.info("outboundUncompressedSize {}", bytes);
		}

		@Override
		public void inboundWireSize(long bytes) {
			LOG.info("inboundWireSize {}", bytes);
		}

		@Override
		public void inboundUncompressedSize(long bytes) {
			LOG.info("inboundUncompressedSize {}", bytes);
		}

		@Override
		public void streamClosed(Status status) {
			if (status != null) {
				// System.out.println(status.getDescription() + ", " + status.getCause() != null ? status.getCause().getStackTrace() : "");
			}
		}
	}
}
