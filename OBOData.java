/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package obodata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.writer.OBOFormatWriter;

/**
 *
 * @author Chinh
 */
public class OBOData {
    
    public static String folderout = "E:/semcare";
    public String version = "1.4";
    public String creator = "Your name goes here";
    
    // this file can be found online at http://semanticnetwork.nlm.nih.gov/SemGroups/SemGroups.txt
    public String semanticgroupsfile = "E:/netbeans/OBOData/data/SemGroups.txt";
    public String SRSTRE1file = "E:/netbeans/OBOData/data/SRSTRE1";

    public OBODoc loadOBOfile(String fn) throws Exception {

        OBOFormatParser p = new OBOFormatParser();
        OBODoc obodoc = p.parse(new BufferedReader(new FileReader(fn)));
        return obodoc;
    }

    public void createOBOfile(String dest) throws Exception {

        OBODoc obodoc = new OBODoc();
        // set up header frame 
        obodoc.setHeaderFrame(createHeaderFrame());
        // create term def
        createTermDef(obodoc);
        // Query UMLS for concepts
        DataProvider provider = new DataProvider();
        long t1 = System.currentTimeMillis();
        provider.getMainConcepts(obodoc);
        provider.closeAll();
        long t2 = System.currentTimeMillis();
        System.out.println("Total time querying concepts:\t"+((t2-t1)/1000));
        OBOFormatWriter writer = new OBOFormatWriter();
        writer.write(obodoc, dest+".obo");
        t2 = System.currentTimeMillis();
        System.out.println("Total time:\t"+((t2-t1)/1000));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        OBOData data = new OBOData();
        try {
            data.createOBOfile(folderout);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // TODO: Query Semantic group, semantic type, CUI has semantic type
    // Use Header frame similar to Mantra project (next step)
    /**
     * Create header frame for OBO file
     *
     * @return
     */
    private Frame createHeaderFrame() {
        Frame hFrame = new Frame(Frame.FrameType.HEADER);
        // Date
        Date date = new Date();
        String datestring = new SimpleDateFormat("dd:MM:yyyy HH:ss").format(date);
        Clause c = new Clause(OBOFormatConstants.OboFormatTag.TAG_DATE, datestring);
        hFrame.addClause(c);
        // Format
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_FORMAT_VERSION, version);
        hFrame.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SAVED_BY, creator);
        hFrame.addClause(c);
        // ENG
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "PREFERREDTERM_en");
        c.addValue("preferred terms in English");
        hFrame.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "TERM_en");
        c.addValue("synonyms in English");
        hFrame.addClause(c);
        // GER
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "PREFERREDTERM_de");
        c.addValue("preferred terms in German");
        hFrame.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "TERM_de");
        c.addValue("synonyms in German");
        hFrame.addClause(c);

        // DUTCH
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "PREFERREDTERM_nl");
        c.addValue("preferred terms in Dutch");
        hFrame.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYMTYPEDEF, "TERM_nl");
        c.addValue("synonyms in Dutch");
        hFrame.addClause(c);

        return hFrame;
    }

    private void createTermDef(OBODoc doc) throws Exception {
        // Semantic group
        Frame f = new Frame(Frame.FrameType.TYPEDEF);
        f.setId("has_semantic_group");
        Clause c = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, "has_semantic_group");
        f.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, "has semantic group");
        f.addClause(c);
        c = new Clause("is_transitive", "true");
        f.addClause(c);
        doc.addTypedefFrame(f);
        // Semantic type
        f = new Frame(Frame.FrameType.TYPEDEF);
        f.setId("has_semantic_type");
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, "has_semantic_type");
        f.addClause(c);
        c = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, "has semantic type");
        f.addClause(c);
        c = new Clause("is_transitive", "true");
        f.addClause(c);
        doc.addTypedefFrame(f);
        // create list of semantic group and semantic type
        addSemanticGroupandType(doc);
    }

    private void addSemanticGroupandType(OBODoc doc) {
        try {
            FileReader reader = new FileReader(semanticgroupsfile);
            BufferedReader bf = new BufferedReader(reader);
            String txt, st[];
            Frame f;
            Clause cl;
            // Definition of Semantic types
            while ((txt = bf.readLine()) != null) {
                st = txt.split("\\|");
                f = doc.getTermFrame(st[0]);
                if (f == null) {
                    f = new Frame(Frame.FrameType.TERM);
                    f.setId(st[0]); //group
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, st[0]);
                    f.addClause(cl);
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, st[1]);
                    f.addClause(cl);
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAMESPACE, "umls_semantic_group"); 
                    // add group
                    f.addClause(cl);
                    doc.addTermFrame(f);
                }
                f = doc.getTermFrame(st[2]);
                if (f == null) {
                    f = new Frame(Frame.FrameType.TERM);
                    f.setId(st[2]); // type
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, st[2]);
                    f.addClause(cl);
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, st[3]);
                    f.addClause(cl);
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAMESPACE, "umls_semantic_type"); 
                    f.addClause(cl);
                    cl = new Clause("relationship", "has_semantic_group");
                    cl.addValue(st[0]);
                    f.addClause(cl);
                    // add type
                    doc.addTermFrame(f);
                }
            }
            bf.close();
            reader.close();
            // Relationships of semantic types
            reader = new FileReader(SRSTRE1file);
            bf = new BufferedReader(reader);
            // Definition of Semantic types
            while ((txt = bf.readLine()) != null) {
                st = txt.split("\\|");
                if(!st[1].equals("T186")){ // only add IS_A relationships
                    continue;
                }
                f = doc.getTermFrame(st[0]);
                if (f != null) {
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_IS_A,st[2]);
                    f.addClause(cl);
                }
            }
            bf.close();
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
