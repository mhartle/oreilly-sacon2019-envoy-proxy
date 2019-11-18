package com.hartle_klug.mhartle.sacon2019.control_plane.usecases;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.controlplane.cache.SnapshotCache;
import io.envoyproxy.envoy.api.v2.Cluster;
import io.envoyproxy.envoy.api.v2.Cluster.DiscoveryType;
import io.envoyproxy.envoy.api.v2.Cluster.EdsClusterConfig;
import io.envoyproxy.envoy.api.v2.Cluster.LbPolicy;
import io.envoyproxy.envoy.api.v2.ClusterLoadAssignment;
import io.envoyproxy.envoy.api.v2.Listener;
import io.envoyproxy.envoy.api.v2.RouteConfiguration;
import io.envoyproxy.envoy.api.v2.auth.CommonTlsContext;
import io.envoyproxy.envoy.api.v2.auth.DownstreamTlsContext;
import io.envoyproxy.envoy.api.v2.auth.SdsSecretConfig;
import io.envoyproxy.envoy.api.v2.auth.Secret;
import io.envoyproxy.envoy.api.v2.core.Address;
import io.envoyproxy.envoy.api.v2.core.AggregatedConfigSource;
import io.envoyproxy.envoy.api.v2.core.ConfigSource;
import io.envoyproxy.envoy.api.v2.core.GrpcService;
import io.envoyproxy.envoy.api.v2.core.GrpcService.EnvoyGrpc;
import io.envoyproxy.envoy.api.v2.core.HeaderValue;
import io.envoyproxy.envoy.api.v2.core.HeaderValueOption;
import io.envoyproxy.envoy.api.v2.core.Metadata;
import io.envoyproxy.envoy.api.v2.core.SocketAddress;
import io.envoyproxy.envoy.api.v2.endpoint.Endpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LbEndpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LocalityLbEndpoints;
import io.envoyproxy.envoy.api.v2.listener.Filter;
import io.envoyproxy.envoy.api.v2.listener.FilterChain;
import io.envoyproxy.envoy.api.v2.route.Route;
import io.envoyproxy.envoy.api.v2.route.RouteAction;
import io.envoyproxy.envoy.api.v2.route.RouteMatch;
import io.envoyproxy.envoy.api.v2.route.VirtualHost;
import io.envoyproxy.envoy.config.accesslog.v2.CommonGrpcAccessLogConfig;
import io.envoyproxy.envoy.config.accesslog.v2.HttpGrpcAccessLogConfig;
import io.envoyproxy.envoy.config.filter.accesslog.v2.AccessLog;
import io.envoyproxy.envoy.config.filter.http.router.v2.Router;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager.ForwardClientCertDetails;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager.SetCurrentClientCertDetails;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpFilter;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.Rds;

public class HttpsFrontProxyUseCase extends AbstractUseCase {
	private static final Logger LOG = LoggerFactory.getLogger(HttpsFrontProxyUseCase.class);

	public static final String LISTENER_SERVICE = "listener-service";
	
	public static final String CLUSTER_SERVICE = "cluster-service";
	public static final String CLUSTER_ACCESSLOG = "cluster-accesslog";
	public static final String CLUSTER_ADS = "cluster-ads";
	
	public static final String ROUTE_SERVICE = "route-service";

	public static final String SECRET_EDGE_SERVER = "secret-edge-server";
	public static final String SECRET_EDGE_CLIENT = "secret-edge-client";
	
	private final Provider provider;
	
	public HttpsFrontProxyUseCase(Provider provider) {
		this.provider = provider;
	}
	
	public void apply(Map<String, List<String>> parameters, SnapshotCache<String> snapshotCache, long version) throws Exception {
		LOG.info("Applying use case...");

		final KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		final X509Certificate selfSignedCertificate =
				generateCertificate(
						this.provider,
						"C=DE, ST=Hesse, L=Darmstadt, O=Hartle & Klug Consulting GmbH, CN=envoy-edge",
						keyPair.getPrivate(),
						"C=DE, ST=Hesse, L=Darmstadt, O=Hartle & Klug Consulting GmbH, CN=envoy-edge",
						keyPair.getPublic(),
						Date.from(Instant.now()),
						Date.from(Instant.now().plus(java.time.Duration.ofHours(1))),
						BigInteger.valueOf(System.currentTimeMillis()),
						true);
		
		final X509Certificate clientCertificate =
				readCertificate(new File("etc/client.cert"));
		
		LOG.info("Certificate {}", selfSignedCertificate.getSerialNumber().toString(16));
		LOG.info("version {}", version);
		
		final Iterable<Cluster> clusters = createClusters(version);
		final Iterable<ClusterLoadAssignment> endpoints = createEndpoints(version);
		final Iterable<Listener> listeners = createListeners(version);
		final Iterable<RouteConfiguration> routes = createRoutes(version);
		final Iterable<Secret> secrets =
				ImmutableList.<Secret>builder()
				.add(createTlsCertificateSecret(SECRET_EDGE_SERVER, Lists.newArrayList(selfSignedCertificate), keyPair.getPrivate()))
				.add(createValidationContextSecret(SECRET_EDGE_CLIENT, clientCertificate))
				.build();

		final Snapshot edgeSnapshot =
				Snapshot.create(
						clusters,
						endpoints,
						listeners,
						routes,
						secrets,
						Long.toString(version));
		
		snapshotCache.setSnapshot("edge", edgeSnapshot);
		
		LOG.info("Applied use case.");
	}

