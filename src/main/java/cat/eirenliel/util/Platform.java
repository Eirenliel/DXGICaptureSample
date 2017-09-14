package cat.eirenliel.util;

import java.io.File;

public class Platform {
	
	private static boolean platformLoaded = false;
	private static File storageFolder;
	
	public static synchronized void load() throws UnsupportedHardwareException {
		if(platformLoaded)
			return;
		try {
			Natives.extractNatives(OperatingSystem.WINDOWS);
		} catch(Exception e) {
			throw new UnsupportedHardwareException("Can not initialize platform", e);
		}
		platformLoaded = true;
	}
	
	public static File getStorageFolder() {
		if(storageFolder != null)
			return storageFolder;
		 File workingFolder = new File("").getAbsoluteFile();
         if (!workingFolder.canWrite()) {
        	 workingFolder = new File(System.getProperty("user.home"), ".screenbridge");
         } else {
             try {
                 File file = new File(workingFolder.getAbsolutePath() + File.separator + ".sbtestwrite");
                 file.createNewFile();
                 file.delete();
             } catch (Exception e) {
            	 workingFolder = new File(System.getProperty("user.home"), ".screenbridge");
             }
         }
         if(workingFolder.exists())
        	 workingFolder.mkdir();
         storageFolder = workingFolder;
         return storageFolder;
	}
}
