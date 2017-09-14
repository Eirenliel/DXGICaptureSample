package cat.eirenliel.media.screen;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import cat.eirenliel.util.OperatingSystem;
import cat.eirenliel.util.Platform;
import cat.eirenliel.util.UnsupportedHardwareException;

public abstract class ScreenCaptureManager implements Closeable {
	
	private static final AtomicInteger fileHandlers = new AtomicInteger(0);
	
	public abstract void open() throws IOException;
	
	public abstract void close();
	
	public abstract boolean isAlive();
	
	public abstract ScreenView getScreenView();
	
	public abstract BufferedImage captureFrame();
	
	public abstract BufferedImage captureFrame(BufferedImage target);
	
	public static ScreenCaptureManager createScreenCaptureManager() {
		switch(OperatingSystem.getCurrentPlatform()) {
		case WINDOWS:
			return new WindowsScreenCaptureManager(new File(Platform.getStorageFolder(), ".screenhandle-" + fileHandlers.getAndIncrement()));
		default:
			throw new UnsupportedHardwareException("Operating system " + OperatingSystem.getCurrentPlatform() + " is not supported");
		}
	}
}
