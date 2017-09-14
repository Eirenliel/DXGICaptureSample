package cat.eirenliel.media.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import cat.eirenliel.util.OperatingSystem;
import cat.eirenliel.util.Platform;
import cat.eirenliel.util.UnsupportedHardwareException;

public class WindowsScreenCaptureManager extends ScreenCaptureManager {
	
	static {
		Platform.load();
	}
	
	private File targetFile;
	private RandomAccessFile openedFile;
	private FileChannel fileChannel;
	private MappedByteBuffer memory;
	private Process process;
	
	protected WindowsScreenCaptureManager(File targetFile) {
		if(OperatingSystem.getCurrentPlatform() != OperatingSystem.WINDOWS)
			throw new UnsupportedHardwareException("Operating system " + OperatingSystem.getCurrentPlatform() + " is not supported by this implimentation");
		targetFile.deleteOnExit();
	}

	@Override
	public void open() throws IOException {
		if(process != null)
			return; // Already opened
		openedFile = new RandomAccessFile(targetFile, "r");
		fileChannel = openedFile.getChannel();
		memory = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
	}
	
	@Override
	public boolean isAlive() {
		return process != null && process.isAlive();
	}

	@Override
	public void close() {
		if(process == null)
			return; // Already closed
		
		targetFile.delete();
	}

	@Override
	public ScreenView getScreenView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage captureFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage captureFrame(BufferedImage target) {
		ScreenUtils.convertScreenShot(memory, target);
		return target;
	}
	
}
