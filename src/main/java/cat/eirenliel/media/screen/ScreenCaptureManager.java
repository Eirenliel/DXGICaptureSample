package cat.eirenliel.media.screen;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

public abstract class ScreenCaptureManager implements Closeable {
	
	public abstract void open() throws IOException;
	
	public abstract void close();
	
	public abstract boolean isAlive();
	
	public abstract ScreenView getScreenView();
	
	public abstract BufferedImage captureFrame();
	
	public abstract BufferedImage captureFrame(BufferedImage target);
}
