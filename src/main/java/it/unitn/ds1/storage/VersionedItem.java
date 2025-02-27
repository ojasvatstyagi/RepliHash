package it.unitn.ds1.storage;

/**
 * Represent an item saved in the data store.
 * Each item has a value and a version.
 */
public class VersionedItem {

	private final String value;
	private final int version;

	public VersionedItem(String value, int version) {
		this.value = value;
		this.version = version;
	}

	public String getValue() {
		return value;
	}

	public int getVersion() {
		return version;
	}
}
