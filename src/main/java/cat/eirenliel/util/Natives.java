/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cat.eirenliel.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for extracting the natives (dll, so) from the jars.
 * This class should only be used internally.
 */
public final class Natives {

    private static final Logger logger = Logger.getLogger(Natives.class.getName());
    private static final byte[] buf = new byte[1024];
    private static File extractionDirOverride = null;
    private static File extractionDir = null;

    public static void setExtractionDir(String name) {
        extractionDirOverride = new File(name).getAbsoluteFile();
    }

    public static File getExtractionDir() {
        if (extractionDirOverride != null) {
            return extractionDirOverride;
        }
        if (extractionDir == null) {
            File workingFolder = new File("").getAbsoluteFile();
            if (!workingFolder.canWrite()) {
                setStorageExtractionDir();
            } else {
                try {
                    File file = new File(workingFolder.getAbsolutePath() + File.separator + ".sbtestwrite");
                    file.createNewFile();
                    file.delete();
                    extractionDir = workingFolder;
                } catch (Exception e) {
                    setStorageExtractionDir();
                }
            }
        }
        return extractionDir;
    }

    private static void setStorageExtractionDir() {
        logger.log(Level.WARNING, "Working directory is not writable. Using home directory instead.");
        extractionDir = new File(Platform.getStorageFolder(),
                "natives_" + Integer.toHexString(computeNativesHash()));
        if (!extractionDir.exists()) {
            extractionDir.mkdir();
        }
    }

    private static int computeNativesHash() {
        URLConnection conn = null;
        try {
            String classpath = System.getProperty("java.class.path");
            URL url = Thread.currentThread().getContextClassLoader().getResource("cat/eirenliel/util/Natives.class");

            StringBuilder sb = new StringBuilder(url.toString());
            if (sb.indexOf("jar:") == 0) {
                sb.delete(0, 4);
                sb.delete(sb.indexOf("!"), sb.length());
                sb.delete(sb.lastIndexOf("/") + 1, sb.length());
            }
            try {
                url = new URL(sb.toString());
            } catch (MalformedURLException ex) {
                throw new UnsupportedOperationException(ex);
            }

            conn = url.openConnection();
            int hash = classpath.hashCode() ^ (int) conn.getLastModified();
            return hash;
        } catch (IOException ex) {
            throw new UnsupportedOperationException(ex);
        } finally {
            if (conn != null) {
                try {
                    conn.getInputStream().close();
                    conn.getOutputStream().close();
                } catch (IOException ex) { }
            }
        }
    }

    public static void extractNativeLib(String sysName, String name) throws IOException {
        extractNativeLib(sysName, name, false, true);
    }

    public static void extractNativeLib(String sysName, String name, boolean load) throws IOException {
        extractNativeLib(sysName, name, load, true);
    }

    public static void extractNativeLib(String sysName, String name, boolean load, boolean warning) throws IOException {
        String fullname;
        String path;
        //XXX: hack to allow specifying the extension via supplying an extension in the name (e.g. "blah.dylib")
        //     this is needed on osx where the openal.dylib always needs to be extracted as dylib
        //     and never as jnilib, even if that is the platform JNI lib suffix (OpenAL is no JNI library)
        if(!name.contains(".")){
            // automatic name mapping
            fullname = System.mapLibraryName(name);
            path = "native/" + sysName + "/" + fullname;
            //XXX: Hack to extract jnilib to dylib on OSX Java 1.7+
            //     This assumes all jni libs for osx are stored as "jnilib" in the jar file.
            //     It will be extracted with the name required for the platform.
            //     At a later stage this should probably inverted so that dylib is the default name.
            if(sysName.equals("macosx")){
                path = path.replaceAll("dylib","jnilib");
            }
        } else{
            fullname = name;
            path = "native/" + sysName + "/" + fullname;
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if (url == null) {
            if (!warning) {
                logger.log(Level.WARNING, "Cannot locate native library: {0}/{1}",
                        new String[]{sysName, fullname});
            }
            return;
        }

        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        File targetFile = new File(getExtractionDir(), fullname);
        OutputStream out = null;
        try {
            if (targetFile.exists()) {
                // OK, compare last modified date of this file to 
                // file in jar
                long targetLastModified = targetFile.lastModified();
                long sourceLastModified = conn.getLastModified();

                // Allow ~1 second range for OSes that only support low precision
                if (targetLastModified + 1000 > sourceLastModified) {
                    logger.log(Level.FINE, "Not copying library {0}. Latest already extracted.", fullname);
                    return;
                }
            }

            out = new FileOutputStream(targetFile);
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            in = null;
            out.close();
            out = null;

            // NOTE: On OSes that support "Date Created" property, 
            // this will cause the last modified date to be lower than
            // date created which makes no sense
            targetFile.setLastModified(conn.getLastModified());
        } catch (FileNotFoundException ex) {
            if (ex.getMessage().contains("used by another process")) {
                return;
            }

            throw ex;
        } finally {
            if (load) {
                System.load(targetFile.getAbsolutePath());
            }
            if(in != null){
                in.close();
            }
            if(out != null){
                out.close();
            }
        }
        logger.log(Level.FINE, "Copied {0} to {1}", new Object[]{fullname, targetFile});
    }
    
    public static void extractNativeFile(String sysName, String name) throws IOException {
    	String path = "native/" + sysName + "/" + name;
    	URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if (url == null) {
            return;
        }

        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        File targetFile = new File(getExtractionDir(), name);
        OutputStream out = null;
        try {
            if (targetFile.exists()) {
                // OK, compare last modified date of this file to 
                // file in jar
                long targetLastModified = targetFile.lastModified();
                long sourceLastModified = conn.getLastModified();

                // Allow ~1 second range for OSes that only support low precision
                if (targetLastModified + 1000 > sourceLastModified) {
                    logger.log(Level.FINE, "Not copying library {0}. Latest already extracted.", name);
                    return;
                }
            }

            out = new FileOutputStream(targetFile);
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            in = null;
            out.close();
            out = null;

            // NOTE: On OSes that support "Date Created" property, 
            // this will cause the last modified date to be lower than
            // date created which makes no sense
            targetFile.setLastModified(conn.getLastModified());
        } catch (FileNotFoundException ex) {
            if (ex.getMessage().contains("used by another process")) {
                return;
            }

            throw ex;
        } finally {
            if(in != null){
                in.close();
            }
            if(out != null){
                out.close();
            }
        }
        logger.log(Level.FINE, "Copied {0} to {1}", new Object[]{name, targetFile});
    }

    public static void extractNatives(OperatingSystem os) throws IOException {
        switch(os) {
        case WINDOWS:
        	extractNativeFile("windows", "ScreenCaptureBridge.exe");
        	break;
        default:
        	break;
        }
    }
}
