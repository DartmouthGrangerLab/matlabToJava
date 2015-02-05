import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/**
 *
 *
 * @author Eli Bowen
 * @since Aug 14, 2014
 */
public class Persistence {
    public static void SaveObject (String fileName, Object obj) {
        String fileNameString = fileName.replace(".zip","");
        try {
            FileOutputStream fout = new FileOutputStream(fileNameString + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fout);
            ZipEntry ze = new ZipEntry(fileNameString + ".ser");
            zos.putNextEntry(ze);
            ObjectOutputStream oos = new ObjectOutputStream(zos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            fout.close();
        } catch (Exception ex) {
            throw new RuntimeException("Error in save to file of developmental image foveas!");
        }
    }


    public static Object LoadObject (String fileName) {
        String fileNameString = fileName.replace(".zip","") + ".zip"; //make it end in .zip unless it already does
        Object retVal = null;
        try {
            FileInputStream fin = new FileInputStream(fileNameString);
            ZipInputStream zin = new ZipInputStream(fin);
            zin.getNextEntry();
            ObjectInputStream ois = new ObjectInputStream(zin);
            retVal = ois.readObject();
            ois.close();
            fin.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return retVal;
    }


    public static void SaveCSV (int[] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (int curr : data) {
                out.println(curr);
            }
            out.close();
        } catch (Exception ex) {
            System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    public static void SaveCSV (Integer[] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (int curr : data) {
                out.println(curr);
            }
            out.close();
        } catch (Exception ex) {
        	System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    public static void SaveCSV (double[] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (double curr : data) {
                out.println(curr);
            }
            out.close();
        } catch (Exception ex) {
        	System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    public static void SaveCSV (String[] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (String curr : data) {
                out.println(curr);
            }
            out.close();
        } catch (Exception ex) {
        	System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    public static void SaveCSV (double[][] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (double[] currRow : data) {
                boolean firstPrint = true;
                for (double currElement : currRow) {
                    if (firstPrint == false) {
                        out.print(",");
                    }
                    out.print(currElement);
                    firstPrint = false;
                }
                out.println();
            }
            out.close();
        } catch (Exception ex) {
        	System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    public static void SaveCSV (int[][] data, Path savePath) {
        try {
            File file = savePath.toFile();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            for (int[] currRow : data) {
                boolean firstPrint = true;
                for (int currElement : currRow) {
                    if (firstPrint == false) {
                        out.print(",");
                    }
                    out.print(currElement);
                    firstPrint = false;
                }
                out.println();
            }
            out.close();
        } catch (Exception ex) {
        	System.err.println("Persistance.SaveCSV(): Can not create file: " + ex);
        }
    }
    
}
