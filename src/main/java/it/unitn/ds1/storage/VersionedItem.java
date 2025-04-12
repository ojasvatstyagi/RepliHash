package it.unitn.ds1.storage;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Represent an item saved in the data store.
 * Each item has a value and a version.
 */
public final class VersionedItem implements Serializable {

	// private variables
	private final String value;
	private final int version;

	/**
	 * Create a new versioned item. This stores the value and the version of the item.
	 *
	 * @param value   Value for the item.
	 * @param version Version of the item.
	 */
	public VersionedItem(@Nullable String value, int version) {
		assert version > 0;
		this.value = value;
		this.version = version;
	}

	/**
	 * @return Return the value of the item.
	 */
	@Nullable
	public String getValue() {
		return value;
	}

	/**
	 * @return Return the version of the item.
	 */
	public int getVersion() {
		return version;
	}
}
