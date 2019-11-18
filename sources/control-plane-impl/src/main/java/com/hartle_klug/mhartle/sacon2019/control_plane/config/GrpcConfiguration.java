package com.hartle_klug.mhartle.sacon2019.control_plane.config;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Provider;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hartle_klug.mhartle.sacon2019.control_plane.util.AccessLogService;
import com.hartle_klug.mhartle.sacon2019.control_plane.util.LoggingServerStreamTracerFactory;
import com.hartle_klug.mhartle.sacon2019.control_plane.util.MetricsService;
import com.hartle_klug.mhartle.sacon2019.control_plane.util.TapSinkService;

import io.envoyproxy.controlplane.cache.SnapshotCache;
import io.envoyproxy.controlplane.server.DiscoveryServer;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

@Configuration
public class GrpcConfiguration {
	private static final Logger LOG = LoggerFactory.getLogger(GrpcConfiguration.class);

	private final String address;
	private final int port;
	private final String certChainFilePath;
	private final String privateKeyFilePath;

	public GrpcConfiguration(
			final @Value("${server.address}") String address,
			final @Value("${server.port.xds}") int port,
			final @Value("${server.tls.certChain}") String certChainFilePath,
			final @Value("${server.tls.privateKey}") String privateKeyFilePath) {
		this.address = address;
		this.port = port;
		this.certChainFilePath = certChainFilePath;
		this.privateKeyFilePath = privateKeyFilePath;
	}

	@Bean(initMethod = "start", destroyMethod = "shutdown")
	public Server server(SnapshotCache<String> cache, Provider provider) throws IOException {
		LOG.info("Preparing gRPC server...");
		
		// Prepare DiscoveryServer using injected cache
		final DiscoveryServer discoveryServer = new DiscoveryServer(cache);
		
		final io.grpc.ServerStreamTracer.Factory streamTracerFactory = new LoggingServerStreamTracerFactory();
		
		final TrustManagerFactory trustManagerFactory =
				InsecureTrustManagerFactory.INSTANCE;
		
		// Prepare server builder
		final NettyServerBuilder serverBuilder =
				// Set address and port
				NettyServerBuilder.forAddress(
						new InetSocketAddress(this.address, this.port))
				
				// Log actual stream, if necessary
				.addStreamTracerFactory(streamTracerFactory)
				
				// Protect gRPC with TLS & self-signed certificate 
				.sslContext(
						GrpcSslContexts.configure(
							SslContextBuilder.forServer(
									new File(this.certChainFilePath),
									new File(this.privateKeyFilePath))
		
							// Configure individual ciphers, if necessary
							// .ciphers(Arrays.asList("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256".split(":")))
							.trustManager(trustManagerFactory)
							.clientAuth(ClientAuth.NONE))
							.applicationProtocolConfig(
									new ApplicationProtocolConfig(
										Protocol.ALPN,
										SelectorFailureBehavior.NO_ADVERTISE,
										SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
										"h2")
							)
						
						.build())
				
				
				/*
				// Protect gRPC with TLS & certificate issued by CA
				.useTransportSecurity(
						new ByteArrayInputStream(certChain.getBytes()),
						new ByteArrayInputStream(privateKey.getBytes()))
				*/
				
				// Add aggregated discovery service
				.addService(discoveryServer.getAggregatedDiscoveryServiceImpl())
								
				// Add access log service
				.addService(new AccessLogService())
				.addService(new TapSinkService())
				.addService(new MetricsService());

		// Build server
		final Server server = serverBuilder.build();

		LOG.info("Prepared gRPC server.");

		return server;
	}
}
