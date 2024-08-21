import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;

public class Validator {
    public static void main(String[] args) {
        // Incomplete measure: Full score, measure 216, staff 1. Found: 140/32. Expected: 17/4.
        // Incomplete measure: Full score, measure 216, staff 1. Found: 235/32. Expected: 157/32.
        validate("Violin_Concerto_Op.35__Pyotr_Ilyich_Tchaikovsky.xml");
    }
    public static void validate(String file) {
        File schemaFile = new File("schema/musicxml.xsd");
        Source xmlFile = new StreamSource(new File(file));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(schemaFile);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException | IOException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
        }

    }
}
