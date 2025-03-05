package it.unitn.ds1.storage;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage Manager implementation based on file.
 */
public class FileStorageManager implements StorageManager {

	public static String FILE_LOCATION = "/tmp/csvFile.csv";

	public static void createFile(String location) throws IOException {
		File file = new File(location);
		if (!file.exists()) {
			boolean created = file.createNewFile();
		}
	}

	private String fileLocation;

	public FileStorageManager(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	private CSVPrinter getFilePrinter() throws IOException {
		FileWriter fileWriter = new FileWriter(fileLocation);
		return new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
	}

	@Override
	public VersionedItem readRecord(String key) throws IOException {

		FileReader fileReader = new FileReader(fileLocation);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(fileReader);

		for (CSVRecord record : records) {
			String fileKey = record.get(0);
			if (fileKey.equals(key)) {
				fileReader.close();
				return new VersionedItem(record.get(1), Integer.parseInt(record.get(2)));
			}
		}

		fileReader.close();
		return null;
	}

	@Override
	public Map<String, VersionedItem> readRecords() throws IOException {

		FileReader fileReader = new FileReader(fileLocation);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(fileReader);

		Map<String, VersionedItem> result = new HashMap<>();
		for (CSVRecord record : records) {
			result.put(record.get(0), new VersionedItem(record.get(1), Integer.parseInt(record.get(2))));
		}

		fileReader.close();
		return result;
	}

	@Override
	public void appendRecord(String key, VersionedItem versionedItem) throws IOException {

		Map<String, VersionedItem> records = readRecords();
		CSVPrinter csvFilePrinter = getFilePrinter();

		for (Map.Entry<String, VersionedItem> record : records.entrySet()) {

			String fileKey = record.getKey();
			if (!fileKey.equals(key)) {
				csvFilePrinter.printRecord(toCsvRecord(record));
			}
		}
		csvFilePrinter.printRecord(toCsvRecord(key, versionedItem));
		csvFilePrinter.close();
	}

	@Override
	public void appendRecords(Map<String, VersionedItem> records) throws IOException {
		Map<String, VersionedItem> fileRecords = readRecords();
		CSVPrinter csvFilePrinter = getFilePrinter();

		for (Map.Entry<String, VersionedItem> fileRecord : fileRecords.entrySet()) {

			String fileKey = fileRecord.getKey();
			if (!records.containsKey(fileKey)) {
				csvFilePrinter.printRecord(toCsvRecord(fileRecord));
			}
		}

		for (Map.Entry<String, VersionedItem> record : records.entrySet()) {
			csvFilePrinter.printRecord(toCsvRecord(record));
		}
		csvFilePrinter.close();
	}

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

	@Override
	public void writeRecords(Map<String, VersionedItem> records) throws IOException {
		CSVPrinter csvFilePrinter = getFilePrinter();
		for (Map.Entry<String, VersionedItem> record : records.entrySet()) {
			List<String> csvRecord = toCsvRecord(record.getKey(), record.getValue());
			csvFilePrinter.printRecord(csvRecord);
		}
		csvFilePrinter.close();
	}

	@Override
	public void removeRecords(List<String> keys) throws IOException {
		Map<String, VersionedItem> fileRecords = readRecords();
		CSVPrinter csvFilePrinter = getFilePrinter();

		for (Map.Entry<String, VersionedItem> fileRecord : fileRecords.entrySet()) {

			String fileKey = fileRecord.getKey();
			if (keys.indexOf(fileKey) == -1) {
				csvFilePrinter.printRecord(toCsvRecord(fileRecord));
			}
		}
		csvFilePrinter.close();
	}
}
