package com.hartle_klug.mhartle.sacon2019.control_plane.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class CustomNettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
	private final String address;
	private final int port;

	public CustomNettyWebServerFactoryCustomizer(
			final @Value("${server.address}") String address,
			final @Value("${server.port.web}") int port) {
		this.address = address;
		this.port = port;
	}

	@Override
	public void customize(NettyReactiveWebServerFactory serverFactory) {
		try {
			serverFactory.setAddress(InetAddress.getByName(this.address));
			serverFactory.setPort(this.port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
