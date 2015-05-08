/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package obodata;

/**
 *
 * @author Chinh
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;

// assume that conn is an already created JDBC connection (see previous examples)
public class DataProvider {

    Statement stmt = null;
    ResultSet rs = null;
    Connection conn = null;
    PreparedStatement psSym = null;
    PreparedStatement psType = null;
    PreparedStatement psRel = null;
    PreparedStatement psDef = null;
    Map<String, String> langMap = new HashMap<>();
    public DataProvider() {
        langMap.put("ENG", "en");
        langMap.put("DUT", "nl");
        langMap.put("GER", "de");
    }

    private void init() {
        try {
            System.setProperty("file.encoding" , "UTF-8");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            // 
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/umls2014aa?useUnicode=yes&characterEncoding=UTF-8", "umls2014aa", "erasmusmc");            
            String sql = "select * from MRCONSO where cui=? and LAT in('ENG','GER','DUT') AND SUPPRESS='N' order by CUI, STR ";
            psSym = conn.prepareStatement(sql);
            String sql2 = "select * from MRSTY where cui=?";
            psType = conn.prepareStatement(sql2);
            // Rel
            String sqlrel = "select CUI2 from MRREL where cui1=? and REL='PAR'";
            psRel = conn.prepareStatement(sqlrel);
            // Def
            String sqldef = "select DEF from MRDEF where cui=?";
            psDef = conn.prepareStatement(sqldef);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getLang(String lang){
        String value = langMap.get(lang);
        if(value!=null){
            return value;
        }else {
            return "en";
        }
    }
    public void getMainConcepts(OBODoc doc) throws Exception {
        try {
            //System.setProperty("file.encoding" , "UTF-8");
            // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            stmt = conn.createStatement();
            // Get list of prefer CUI in ENGLISH
            String sql = "select * from MRCONSO where LAT ='ENG' and SUPPRESS ='N' and ispref='Y' and TS='P' order by CUI";
            // get prefer term, namespace
            rs = stmt.executeQuery(sql);
            String CUI = "";
            int concept_count=0;
            while (rs.next()) { // loop over list of prefer terms
                // while the same cui: - get prefer term (STT=PF)-> Term (name) ; get SAB list (namespace) ;
                String cui = rs.getString("CUI");
                if (!cui.equals(CUI)) { // start new CUI
                    boolean should_add;
                    Frame f = new Frame(Frame.FrameType.TERM);
                    CUI = cui; // assign new CUI
                    f.setId(CUI);
                    Clause cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, CUI);
                    f.addClause(cl);
                    String term =rs.getString("STR");
                    term= term.replaceAll("\\{", "[");
                    term= term.replaceAll("}", "]");
                    
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, term);
                    f.addClause(cl);
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAMESPACE, "umls_terms"); //rs.getString("SAB")
                    f.addClause(cl);
                    
                    // get Definition
                    getDef(f,CUI);
                    // get synonyms list
                    getSynonyms(f, CUI);
                    //get Semantic type
                    should_add =getSemanticType(f, CUI);
                    if(should_add){
                       doc.addTermFrame(f); 
                       concept_count++;
                    }
                    //set Relationship (is a)
                    getRel(f, CUI);
                   // System.out.println(term);
                }
            }
            // get type from mrsty
            // get synonyms: 
            // preperared statement
            //select * from mrconso where cui=? and LAT in ('ENG') and SUPPRESS ='N' and SAB in ('MSH','MDR','SNOMEDCT_US','SNOMEDCT')
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                stmt = null;
            }
        }

    }

    public void getSynonyms(Frame f, String CUI) throws Exception {
        try { 
            // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            // get prefer term, namespace
            // loop over term fram list: next step
            psSym.setString(1, CUI);
            ResultSet rs1 = psSym.executeQuery();
            String TERM = "";
            String sym_type = "";
            Set<Xref> namespace = new HashSet<>(20);
            while (rs1.next()) {
                // while the same cui: - get prefer term (STT=PF)-> Term (name) ; get SAB list (namespace) ;
                String term = rs1.getString("STR");
                if (!term.equals(TERM)) { // start new CUI
                    if (!namespace.isEmpty() && !TERM.isEmpty()) { // store this term
                        Clause cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, TERM);
                        cl.addValue(OBOFormatConstants.OboFormatTag.TAG_EXACT);
                        cl.addValue(sym_type);
                        cl.setXrefs(namespace);
                        f.addClause(cl);
                    }
                   
                     //print text here  

                    TERM = term;//reset term
                    String lang= getLang(rs1.getString("LAT"));
                    if (rs1.getString("TS").equals("P") && rs1.getString("ISPREF").equals("Y")) {
                        sym_type = "PREFERREDTERM_" + lang;
                    } else {
                        sym_type = "TERM_" + lang;
                    }
                    namespace = new HashSet<>(20);
                }
                String xf_val =rs1.getString("SAB") + ":" + rs1.getString("CODE");
                xf_val= xf_val.replace("\\", "");
                Xref xf = new Xref(xf_val);
                namespace.add(xf);
            }
            //  save last record, 
            if (!namespace.isEmpty() && !TERM.isEmpty()) { // store this term
                Clause cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, TERM);
                cl.addValue(OBOFormatConstants.OboFormatTag.TAG_EXACT);
                cl.addValue(sym_type);
                cl.setXrefs(namespace);
                f.addClause(cl);
            }
            rs1.close();
            // get type from mrsty
            // get synonyms: 
            // preperared statement
            //select * from mrconso where cui=? and LAT in ('ENG') and SUPPRESS ='N' and SAB in ('MSH','MDR','SNOMEDCT_US','SNOMEDCT')

           // os.close();//herman code
            
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
        }

    }

    public boolean getSemanticType(Frame f, String CUI) throws Exception {
        boolean has_semtype =false;
        try {
            
            // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            psType.setString(1, CUI);
            ResultSet rs1 = psType.executeQuery();
            Clause cl;
            while (rs1.next()) { // one record only
                // while the same cui: - get prefer term (STT=PF)-> Term (name) ; get SAB list (namespace) ;
                String semType = rs1.getString("TUI");
                if (semType != null && !semType.isEmpty()) {
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_RELATIONSHIP, "has_semantic_type");
                    cl.addValue(rs1.getString("TUI"));
                    f.addClause(cl);
                    has_semtype =true;
                }
            }
            rs1.close();
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
        }
        return has_semtype;
    }
    
    public void getDef(Frame f, String CUI) throws Exception {
        try {

            // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            psDef.setString(1, CUI);
            ResultSet rs1 = psDef.executeQuery();
            Clause cl;
            while (rs1.next()) { // one record only
                // while the same cui: - get prefer term (STT=PF)-> Term (name) ; get SAB list (namespace) ;
                String def =rs1.getString("DEF");
                cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_DEF, def);
                f.addClause(cl);
                break; // First definition
            }
            rs1.close();
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
        }

    }
    
    public void getRel(Frame f, String CUI) throws Exception {
        try {

            // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            psRel.setString(1, CUI);
            ResultSet rs1 = psRel.executeQuery();
            Clause cl;
            Set<String> cuiSet = new HashSet<>();
            while (rs1.next()) { // one record only
                // while the same cui: - get prefer term (STT=PF)-> Term (name) ; get SAB list (namespace) ;
                String cui2 = rs1.getString("CUI2");
                if(!cuiSet.contains(cui2) && !cui2.equals(CUI)){
                    cl = new Clause(OBOFormatConstants.OboFormatTag.TAG_IS_A, cui2);
                    f.addClause(cl);
                    cuiSet.add(cui2);
                }
            }
            rs1.close();
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
        }

    }
    

    private String hashToStr(Set<String> set) {
        String v = "";
        for (String s : set) {
            if (v.isEmpty()) {
                v = s;
            } else {
                v = v + "|" + s;
            }
        }
        return v;
    }

    public void closeAll() {
        try {
            if (conn != null) {
                if (!conn.isClosed()) {
                    conn.commit();
                    conn.close();
                }
            }
        } catch (Exception ex) {

        }
    }

    /**
     * Creating temp table containing CUI, AUI, DEF,TYPE from
     *
     * @param con
     */
    public void selectCUIs(Connection con) {
        /**
         * CREATE TEMPORARY TABLE IF NOT EXISTS temp_table ( INDEX(col_2) )
         * ENGINE=MyISAM AS ( SELECT col_1, coll_2, coll_3 FROM mytable )
         */
    }
    public void testUnicode(){
        try {
        init();
        String sql= "Select str from MRCONSO where CUI like 'C0000084'";
        // The newInstance() call is a work around for some
            // broken Java implementations
            if (conn == null) {
                init();
            }
            System.out.println("ガンマ-カルボキシグルタミン酸塩");
            stmt = conn.createStatement();
            ResultSet rs1 = stmt.executeQuery(sql);
            Clause cl;
            while (rs1.next()) { // one record only
                System.out.println(new String(rs1.getString(1)));                
            }
            rs1.close();
        } catch (Exception ex) {
            // handle any errors
            ex.printStackTrace();
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
        }
        closeAll();
    }
    public static void main(String[] args) {
        DataProvider data = new DataProvider();
        data.testUnicode();
        
    }
}
