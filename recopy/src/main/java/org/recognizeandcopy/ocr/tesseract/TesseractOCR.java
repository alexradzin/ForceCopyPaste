package org.recognizeandcopy.ocr.tesseract;

import java.awt.im.InputContext;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.recognizeandcopy.ocr.OCR;
import org.recognizeandcopy.ocr.OCREventHandler;
import org.recognizeandcopy.util.IOUtil;

public class TesseractOCR implements OCR {
	private final OCREventHandler eventHandler;
    private final File homeDir;
	
	public TesseractOCR(OCREventHandler eventHandler) throws IOException {
		this.eventHandler = eventHandler;
		homeDir = discoverHomeDir();
		install();
		init();
	}
	


	@Override
	public String parse(BufferedImage capture) {
		try {
			String lang = getCurrentInputLanguage();
			if (!installLanguage(lang)) {
				throw new UnsupportedOperationException("Unsupported language " + lang);
			}
			Tesseract instance = Tesseract.getInstance();
			instance.setDatapath(getDataPath());
			instance.setLanguage(lang);
			String text = instance.doOCR(capture);
			return text;
		} catch (TesseractException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String getDataPath() {
		return new File(homeDir, "tessdata").getAbsolutePath();
	}
	
	private void install() throws IOException {
		installLib();
		installLanguage(getCurrentInputLanguage());
	}


	private void installLib() throws IOException {
		eventHandler.displayMessage("Tesseract", "Installing native library", OCREventHandler.Type.INFO);
		File lib = getLibDir();
		String fileTemplate = "%s.%s";
		String resourceTemplate = "/%s/%s.%s";
		String[] binaries = new String[] {"liblept168", "libtesseract302"}; 
		
		String dir = is32bits() ? "x86" : "x64";
		String ext = isWindows() ? "dll" : "so";

		lib.mkdirs();
		for (String bin : binaries) {
			File binFile = new File(lib, String.format(fileTemplate, bin, ext));
			if (binFile.exists()) {
				continue;
			}
			try (FileOutputStream fileOut = new FileOutputStream(binFile)) {
				String path = String.format(resourceTemplate, dir, bin, ext);
				IOUtil.copy(getClass().getResourceAsStream(path), fileOut);
			}
		}
		eventHandler.displayMessage("Tesseract", "Native library has been installed successfully", OCREventHandler.Type.INFO);
	}

	private boolean installLanguage(String lang) {
		if (isLanguageInstalled(lang)) {
			return true;
		}
		try {
			// TODO: invoke this method asynchronously?
			return installLanguage(homeDir.getParentFile(), lang);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String getCurrentInputLanguage() {
		return InputContext.getInstance().getLocale().getISO3Language();
	}
	
	
	private boolean installLanguage(File home, String language) throws IOException {
		eventHandler.displayMessage("Tesseract", "Installing languge definitions for " + language, OCREventHandler.Type.INFO);
		Pattern langUrlPattern = Pattern.compile("<a href=\"(//tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.(.+?).tar.gz)\"");
		URL url = new URL("https://code.google.com/p/tesseract-ocr/downloads/list?num=10000&start=0");
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			Matcher m = langUrlPattern.matcher(line);
			if (!m.find()) {
				continue;
			}
			String actualLanguage = m.group(2);
			if (!actualLanguage.equals(language)) {
				continue;
			}
			URL langPackUrl = new URL("http:" + m.group(1));
			IOUtil.untargzip(langPackUrl.openStream(), home);
			eventHandler.displayMessage("Tesseract", "Languge definitions for " + language + " have been installed successfully", OCREventHandler.Type.INFO);
			return true;
		}
		eventHandler.displayMessage("Tesseract", "Languge definitions for " + language + " were not found", OCREventHandler.Type.WARNING);
		return false;
	}
	
	private void init() {
		System.setProperty("jna.library.path", getLibDir().getAbsolutePath());
		System.out.println(System.getProperty("jna.library.path"));
	}
	
	
	private boolean is32bits() {
		return "32".equals(System.getProperty("sun.arch.data.model"));
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}
	
	

    private File discoverHomeDir() {
    	File tmp = new File(System.getProperty("java.io.tmpdir"));
    	File tesseractOcrHome = new File(tmp, "tesseract-ocr");
    	return tesseractOcrHome;
    }

    private File getLibDir() {
    	return new File(homeDir, "lib");
    }
    
    private boolean isLanguageInstalled(String lang) {
    	return new File(getDataPath(), lang + ".traineddata").exists();
    }
}
