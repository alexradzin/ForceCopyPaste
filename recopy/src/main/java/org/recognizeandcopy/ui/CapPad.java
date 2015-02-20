package org.recognizeandcopy.ui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.recognizeandcopy.ocr.OCR;
import org.recognizeandcopy.ocr.OCREventHandler;
import org.recognizeandcopy.ocr.tesseract.TesseractOCR;

@SuppressWarnings("serial")
public class CapPad extends JFrame implements MouseMotionListener, MouseListener, OCREventHandler {
	private final JDialog rect;
	private final Robot robot;
	private final OCR ocr;
	private final TrayIcon trayIcon;

	public CapPad(int closeOperation) throws AWTException, IOException {
		setLayout(new BorderLayout(0, 0));
		setAlwaysOnTop(true);
		setUndecorated(true);
		final float defaultOpacity = 0.55f;
		setOpacity(defaultOpacity);

		rect = new JDialog(this);
		rect.setUndecorated(true);
		rect.setOpacity(0.77f);
		rect.setSize(100, 100);

		robot = new Robot();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(screenSize);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image image = toolkit.getImage(getClass().getResource("/OCR.png"));
		Point hotSpot = new Point(0, 0);  
		Cursor cursor = toolkit.createCustomCursor(image, hotSpot, "OCR");
		setCursor(cursor);
		
		if (SystemTray.isSupported()) {
			trayIcon = new TrayIcon(image);
			SystemTray.getSystemTray().add(trayIcon);
		} else {
			trayIcon = null;
		}
		
		ocr = new TesseractOCR(this);
		displayMessage(
				"Recongize & copy", 
				"Switch to relevant language and select area where text is written using mouse", 
				org.recognizeandcopy.ocr.OCREventHandler.Type.INFO);
	}

	public static BufferedImage scale(BufferedImage img, double coef) {
		int width = (int) (coef * img.getWidth());
		int height = (int) (coef * img.getHeight());
		return resize(img, width, height);
	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH,
				BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	public static void main(String[] args) throws AWTException, IOException {
		CapPad pad = new CapPad(JFrame.EXIT_ON_CLOSE);
		pad.setVisible(true);
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		rect.setSize(e.getX() - rect.getX(), e.getY() - rect.getY());
		rect.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		System.out.println("pressed!");
		// rect = new JDialog(this);
		// rect.setUndecorated(true);
		// rect.setOpacity(0.77f);
		rect.setLocation(e.getX(), e.getY());
		// rect.setSize(100, 100);
		rect.setVisible(true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// capture
		Point absLoc = rect.getLocationOnScreen();
		Rectangle screenRect = new Rectangle(absLoc.x, absLoc.y, rect.getWidth(), rect.getHeight());
		rect.setVisible(false);
		this.setVisible(false);
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedImage capture = scale(robot.createScreenCapture(screenRect), 4);

		String text = ocr.parse(capture);
		
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
		System.exit(0);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void displayMessage(String header, String text, org.recognizeandcopy.ocr.OCREventHandler.Type type) {
		TrayIcon.MessageType messageType = TrayIcon.MessageType.valueOf(type.name()); 
		trayIcon.displayMessage(header, text, messageType);
	}
}