	public void unapply(SnapshotCache<String> snapshotCache, long version) throws Exception {
		LOG.info("Unapplying use case...");

		/*
		final Snapshot edgeSnapshot =
				Snapshot.createEmpty(Long.toString(version));
		
		snapshotCache.setSnapshot("edge", edgeSnapshot);
		*/
		
		LOG.info("Unapplied use case.");
	}
	
	private static List<Cluster> createClusters(final long version) {
		final List<Cluster> result =
				ImmutableList.<Cluster>builder()								
				.add(
					Cluster.newBuilder()
					.setName(CLUSTER_SERVICE)
					.setConnectTimeout(
							Duration.newBuilder()
							.setNanos(250_000_000)
							.build())
					.setLbPolicy(LbPolicy.ROUND_ROBIN)
					.setType(DiscoveryType.EDS)
					.setEdsClusterConfig(
							EdsClusterConfig.newBuilder()
							.setEdsConfig(
								ConfigSource.newBuilder()
								.setAds(
									AggregatedConfigSource.newBuilder()
									.build())
								.build())
							.build())
					.build())
				.build();
		
		return result;
	}
	
	private static List<ClusterLoadAssignment> createEndpoints(final long version) {
		final List<ClusterLoadAssignment> result =
				ImmutableList.<ClusterLoadAssignment>builder()
				.add(
					ClusterLoadAssignment.newBuilder()
					.setClusterName(CLUSTER_SERVICE)
					.addEndpoints(
							LocalityLbEndpoints.newBuilder()
							.addLbEndpoints(
									LbEndpoint.newBuilder()
									.setEndpoint(
											Endpoint.newBuilder()
											.setAddress(
													Address.newBuilder()
													.setSocketAddress(
															SocketAddress.newBuilder()
															.setAddress("127.0.0.3")
															.setPortValue(8081)
															.build())
													.build())
											.build())
									.setMetadata(
											Metadata.newBuilder()
											.putFilterMetadata("envoy.lb",
													Struct.newBuilder()
													.putFields("host",
															Value.newBuilder()
															.setStringValue("127.0.0.3:8081")
															.build())
													.build())
											.build())
									.build())
							.build())
					.build())
				.build();
		
		return result;
	}
	
