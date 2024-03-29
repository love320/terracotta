package org.terracotta.modules.ehcache_1_2_4;

interface IConstants {
	static final String LINKEDHASHMAP_CLASS_NAME_DOTS = "java.util.LinkedHashMap";

	static final String LRUMAP_CLASS_NAME_DOTS = "org.apache.commons.collections.LRUMap";

	static final String LRUMEMORYSTORE_CLASS_NAME_SLASH = "net/sf/ehcache/store/LruMemoryStore";

	static final String LRUMEMORYSTORE_CLASS_NAME_DOTS = "net.sf.ehcache.store.LruMemoryStore";

	static final String SPOOLINGLINKEDHASHMAP_CLASS_NAME_SLASH = LRUMEMORYSTORE_CLASS_NAME_SLASH
			+ "$SpoolingLinkedHashMap";

	static final String SPOOLINGLRUMAP_CLASS_NAME_SLASH = LRUMEMORYSTORE_CLASS_NAME_SLASH
			+ "$SpoolingLRUMap";
}
