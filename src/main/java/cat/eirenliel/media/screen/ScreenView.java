package cat.eirenliel.media.screen;

import java.nio.ByteBuffer;

public interface ScreenView {
	
	public int width();
	
	public int height();
	
	public int readBGRA(int x, int y);
	
	public int readARGB(int x, int y);
	
	public void copyFrameBGRA(byte[] targetArray);
	
	public void copyFrameARGB(ByteBuffer targetBuffer);
	
}
