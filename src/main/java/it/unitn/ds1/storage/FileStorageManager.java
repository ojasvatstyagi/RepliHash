package it.unitn.ds1.storage;

import it.unitn.ds1.storage.exceptions.ReadException;
import it.unitn.ds1.storage.exceptions.WriteException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage Manager implementation based on file.
 */
public final class FileStorageManager implements StorageManager {

	// use space as record separator as requested from project guidelines
	private static final CSVFormat CUSTOM_CSV_FORMAT = CSVFormat.DEFAULT.withDelimiter(' ');


	// location of the file for this node
	private final String fileLocation;

	/**
	 * Create a new file-based storage for the node with the given ID.
	 * Each node is given a unique file name in order to be able to run
	 * multiple nodes on the same host without conflicts.
	 *
	 * @param directory Directory where to store the file.
	 * @param nodeID    ID of the node that uses this storage.
	 * @throws IOException If it is not possible to read or create the file.F
	 */
	public FileStorageManager(@NotNull String directory, int nodeID) throws IOException {

		// locate the file where to write
		final File file = new File(directory, "nodeStorage-" + nodeID + ".txt");
		this.fileLocation = file.getAbsolutePath();

		// check if file exists... if not, create a new one
		if (!file.exists()) {
			final boolean created = file.createNewFile();
			if (!created) {
				throw new RuntimeException("Unable to create new file \"" + fileLocation + "\" for storage purposes.\n" +
					"Please, check the \"storage-path\" key in Akka configuration file.");
			}
		}
	}

	@Nullable
	@Override
	public VersionedItem readRecord(int key) {
		try (FileReader fileReader = new FileReader(fileLocation)) {

			// get all records
			final Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);
			for (CSVRecord record : records) {
				validateRecord(record);

				// return the item, if found
				final int fileKey = Integer.parseInt(record.get(0));
				if (fileKey == key) {
					return new VersionedItem(record.get(1), Integer.parseInt(record.get(2)));
				}
			}

		} catch (IOException | NumberFormatException e) {
			throw new ReadException(e);
		}

