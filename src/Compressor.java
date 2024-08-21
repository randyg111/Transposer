import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressor {
    public static void main(String[] args) {
        compress("Tchaikovsky_Violin_Concerto_For_Viola.xml");
    }
    public static void compress(String fileName) {
        try {
            // Read file into DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse("container.xml");
            doc.getDocumentElement().normalize();

            // Extract root file
            String root = ((Element) doc.getElementsByTagName("rootfile").item(0)).getAttribute("full-path");

            // Convert to mxl file
            String newMxl = fileName.substring(0, fileName.length()-4) + "_For_Viola.mxl";

            FileInputStream in = new FileInputStream("container.xml");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newMxl));
            out.putNextEntry(new ZipEntry("META-INF/container.xml"));

            byte[] b = new byte[1024];
            int count;

            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            in.close();

            in = new FileInputStream(fileName);
            out.putNextEntry(new ZipEntry(root));

            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            out.close();
            in.close();

            // Validate the file
            File schemaFile = new File("schema/musicxml.xsd");
            Source xmlFile = new StreamSource(new File(fileName));
            SchemaFactory schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                Schema schema = schemaFactory.newSchema(schemaFile);
                Validator validator = schema.newValidator();
                validator.validate(xmlFile);
                System.out.println(xmlFile.getSystemId() + " is valid");
            } catch (SAXException e) {
                System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }
}
