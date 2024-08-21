import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.zip.*;

public class Transposer {
    // index = key (0 = Cb, 7 = C, 14 = C#)
    // first digit = note (0 = A, 1 = B, 2 = C, 3 = D, 4 = E, 5 = F, 6 = G)
    // second digit = accidental (0 = bb, 1 = b, 2 = natural, 3 = #, 4 = ##)
    private static final int[][] circle =
            {
                    {21, 31, 41, 51, 61, 1, 11}, // Cb
                    {61, 1, 11, 21, 31, 41, 52}, // Gb
                    {31, 41, 52, 61, 1, 11, 22}, // Db
                    {1, 11, 22, 31, 41, 52, 62}, // Ab
                    {41, 52, 62, 1, 11, 22, 32}, // Eb
                    {11, 22, 32, 41, 52, 62, 2}, // Bb
                    {52, 62, 2, 11, 22, 32, 42}, // F
                    {22, 32, 42, 52, 62, 2, 12}, // C
                    {62, 2, 12, 22, 32, 42, 53}, // G
                    {32, 42, 53, 62, 2, 12, 23}, // D
                    {2, 12, 23, 32, 42, 53, 63}, // A
                    {42, 53, 63, 2, 12, 23, 33}, // E
                    {12, 23, 33, 42, 53, 63, 3}, // B
                    {53, 63, 3, 12, 23, 33, 43}, // F#
                    {23, 33, 43, 53, 63, 3, 13}  // C#
            };

