package com.hartle_klug.mhartle.sacon2019.control_plane.usecases;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.envoyproxy.controlplane.cache.SnapshotCache;

public class NoOpUseCase extends AbstractUseCase {
	private static final Logger LOG = LoggerFactory.getLogger(HttpFrontProxyUseCase.class);

	@Override
	public void apply(Map<String, List<String>> parameters, SnapshotCache<String> snapshotCache, long version) throws Exception {
		LOG.info("Skipped applying use case.");
	}

	@Override
	public void unapply(SnapshotCache<String> snapshotCache, long version) throws Exception {
		LOG.info("Skipped unapplying use case.");
	}
}
