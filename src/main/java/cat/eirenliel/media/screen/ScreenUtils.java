package cat.eirenliel.media.screen;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

public class ScreenUtils {
	
	/**
	 * 
	 * @param bgraBuf screen buffer
	 * @param out BufferedImage in format TYPE_4BYTE_ABGR
	 */
	public static void convertScreenShot(ByteBuffer bgraBuf, BufferedImage out) {
		WritableRaster wr = out.getRaster();
		DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
		
		byte[] cpuArray = db.getData();
		
		// copy native memory to java memory
		bgraBuf.clear();
		bgraBuf.get(cpuArray);
		bgraBuf.clear();
		
		// flip the components the way AWT likes them
		for(int ptr = 0; ptr < cpuArray.length; ptr += 4) {
			byte b1 = cpuArray[ptr + 0];
			byte g1 = cpuArray[ptr + 1];
			byte r1 = cpuArray[ptr + 2];
			byte a1 = cpuArray[ptr + 3];
			cpuArray[ptr + 0] = a1;
			cpuArray[ptr + 1] = b1;
			cpuArray[ptr + 2] = g1;
			cpuArray[ptr + 3] = r1;
		}
	}
}
