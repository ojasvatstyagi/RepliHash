package it.unitn.ds1.storage;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Represent an item saved in the data store.
 * Each item has a value and a version.
 */
public final class VersionedItem implements Serializable {

	private final String value;
	private final int version;

	public VersionedItem(String value, int version) {
		assert version > 0;
		this.value = value;
		this.version = version;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "VersionedItem{" +
			"value='" + value + '\'' +
			", version=" + version +
			'}';
	}
}
