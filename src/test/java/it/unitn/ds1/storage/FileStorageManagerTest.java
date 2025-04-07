package it.unitn.ds1.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test storage manger
 */
public final class FileStorageManagerTest {

	private static final int NODE_ID = 10;

	private static final String storageFilePath = "/tmp/" + "nodeStorage-" + NODE_ID + ".txt";
	private static final String storageFileDirectory = "/tmp";

	@Before
	public void prepareStorage() throws IOException {

		File file = new File(storageFilePath);
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();

		InputStream input = FileStorageManagerTest.class.getResourceAsStream("/it/unitn/ds1/storage/sampleStorage.txt");
		OutputStream output = new FileOutputStream(storageFilePath);

		byte[] buf = new byte[1024];
		int bytesRead;
		while ((bytesRead = input.read(buf)) > 0) {
			output.write(buf, 0, bytesRead);
		}

		input.close();
		output.close();
	}

	@After
	public void removeStorage() {
		File file = new File(storageFilePath);
		if (file.exists()) {
			file.delete();
		}
	}

	@Test
	public void readRecords() throws IOException {

		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);

		Map<Integer, VersionedItem> records = storageManager.readRecords();
		assertEquals(5, records.size());
	}

	@Test
	public void readRecord() throws IOException {

		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);

		VersionedItem record = storageManager.readRecord(17);
		assertNotNull(record);
		assertEquals("valueC", record.getValue());
		assertEquals(4, record.getVersion());
	}

	@Test
	public void appendNewRecord() throws IOException {

		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);
		storageManager.appendRecord(20, new VersionedItem("value20", 1));

		assertEquals(6, storageManager.readRecords().size());

		VersionedItem record = storageManager.readRecord(20);
		assertNotNull(record);
		assertEquals("value20", record.getValue());
		assertEquals(1, record.getVersion());
	}

	@Test
	public void appendExistentRecord() throws IOException {

		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);
		storageManager.appendRecord(12, new VersionedItem("newValue", 16));

		assertEquals(5, storageManager.readRecords().size());

		VersionedItem record = storageManager.readRecord(12);
		assertNotNull(record);
		assertEquals("newValue", record.getValue());
		assertEquals(16, record.getVersion());
	}

	@Test
	public void writeRecords() throws IOException {
		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);

		Map<Integer, VersionedItem> records = new HashMap<>();
		records.put(100, new VersionedItem("val100", 1));
		records.put(200, new VersionedItem("val200", 1));

		storageManager.writeRecords(records);

		assertEquals(2, storageManager.readRecords().size());
	}

	@Test
	public void removeRecords() throws IOException {
		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);

		List<Integer> keys = new ArrayList<>();
		keys.add(13);
		keys.add(17);

		storageManager.removeRecords(keys);
		assertEquals(3, storageManager.readRecords().size());
		assertNull(storageManager.readRecord(13));
		assertNull(storageManager.readRecord(17));
	}


	@Test
	public void clearRecords() throws IOException {
		StorageManager storageManager = new FileStorageManager(storageFileDirectory, NODE_ID);

		storageManager.clearStorage();
		assertEquals(0, storageManager.readRecords().size());
	}
}