		// if here, no key was found
		return null;
	}

	@NotNull
	@Override
	public Map<Integer, VersionedItem> readRecords() {

		// map to store all items
		final Map<Integer, VersionedItem> result = new HashMap<>();

		try (FileReader fileReader = new FileReader(fileLocation)) {

			// put all records in the map
			final Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);
			for (CSVRecord record : records) {
				validateRecord(record);
				result.put(Integer.parseInt(record.get(0)), new VersionedItem(record.get(1), Integer.parseInt(record.get(2))));
			}

		} catch (IOException | NumberFormatException e) {
			throw new ReadException(e);
		}

		// return the map with all values
		return result;
	}

	@Override
	public void appendRecord(int key, @NotNull VersionedItem versionedItem) {
		try {
			final Map<Integer, VersionedItem> fileRecords = readRecords();
			final CSVPrinter csvFilePrinter = getFilePrinter();

			updateRecordMap(fileRecords, key, versionedItem);

			// save updated map
			for (Map.Entry<Integer, VersionedItem> record : fileRecords.entrySet()) {
				csvFilePrinter.printRecord(toCsvRecord(record));
			}

			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void appendRecords(@NotNull Map<Integer, VersionedItem> records) {
		try {

			final Map<Integer, VersionedItem> fileRecords = readRecords();
			final CSVPrinter csvFilePrinter = getFilePrinter();

			// update map with new records
			for (Map.Entry<Integer, VersionedItem> record : records.entrySet()) {
				updateRecordMap(fileRecords, record.getKey(), record.getValue());
			}

			// save updated map
			for (Map.Entry<Integer, VersionedItem> fileRecord : fileRecords.entrySet()) {
				csvFilePrinter.printRecord(toCsvRecord(fileRecord));
			}

			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void writeRecords(@NotNull Map<Integer, VersionedItem> records) {

		// replace all old records with the new ones
		try {
			final CSVPrinter csvFilePrinter = getFilePrinter();
			for (Map.Entry<Integer, VersionedItem> record : records.entrySet()) {
				final List<String> csvRecord = toCsvRecord(record.getKey(), record.getValue());
				csvFilePrinter.printRecord(csvRecord);
			}
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void removeRecords(@NotNull List<Integer> keys) {
		try {

			// read current records
			final Map<Integer, VersionedItem> fileRecords = readRecords();

			final CSVPrinter csvFilePrinter = getFilePrinter();

			for (Map.Entry<Integer, VersionedItem> fileRecord : fileRecords.entrySet()) {
				final Integer fileKey = fileRecord.getKey();
				if (keys.indexOf(fileKey) == -1) {
					csvFilePrinter.printRecord(toCsvRecord(fileRecord));
				}
			}
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	/**
	 * Clear the storage.
	 * NB: Instead of clearing the file, it closes the file stream.
	 * Once the file will be reopened, it will be wiped.
	 *
	 * @throws WriteException on file I/O exceptions
	 */
	@Override
	public void clearStorage() throws WriteException {
		try {
			final CSVPrinter csvFilePrinter = getFilePrinter();
			csvFilePrinter.close();
		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	/**
	 * Delete the file in which the keys are contained
	 */
	@Override
	public void deleteStorage() {
		final File file = new File(fileLocation);
		if (file.exists()) {
			boolean delete = file.delete();
			if (!delete) {
				throw new RuntimeException("Unable to delete file \"" + fileLocation + "\".");
			}
		}
	}

	/* -----
	 * Utils
	 ----- */

	private CSVPrinter getFilePrinter() throws IOException {
		final FileWriter fileWriter = new FileWriter(fileLocation);
		return new CSVPrinter(fileWriter, CUSTOM_CSV_FORMAT);
	}

	/**
	 * Add a record to record map according to some criteria:
	 * 1. If record is not in the record map, add it
	 * 2. If record is already in the record map, substitute the old record only if this one
	 * as a lower or equal version than the new record. Otherwise keep the old record.
	 *
	 * @param fileRecords The record map to update
	 * @param key         The key of the record to add
	 * @param newRecord   The data of the record to add
	 */
	private void updateRecordMap(Map<Integer, VersionedItem> fileRecords, Integer key, VersionedItem newRecord) {

		if (fileRecords.containsKey(key)) {
			final VersionedItem fileRecord = fileRecords.get(key);

			// Check if record to append has a greater version than the local record
			// Otherwise local record will be kept
			if (fileRecord.getVersion() <= newRecord.getVersion()) {
				fileRecords.put(key, newRecord);
			}
		} else {
			fileRecords.put(key, newRecord);
		}
	}

	private List<String> toCsvRecord(int key, @NotNull VersionedItem versionedItem) {
		List<String> csvRecord = new ArrayList<>();
		csvRecord.add(key + "");
		csvRecord.add(versionedItem.getValue());
		csvRecord.add(versionedItem.getVersion() + "");
		return csvRecord;
	}

	private List<String> toCsvRecord(@NotNull Map.Entry<Integer, VersionedItem> record) {
		List<String> csvRecord = new ArrayList<>();
		csvRecord.add(record.getKey() + "");
		csvRecord.add(record.getValue().getValue());
		csvRecord.add(record.getValue().getVersion() + "");
		return csvRecord;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void validateRecord(@NotNull CSVRecord record) {

		if (record.size() != 3) {
			throw new ReadException("Read bad record. Key, value or version is missing for record \"" + record.toString() + "\".");
		}

		try {
			Integer.parseInt(record.get(0));
		} catch (NumberFormatException e) {
			throw new ReadException("Read bad record. Key of record \"" + record.toString() + "\" is not a valid number.");
		}

		try {
			Integer.parseInt(record.get(2));
		} catch (NumberFormatException e) {
			throw new ReadException("Read bad record. Version of record \"" + record.toString() + "\" is not a valid number.");
		}
	}
}