	private static List<Listener> createListeners(final long version) {
		final List<Listener> result =
				ImmutableList.<Listener>builder()
				.add(
						Listener.newBuilder()
						.setName(LISTENER_SERVICE)
						.setAddress(
								Address.newBuilder()
								.setSocketAddress(
										SocketAddress.newBuilder()
										.setAddress("127.0.0.1")
										.setPortValue(8443)
										.build())
								.build())
						// Ensure listeners are modified for each version so
						// old listeners on old secrets are drained immediately
						.setMetadata(
								Metadata.newBuilder()
								.putFilterMetadata("envoy.lb",
										Struct.newBuilder()
										.putFields("version",
												Value.newBuilder()
												.setStringValue(Long.toString(version))
												.build())
										.build())
								.build())
						.addFilterChains(
								FilterChain.newBuilder()
								// FIXME: Update to .setTransportSocket
								.setTlsContext(
										DownstreamTlsContext.newBuilder()
										.setCommonTlsContext(
												CommonTlsContext.newBuilder()
												.addTlsCertificateSdsSecretConfigs(
														SdsSecretConfig.newBuilder()
														.setSdsConfig(
																ConfigSource.newBuilder()
																.setAds(
																		AggregatedConfigSource.newBuilder()
																		.build())
																.build())
														.setName(SECRET_EDGE_SERVER)
														.build())
												.setValidationContextSdsSecretConfig(
														SdsSecretConfig.newBuilder()
														.setSdsConfig(
																ConfigSource.newBuilder()
																.setAds(
																		AggregatedConfigSource.newBuilder()
																		.build())
																.build())
														.setName(SECRET_EDGE_CLIENT)
														.build())
												.build())
										.build())
								
								// Tap into HTTP stream and forward it to gRPC service
								/*
								.addFilters(
										Filter.newBuilder()
										.setName("envoy.filters.http.tap")
										.setTypedConfig(
												Any.pack(
													Tap.newBuilder()
													.setCommonConfig(
															CommonExtensionConfig.newBuilder()
															.setStaticConfig(
																	TapConfig.newBuilder()
																	.setMatchConfig(
																			MatchPredicate.newBuilder()
																			.setAnyMatch(true)
																			.build())
																	.setOutputConfig(
																			OutputConfig.newBuilder()
																			.setStreaming(true)
																			.addSinks(
																					OutputSink.newBuilder()
																					.setFormat(Format.JSON_BODY_AS_BYTES)
																					.setStreamingGrpc(
																							StreamingGrpcSink.newBuilder()
																							.setTapId("alpha")
																							.setGrpcService(
																									GrpcService.newBuilder()
																									.setEnvoyGrpc(
																											EnvoyGrpc.newBuilder()
																											.setClusterName(CLUSTER_ADS)
																											.build())
																									.build())
																							.build())
																					.build())
																			.build())
																	.build())
															.build())
													.build()))
										.build())
								*/
								
								.addFilters(
										Filter.newBuilder()
										.setName("envoy.http_connection_manager")
										.setTypedConfig(
												Any.pack(
													HttpConnectionManager.newBuilder()
													.setForwardClientCertDetails(ForwardClientCertDetails.APPEND_FORWARD)
													.setSetCurrentClientCertDetails(
															SetCurrentClientCertDetails.newBuilder()
															.setCert(true)
															.setChain(true)
															.setDns(true)
															.setUri(true)
															.setSubject(BoolValue.of(true))
															.build())
													.setStatPrefix("echo-service")
													.addAccessLog(
															AccessLog.newBuilder()
															.setName("envoy.http_grpc_access_log")
															.setTypedConfig(
																	Any.pack(
																			HttpGrpcAccessLogConfig.newBuilder()
																			.setCommonConfig(
																					CommonGrpcAccessLogConfig.newBuilder()
																					.setLogName("echo-service-edge")
																					.setGrpcService(
																							GrpcService.newBuilder()
																							.setEnvoyGrpc(
																									EnvoyGrpc.newBuilder()
																									.setClusterName(CLUSTER_ADS)
																									.build())
																							.build())
																					.build())
																			.addAdditionalResponseHeadersToLog("date")
																			.build()))
															.build())
													.setRds(
															Rds.newBuilder()
															.setRouteConfigName(ROUTE_SERVICE)
															.setConfigSource(
																	ConfigSource.newBuilder()
																	.setAds(
																			AggregatedConfigSource.newBuilder()
																			.build())
																	.build())
															.build())
													.addHttpFilters(
															HttpFilter.newBuilder()
															.setName("envoy.router")
															.setTypedConfig(
																	Any.pack(
																			Router.newBuilder()
																			.build()))
															.build())
													.build()))
										.build())
								.build())
						.build())
				.build();

		return result;
	}

	private static List<RouteConfiguration> createRoutes(final long version) {
		final List<RouteConfiguration> result =
				ImmutableList.<RouteConfiguration>builder()
				.add(
					RouteConfiguration.newBuilder()
					.setName(ROUTE_SERVICE)
					.addVirtualHosts(
							VirtualHost.newBuilder()
							.setName("echo-service-host")
							.addDomains("*")
							.addRoutes(
									Route.newBuilder()
									.setMatch(
											RouteMatch.newBuilder()
											.setPrefix("/")
											.build())
									.setRoute(
											RouteAction.newBuilder()
											.setCluster(CLUSTER_SERVICE)
											.build()))
									/*
									.setMetadata(
											Metadata.newBuilder()
											// Add metadata for the access log
											.putFilterMetadata("envoy.http_connection_manager.access_log",
													Struct.newBuilder()
													.putFields("example",
															Value.newBuilder()
															.setStringValue("data")
															.build())
													.build())
											.build())
									.build()
									*/
							.build())
					.addResponseHeadersToAdd(
							HeaderValueOption.newBuilder()
							.setHeader(
									HeaderValue.newBuilder()
									.setKey("x-upstream-host")
									.setValue("%UPSTREAM_METADATA([\"envoy.lb\", \"host\"])%")
									.build())
							.setAppend(BoolValue.of(true))
							.build())
					.build())
				.build();
		
		return result;
	}
}
