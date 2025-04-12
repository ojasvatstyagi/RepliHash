package it.unitn.ds1.storage;

import it.unitn.ds1.storage.exceptions.ReadException;
import it.unitn.ds1.storage.exceptions.WriteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	 * @return he record or null if it is not found
	 * @throws ReadException thrown if any error during reading occur (IOException, bad file format, ...)
	 */
	@Nullable
	VersionedItem readRecord(int key) throws ReadException;

	/**
	 * Read all the records in storage
	 *
	 * @return a map <key, record> containing all read records
	 * @throws ReadException thrown if any error during reading occur (IOException, bad file format, ...)
	 */
	@NotNull
	Map<Integer, VersionedItem> readRecords() throws ReadException;

	/**
	 * Add a record into the storage.
	 * If a record already exists and has a lower or the same version, it will be overwritten.
	 * Otherwise the more recent version will be kept.
	 *
	 * @param key           key of the record
	 * @param versionedItem record
	 * @throws ReadException  thrown if any error during reading occur (IOException, bad file format, ...)
	 * @throws WriteException thrown if any error during writing occur
	 */
	void appendRecord(int key, @NotNull VersionedItem versionedItem) throws ReadException, WriteException;

	/**
	 * Add records into the storage.
	 * If a record already exists and has a lower or the same version, it will be overwritten.
	 * Otherwise the more recent version will be kept.
	 *
	 * @param records records to append
	 * @throws ReadException  thrown if any error during reading occur (IOException, bad file format, ...)
	 * @throws WriteException thrown if any error during writing occur
	 */
	void appendRecords(@NotNull Map<Integer, VersionedItem> records) throws ReadException, WriteException;

	/**
	 * Write provided records into the storage.
	 * All previous record in the storage will be deleted.
	 *
	 * @param records records to write
	 * @throws WriteException thrown if any error during writing occur
	 */
	void writeRecords(@NotNull Map<Integer, VersionedItem> records) throws WriteException;

	/**
	 * Remove provided records from the storage, if they exist.
	 *
	 * @param keys key of records to remove
	 * @throws WriteException thrown if any error during writing occur
	 */
	void removeRecords(@NotNull List<Integer> keys) throws WriteException;

	/**
	 * Remove all records from storage.
	 *
	 * @throws WriteException thrown if any error during writing occur
	 */
	void clearStorage() throws WriteException;

	/**
	 * Delete the persistent storage.
	 */
	void deleteStorage();
}
