package it.unitn.ds1.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for storage manager.
 */
public interface StorageManager {

	VersionedItem readRecord(String key) throws IOException;

	Map<String, VersionedItem> readRecords() throws IOException;

	void appendRecord(String key, VersionedItem versionedItem) throws IOException;

	void appendRecords(Map<String, VersionedItem> records) throws IOException;

	void writeRecords(Map<String, VersionedItem> records) throws IOException;

	void removeRecords(List<String> keys) throws IOException;
}
