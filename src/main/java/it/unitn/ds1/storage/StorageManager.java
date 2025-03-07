package it.unitn.ds1.storage;

import it.unitn.ds1.storage.exceptions.ReadException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for storage manager.
 */
public interface StorageManager {

	/**
	 * Read a single record from storage
	 *
	 * @param key key of the record to read
	 * @return the record or null if it is not found
	 * @throws ReadException thrown if any error during reading occur (IOException, bad file format, ...)
	 */
	VersionedItem readRecord(@NotNull String key) throws ReadException;

	/**
	 * Read all the records in storage
	 *
	 * @return a map <key, record> containing all read records
	 * @throws ReadException thrown if any error during reading occur (IOException, bad file format, ...)
	 */
	Map<String, VersionedItem> readRecords() throws ReadException;

	/**
	 * Add a record into the storage.
	 * If record already exists, it will be overwritten.
	 *
	 * @param key           key of the record
	 * @param versionedItem record
	 * @throws ReadException  thrown if any error during reading occur (IOException, bad file format, ...)
	 * @throws WriteException thrown if any error during writing occur
	 */
	void appendRecord(@NotNull String key, @NotNull VersionedItem versionedItem) throws ReadException, WriteException;

	/**
	 * Add records into the storage.
	 * If a record already exists, it will be overwritten.
	 *
	 * @param records records to append
	 * @throws ReadException  thrown if any error during reading occur (IOException, bad file format, ...)
	 * @throws WriteException thrown if any error during writing occur
	 */
	void appendRecords(@NotNull Map<String, VersionedItem> records) throws ReadException, WriteException;

	/**
	 * Write provided records into the storage.
	 * All previous record in the storage will be deleted.
	 *
	 * @param records records to write
	 * @throws WriteException thrown if any error during writing occur
	 */
	void writeRecords(@NotNull Map<String, VersionedItem> records) throws WriteException;

	/**
	 * Remove provided records from the storage, if they exist.
	 *
	 * @param keys key of records to remove
	 * @throws WriteException thrown if any error during writing occur
	 */
	void removeRecords(@NotNull List<String> keys) throws WriteException;

	/**
	 * Remove all records from storage
	 *
	 * @throws WriteException thrown if any error during writing occur
	 */
	void clearStorage() throws WriteException, IOException;
}
