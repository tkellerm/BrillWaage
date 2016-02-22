package de.abasgmbh.brill.waage;

import java.net.*;
import java.io.*;
import java.awt.*;

/**
 *
 * @author Akshathkumar Shetty
 */
public class Util {
    
    
    
    public static boolean checkHost(String host) {
        try {
            InetAddress.getByName(host);
            return(true);
        } catch(UnknownHostException uhe) {
            return(false);
        }
    }
    
    public static void writeFile(String fileName, String text)
    throws IOException {
        PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(fileName)));
        out.print(text);
        out.close();
    }
    
    public static String readFile(String fileName, Object parent)
    throws IOException {
        StringBuffer sb = new StringBuffer();
        ClassLoader cl = parent.getClass().getClassLoader();
        InputStream is = cl.getResourceAsStream(fileName);
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String s;
        while((s = in.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        in.close();
        return sb.toString();
    }    
}
