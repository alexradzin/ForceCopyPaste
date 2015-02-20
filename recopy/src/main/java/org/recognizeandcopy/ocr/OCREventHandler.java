package org.recognizeandcopy.ocr;

public interface OCREventHandler {
	public static enum Type {
		NONE, INFO, WARNING, ERROR
	}
	
	
	public void displayMessage(String header, String text, Type type);
}
