package org.recognizeandcopy.ocr;

import java.awt.image.BufferedImage;

public interface OCR {
	String parse(BufferedImage image);
}
