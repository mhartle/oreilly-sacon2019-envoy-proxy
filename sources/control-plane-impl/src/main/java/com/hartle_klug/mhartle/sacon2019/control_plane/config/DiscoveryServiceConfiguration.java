package com.hartle_klug.mhartle.sacon2019.control_plane.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import com.hartle_klug.mhartle.sacon2019.control_plane.util.JsonUtils;

import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.cache.SnapshotCache;
import io.envoyproxy.envoy.api.v2.core.Node;

@Configuration
public class DiscoveryServiceConfiguration {
	private static final Logger LOG = LoggerFactory.getLogger(DiscoveryServiceConfiguration.class);
	
	@Bean
	public SnapshotCache<String> cache() {
		return new SimpleCache<>(this::mapNodeMetadataToKey);
	}

	private String mapNodeMetadataToKey(final Node node) {
		try {			
			// Map node metadata protobuf to JSON string
			final String jsonMetadata = JsonFormat.printer().print(node.getMetadata());
						
			// Map metadata JSON string to map
			final ObjectMapper objectMapper = new ObjectMapper();
			final Map<String, Object> data =
					objectMapper.readValue(
							jsonMetadata,
							new TypeReference<Map<String, Object>>() {});
			
			// Map metadata map to id
			final String key = (String) JsonUtils.get(data, "role");
			
			LOG.debug("Mapped {} to key {}.", jsonMetadata, key);
			
			return key;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "unknown";
	}
}
