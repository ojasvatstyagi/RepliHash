package it.unitn.ds1.storage;

import it.unitn.ds1.storage.exceptions.ReadException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

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
public class FileStorageManager implements StorageManager {

	// use space as record separator as requested from project guidelines
	private static CSVFormat CUSTOM_CSV_FORMAT = CSVFormat.DEFAULT.withDelimiter(' ');

	private String fileLocation;

	public FileStorageManager(@NotNull String storagePath, int nodeId) throws IOException {

		File file = new File(storagePath, "nodeStorage-" + nodeId + ".txt");
		fileLocation = file.getAbsolutePath();

		// check if file exists. If no, create a new one.
		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new RuntimeException("Unable to create new file \"" + fileLocation + "\" for storage purposes.\n" +
					"Please, check the \"storage-path\" key in Akka configuration file.");
			}
		}
	}

	private CSVPrinter getFilePrinter() throws IOException {
		FileWriter fileWriter = new FileWriter(fileLocation);
		return new CSVPrinter(fileWriter, CUSTOM_CSV_FORMAT);
	}

	@Override
	public VersionedItem readRecord(int key) {

		try {
			FileReader fileReader = new FileReader(fileLocation);
			Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);

			for (CSVRecord record : records) {

				validateRecord(record);

				int fileKey = Integer.parseInt(record.get(0));
				if (fileKey == key) {
					fileReader.close();
					return new VersionedItem(record.get(1), Integer.parseInt(record.get(2)));
				}
			}
			fileReader.close();

		} catch (IOException | NumberFormatException e) {
			throw new ReadException(e);
		}

		return null;
	}

	@Override
	public Map<Integer, VersionedItem> readRecords() {

		Map<Integer, VersionedItem> result = new HashMap<>();

		try {
			FileReader fileReader = new FileReader(fileLocation);
			Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);

			for (CSVRecord record : records) {
				validateRecord(record);
				result.put(Integer.parseInt(record.get(0)), new VersionedItem(record.get(1), Integer.parseInt(record.get(2))));
			}

			fileReader.close();
		} catch (IOException | NumberFormatException e) {
			throw new ReadException(e);
		}

		return result;
	}

	@Override
	public void appendRecord(int key, @NotNull VersionedItem versionedItem) {

		try {
			Map<Integer, VersionedItem> fileRecords = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

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

			Map<Integer, VersionedItem> fileRecords = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

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

		try {
			CSVPrinter csvFilePrinter = getFilePrinter();
			for (Map.Entry<Integer, VersionedItem> record : records.entrySet()) {
				List<String> csvRecord = toCsvRecord(record.getKey(), record.getValue());
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

			Map<Integer, VersionedItem> fileRecords = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

			for (Map.Entry<Integer, VersionedItem> fileRecord : fileRecords.entrySet()) {

				Integer fileKey = fileRecord.getKey();
				if (keys.indexOf(fileKey) == -1) {
					csvFilePrinter.printRecord(toCsvRecord(fileRecord));
				}
			}
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void clearStorage() throws IOException {
		CSVPrinter csvFilePrinter = getFilePrinter();
		csvFilePrinter.close();
	}

	@Override
	public void createStorage() throws IOException {
		File file = new File(fileLocation);
		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new RuntimeException("Unable to create new file \"" + fileLocation + "\" for storage purposes.");
			}
		}
	}

	@Override
	public void deleteStorage() {

		File file = new File(fileLocation);
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

			VersionedItem fileRecord = fileRecords.get(key);

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