    public static void main(String[] args) {
        transpose(new File("Violin_Concerto_Op.35__Pyotr_Ilyich_Tchaikovsky.mxl"));
    }
    public static void transpose(File fileName) {
        try {
            // Unzip file
            ZipFile file = new ZipFile(fileName);
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

            // Extract root file
            String root = ((Element) doc.getElementsByTagName("rootfile").item(0)).getAttribute("full-path");

            br = new BufferedReader(new InputStreamReader(file.getInputStream(file.getEntry(root))));
            newFile = removeExtension(fileName) + ".xml";
            pw = new PrintWriter(newFile);
            while ((line = br.readLine()) != null) {
                line = line.replace("http://www.musicxml.org/dtds", "schema");
                line = line.replace("Violin", "Viola");
                line = line.replace("Vln.", "Vla.");
                pw.println(line);
            }
            pw.close();

            doc = db.parse(newFile);
            doc.getDocumentElement().normalize();

            // Extract viola part
            Element parts = (Element) doc.getElementsByTagName("part-list").item(0);
            String id = "";
            NodeList nodes = parts.getElementsByTagName("score-part");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Element elem = (Element) node;
                Element partName = (Element) elem.getElementsByTagName("part-name").item(0);
                if (partName.getTextContent().equals("Viola"))
                    id = elem.getAttribute("id");
                else
                    parts.removeChild(node);
            }

            // Remove other parts
            nodes = doc.getElementsByTagName("part");
            for (int i = nodes.getLength()-1; i >= 0; i--) {
                Node node = nodes.item(i);
                Element elem = (Element) node;
                if (!elem.getAttribute("id").equals(id))
                    node.getParentNode().removeChild(node);
            }

            // Press M to enable multi measure rests

            // Stay in treble clef if the majority of notes is higher than E5
            // for at least 4 measures in a row
            // or if the measure contains a C6 or higher
            nodes = doc.getElementsByTagName("measure");
            boolean[] treble = new boolean[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Element elem = (Element) node;
                NodeList notes = elem.getElementsByTagName("note");
                int count = 0;
                int total = 0;
                for (int j = 0; j < notes.getLength(); j++) {
                    Node note = notes.item(j);
                    Element ele = (Element) note;
                    Element pitch = (Element) ele.getElementsByTagName("pitch").item(0);
                    if (pitch != null) {
                        String step = pitch.getElementsByTagName("step").item(0).getTextContent();
                        int octave = Integer.parseInt(pitch.getElementsByTagName("octave").item(0).getTextContent());
                        // G6 in treble clef
                        if (octave > 6 || octave == 6 && (step.equals("G") || step.equals("A") || step.equals("B"))) {
                            treble[i] = true;
                            break;
                        }

                        // B5 in treble clef
                        if (octave > 5) {
                            count++;
                        }
                        total++;
                    }
                }
                if (total > 0 && count * 2 >= total) {
                    treble[i] = true;
                }
            }

            // Convert consecutive measures in treble clef to alto clef
            // if there are less than 4 in a row
            int cons = 0;
            for (int i = 0; i <= nodes.getLength(); i++) {
                if (i == nodes.getLength() || !treble[i]) {
                    if (cons < 4) {
                        for (int j = i - cons; j < i; j++) {
                            treble[j] = false;
                        }
                    }
                    cons = 0;
                } else {
                    cons++;
                }
            }

            // Convert consecutive measures in alto clef to treble clef
            // if there are less than 4 in a row
            cons = 0;
            for (int i = 0; i <= nodes.getLength(); i++) {
                if (i == nodes.getLength() || treble[i]) {
                    if (cons < 4) {
                        for (int j = i - cons; j < i; j++) {
                            treble[j] = true;
                        }
                    }
                    cons = 0;
                } else {
                    cons++;
                }
            }

            // Iterate measure by measure
            int currKey = -1;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Element elem = (Element) node;

                // Remove forced page breaks
                Element print = (Element) elem.getElementsByTagName("print").item(0);
                if (print != null) {
                    elem.removeChild(print);
                }

                // Store current key
                Element attributes = (Element) elem.getElementsByTagName("attributes").item(0);
                if (attributes != null) {
                    Element key = (Element) attributes.getElementsByTagName("key").item(0);
                    if (key != null) {
                        currKey = Integer.parseInt(key.getElementsByTagName("fifths").item(0).getTextContent());
                        currKey += 7;
                    }
                    if (i == 0 && !treble[i]) {
                        Element clef = (Element) attributes.getElementsByTagName("clef").item(0);
                        clef.getElementsByTagName("sign").item(0).setTextContent("C");
                        clef.getElementsByTagName("line").item(0).setTextContent("3");
                    }
                }

                // Change clef
                if (i < nodes.getLength()-1 && treble[i+1] && !treble[i]) {
                    if (attributes == null) {
                        attributes = doc.createElement("attributes");
                    }
                    Element clef = doc.createElement("clef");
                    Element sign = doc.createElement("sign");
                    sign.setTextContent("G");
                    clef.appendChild(sign);
                    attributes.appendChild(clef);

                    // Shift clef in measure
//                    NodeList notes = elem.getElementsByTagName("note");
//                    Node last = null;
//                    for (int j = notes.getLength()-1; j >= 0; j--) {
//                        Node note = notes.item(j);
//                        Element ele = (Element) note;
//                        Element pitch = (Element) ele.getElementsByTagName("pitch").item(0);
//                        if (pitch == null)
//                            break;
//                        int octave = Integer.parseInt(pitch.getElementsByTagName("octave").item(0).getTextContent());
//                        if (octave > 5) {
//                            last = note;
//                        } else {
//                            break;
//                        }
//                    }
//                    if (last != null)
//                        elem.insertBefore(attributes, last);
//                    else
                        elem.appendChild(attributes);

                    elem.appendChild(attributes);
                } else if (i < nodes.getLength()-1 && !treble[i+1] && treble[i]) {
//                    Element measure = (Element) nodes.item(i+1);
                    if (attributes == null) {
                        attributes = doc.createElement("attributes");
                    }
                    Element clef = doc.createElement("clef");
                    Element sign = doc.createElement("sign");
                    sign.setTextContent("C");
                    clef.appendChild(sign);
                    attributes.appendChild(clef);
//                    NodeList notes = measure.getElementsByTagName("note");
//                    Node last = null;
//                    for (int j = 0; j < nodes.getLength(); j++) {
//                        Node note = notes.item(j);
//                        Element ele = (Element) note;
//                        Element pitch = (Element) ele.getElementsByTagName("pitch").item(0);
//                        if (pitch == null) {
//                            if (last != null)
//                                last = note;
//                            break;
//                        }
//                        int octave = Integer.parseInt(pitch.getElementsByTagName("octave").item(0).getTextContent());
//                        if (octave <= 5) {
//                            last = note;
//                            break;
//                        } else {
//                            last = note;
//                        }
//                    }
//                    if (last != null)
//                        measure.insertBefore(attributes, last);
//                    else
                        elem.appendChild(attributes);

                }

                // Check for multiple voices
                NodeList notes = elem.getElementsByTagName("note");
                boolean voiced = false;
                for (int j = 0; j < notes.getLength(); j++) {
                    Node note = notes.item(j);
                    Element ele = (Element) note;
                    Element voice = (Element) ele.getElementsByTagName("voice").item(0);
                    if (voice != null && Integer.parseInt(voice.getTextContent()) > 1) {
                        voiced = true;
                        break;
                    }
                }

                // Shift notes down a perfect fifth
                for (int j = 0; j < notes.getLength(); j++) {
                    Node note = notes.item(j);
                    Element ele = (Element) note;

                    Element pitch = (Element) ele.getElementsByTagName("pitch").item(0);
                    Element stem = (Element) ele.getElementsByTagName("stem").item(0);
                    if (pitch != null) {
                        Node step = pitch.getElementsByTagName("step").item(0);
                        String st = step.getTextContent();
                        Node alter = pitch.getElementsByTagName("alter").item(0);
                        int alt = alter == null ? 0 : Integer.parseInt(alter.getTextContent());
                        alt += 2;
                        int oldNote = 10 * getIndex(st) + alt;
                        int deg = -1;
                        for (int k = 0; k < circle[currKey].length; k++) {
                            if (oldNote / 10 == circle[currKey][k] / 10) {
                                deg = k;
                                break;
                            }
                        }
                        int newNote = circle[currKey-1][deg] + oldNote - circle[currKey][deg];
                        Node octave = pitch.getElementsByTagName("octave").item(0);
                        int oct = Integer.parseInt(octave.getTextContent());
                        if (oldNote/10 >= 2 && oldNote/10 <= 5) {
                            oct--;
                        }

                        step.setTextContent(getNote(newNote/10));
                        if (alter != null) {
                            alter.setTextContent(Integer.toString(newNote%10 - 2));
                        } else if (newNote % 10 != 2) {
                            Element add = doc.createElement("alter");
                            add.setTextContent(Integer.toString(newNote%10 - 2));
                            pitch.insertBefore(add, octave);
                        }
                        octave.setTextContent(Integer.toString(oct));

                        if (stem != null && !voiced) {
                            if (oct >= 4) {
                                stem.setTextContent("down");
                            } else {
                                stem.setTextContent("up");
                            }
                        }
                    } else {
                        // Remove rest formatting
                        Element rest = (Element) ele.getElementsByTagName("rest").item(0);
                        if (rest != null) {
                            Element step = (Element) rest.getElementsByTagName("display-step").item(0);
                            Element octave = (Element) rest.getElementsByTagName("display-octave").item(0);
                            if (step != null) {
                                rest.removeChild(step);
                                rest.removeChild(octave);
                            }
                        }
                    }
                }
            }

            // Change key
            nodes = doc.getElementsByTagName("fifths");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                int val = Integer.parseInt(node.getTextContent());
                node.setTextContent(Integer.toString(val-1));
            }

            // Convert to xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(newFile));

            transformer.transform(source, result);

            // Convert to mxl file
            String newMxl = removeExtension(fileName) + "_For_Viola.mxl";

            FileInputStream in = new FileInputStream("container.xml");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newMxl));
            out.putNextEntry(new ZipEntry("META-INF/container.xml"));

            byte[] b = new byte[1024];
            int count;

            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            in.close();

            in = new FileInputStream(newFile);
            out.putNextEntry(new ZipEntry(root));

            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            out.close();
            in.close();

            // Validate the file
            File schemaFile = new File("schema/musicxml.xsd");
            Source xmlFile = new StreamSource(new File(newFile));
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

            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String removeExtension(File s) {
        return s.getName().substring(0, s.getName().length()-4);
    }

    private static int getIndex(String s) {
        return s.charAt(0) - 'A';
    }

    private static String getNote(int i) {
        return Character.toString((char) ('A' + i));
    }
}
