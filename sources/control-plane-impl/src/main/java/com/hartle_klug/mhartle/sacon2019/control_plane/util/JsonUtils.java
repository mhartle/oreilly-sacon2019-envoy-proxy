package com.hartle_klug.mhartle.sacon2019.control_plane.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    @SuppressWarnings("unchecked")
    public static Object get(Map<String, Object> object, String path) {
            final List<String> pathSegments = path.contains(".") ? Arrays.asList(path.split("\\.")) : Arrays.asList(path);
            final Iterator<String> iterator = pathSegments.iterator();

            Object current = object;
            while(iterator.hasNext()) {
                    final String key = iterator.next();
                    if (current instanceof Map) {
                            final Map<String, Object> currentMap = (Map<String, Object>)current;
                            if (currentMap.containsKey(key)) {
                                    current = currentMap.get(key);
                            } else {
                                    return null;
                            }
                    }
            }

            return current;
    }

    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> object, String path, Object value) {
            final List<String> pathSegments = path.contains(".") ? Arrays.asList(path.split("\\.")) : Arrays.asList(path);
            final Iterator<String> iterator = pathSegments.iterator();

            Object result = object;
            Map<String, Object> currentMap = object;
            String key = null;
            while(iterator.hasNext()) {
                    key = iterator.next();
                    if (!iterator.hasNext()) {
                            break;
                    }

                    if (result instanceof Map) {
                            currentMap = (Map<String, Object>)result;
                            if (currentMap.containsKey(key)) {
                                    result = currentMap.get(key);
                            } else {
                                    result = new LinkedHashMap<String, Object>();
                                    currentMap.put(key, result);
                                    currentMap = (Map<String, Object>)result;
                            }
                    }
            }

            currentMap.put(key, value);
    }
}
