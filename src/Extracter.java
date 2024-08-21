import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.zip.ZipFile;

public class Extracter {
    public static void main(String[] args) {
        extract("Tchaikovsky_Violin_Concerto_For_Viola.mxl");
    }
    public static void extract(String fileName) {
        try {
            ZipFile file = new ZipFile(new File(fileName));
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(file.getEntry("META-INF/container.xml"))));
            String newFile = "container.xml";
            PrintWriter pw = new PrintWriter(newFile);
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
            pw.close();

            // Read file into DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(newFile);
            doc.getDocumentElement().normalize();

            String root = ((Element) doc.getElementsByTagName("rootfile").item(0)).getAttribute("full-path");

            br = new BufferedReader(new InputStreamReader(file.getInputStream(file.getEntry(root))));
            newFile = fileName.substring(0, fileName.length() - 4) + ".xml";
            pw = new PrintWriter(newFile);
            while ((line = br.readLine()) != null) {
                line = line.replace("http://www.musicxml.org/dtds", "schema");
                line = line.replace("Violin", "Viola");
                line = line.replace("Vln.", "Vla.");
                pw.println(line);
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
