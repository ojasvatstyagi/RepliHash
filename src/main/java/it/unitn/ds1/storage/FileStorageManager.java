package it.unitn.ds1.storage;

import it.unitn.ds1.SystemConstants;
import it.unitn.ds1.storage.exceptions.ReadException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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

	private static StorageManager storageManager;
	private String fileLocation;

	private FileStorageManager(int nodeId) throws IOException {
		fileLocation = SystemConstants.STORAGE_LOCATION + "/" + "nodeStorage-" + nodeId + ".txt";

		// check if file exists. If no, create a new one.
		File file = new File(fileLocation);
		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new RuntimeException("Unable to create new file \"" + fileLocation + "\" for storage purposes.");
			}
		}
	}

	public static StorageManager getInstance(int nodeId) throws IOException {
		if (storageManager == null) {
			storageManager = new FileStorageManager(nodeId);
		}
		return storageManager;
	}

	private CSVPrinter getFilePrinter() throws IOException {
		FileWriter fileWriter = new FileWriter(fileLocation);
		return new CSVPrinter(fileWriter, CUSTOM_CSV_FORMAT);
	}

	@Override
	public VersionedItem readRecord(@NotNull String key) {

		try {
			FileReader fileReader = new FileReader(fileLocation);
			Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);

			for (CSVRecord record : records) {

				validateRecord(record);

				String fileKey = record.get(0);
				if (fileKey.equals(key)) {
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
	public Map<String, VersionedItem> readRecords() {

		Map<String, VersionedItem> result = new HashMap<>();

		try {
			FileReader fileReader = new FileReader(fileLocation);
			Iterable<CSVRecord> records = CUSTOM_CSV_FORMAT.parse(fileReader);

			for (CSVRecord record : records) {
				validateRecord(record);
				result.put(record.get(0), new VersionedItem(record.get(1), Integer.parseInt(record.get(2))));
			}

			fileReader.close();
		} catch (IOException | NumberFormatException e) {
			throw new ReadException(e);
		}

		return result;
	}

	@Override
	public void appendRecord(@NotNull String key, @NotNull VersionedItem versionedItem) {

		try {
			Map<String, VersionedItem> records = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

			for (Map.Entry<String, VersionedItem> record : records.entrySet()) {

				String fileKey = record.getKey();

				if (!fileKey.equals(key)) { // don't copy the record that has to be appended
					csvFilePrinter.printRecord(toCsvRecord(record));
				}
			}
			// append new record
			csvFilePrinter.printRecord(toCsvRecord(key, versionedItem));
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void appendRecords(@NotNull Map<String, VersionedItem> records) {

		try {

			Map<String, VersionedItem> fileRecords = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

			for (Map.Entry<String, VersionedItem> fileRecord : fileRecords.entrySet()) {

				String fileKey = fileRecord.getKey();
				if (!records.containsKey(fileKey)) { // don't copy the records that has to be appended
					csvFilePrinter.printRecord(toCsvRecord(fileRecord));
				}
			}

			// append new records
			for (Map.Entry<String, VersionedItem> record : records.entrySet()) {
				csvFilePrinter.printRecord(toCsvRecord(record));
			}
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}


	@Override
	public void writeRecords(@NotNull Map<String, VersionedItem> records) {

		try {
			CSVPrinter csvFilePrinter = getFilePrinter();
			for (Map.Entry<String, VersionedItem> record : records.entrySet()) {
				List<String> csvRecord = toCsvRecord(record.getKey(), record.getValue());
				csvFilePrinter.printRecord(csvRecord);
			}
			csvFilePrinter.close();

		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	@Override
	public void removeRecords(@NotNull List<String> keys) {

		try {

			Map<String, VersionedItem> fileRecords = readRecords();
			CSVPrinter csvFilePrinter = getFilePrinter();

			for (Map.Entry<String, VersionedItem> fileRecord : fileRecords.entrySet()) {

				String fileKey = fileRecord.getKey();
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


	/* -----
	 * Utils
	 ----- */

	private List<String> toCsvRecord(String key, VersionedItem versionedItem) {
		List<String> csvRecord = new ArrayList<>();
		csvRecord.add(key);
		csvRecord.add(versionedItem.getValue());
		csvRecord.add(versionedItem.getVersion() + "");
		return csvRecord;
	}

	private List<String> toCsvRecord(Map.Entry<String, VersionedItem> record) {
		List<String> csvRecord = new ArrayList<>();
		csvRecord.add(record.getKey());
		csvRecord.add(record.getValue().getValue());
		csvRecord.add(record.getValue().getVersion() + "");
		return csvRecord;
	}

	private void validateRecord(CSVRecord record) {

		if (record.size() != 3) {
			throw new ReadException("Read bad record. Key, value or version is missing for record \"" + record.toString() + "\".");
		}

		try {
			Integer.parseInt(record.get(2));
		} catch (NumberFormatException e) {
			throw new ReadException("Read bad record. Version of record \"" + record.toString() + "\" is not a valid number.");
		}
	}
}
