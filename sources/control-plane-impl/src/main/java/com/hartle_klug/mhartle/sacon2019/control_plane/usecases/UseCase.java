package com.hartle_klug.mhartle.sacon2019.control_plane.usecases;

import java.util.List;
import java.util.Map;

import io.envoyproxy.controlplane.cache.SnapshotCache;

public interface UseCase {
	public abstract void apply(Map<String, List<String>> parameters, SnapshotCache<String> snapshotCache, long version) throws Exception;
	
	public abstract void unapply(SnapshotCache<String> snapshotCache, long version) throws Exception;
}
