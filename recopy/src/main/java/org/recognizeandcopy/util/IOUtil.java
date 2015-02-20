package org.recognizeandcopy.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class IOUtil {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    
    public static long copy(InputStream input, OutputStream output) throws IOException {
    	return copy(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }
    
    public static long copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while ( (n = input.read(buffer)) >= 0) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    
    public static long ungzip(InputStream in, OutputStream out) throws IOException {
		try (GzipCompressorInputStream gzIn = new GzipCompressorInputStream(new BufferedInputStream(in))) {
			return copy(gzIn, out);
		}
    }
    
    public static long untar(InputStream in, File outDir) throws IOException {
    	AtomicLong bytesCount = new AtomicLong();
    	untar(in, outDir, bytesCount);
    	return bytesCount.get();
    }
    
    private static void untar(InputStream in, File outDir, AtomicLong bytesCount) throws IOException {
    	TarArchiveInputStream tarIn = new TarArchiveInputStream(in);
    	
    	for (ArchiveEntry entry = tarIn.getNextEntry(); entry != null; entry = tarIn.getNextEntry()) {
    		String name = entry.getName();
    		if(entry.isDirectory()) {
    			new File(outDir, name).mkdirs();
    		} else { 
    			File file = new File(outDir, name);
    			File dir = file.getParentFile();
    			if (!dir.exists()) {
    				dir.mkdirs();
    			}
    			try (OutputStream out = new FileOutputStream(file)) {
    				bytesCount.addAndGet(copy(tarIn, out));
    			}
    		}
    	}
    }
    
    
    public static long untargzip(final InputStream in, final File outDir) throws IOException {
        final PipedOutputStream ungizipOutput = new PipedOutputStream();
        final PipedInputStream  untarInput  = new PipedInputStream(ungizipOutput);
        

        Thread thread1 = new Thread() {
            @Override
            public void run() {
                try {
                    ungzip(in, ungizipOutput);
                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
        };        
        

        final AtomicLong bytesCount = new AtomicLong();
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                try {
                	untar(untarInput, outDir, bytesCount);
                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
        };        
        
        
        thread1.start();
        thread2.start();
        try {
			thread2.join();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
        
        return bytesCount.get();       
    }
    
    
    public static void main(String[] args) throws Exception {
    	long bytes = untargzip(
    			new FileInputStream("C:/proj/private/ForceCopyPaste/recopy/tessdata/tesseract-ocr-3.01.grc.tar.gz"), 
    			new File("C:/proj/private/ForceCopyPaste/recopy/tessdata/tmp"));
    	System.out.println(bytes);
    }
}
