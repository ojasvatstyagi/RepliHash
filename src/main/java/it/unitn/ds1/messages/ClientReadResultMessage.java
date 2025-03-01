package it.unitn.ds1.messages;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class ClientReadResultMessage implements Serializable {

	private final int id;
	private final int key;
	private final String value;

	public ClientReadResultMessage(int id, int key, @Nullable String value) {
		this.id = id;
		this.key = key;
		this.value = value;
	}


	public int getId() {
		return id;
	}

	public int getKey() {
		return key;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	public boolean keyFound() {
		return value != null;
	}
}
