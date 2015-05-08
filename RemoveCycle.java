/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package obodata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.writer.OBOFormatWriter;

/**
 *
 * @author bqchinh
 */
public class RemoveCycle {
    
    public static String fileout = "E:/Semcare.obo";

    Map<String, Set<String>> docMap = new HashMap<>();
    OBODoc doc = null;
    int counter=0;
    /**
     * Paring OBO file
     * @param fn: path 
     * @return : OBODoc
     */
    public OBODoc loadOBOfile(String fn)  {
        try {
        OBOFormatParser p = new OBOFormatParser();
        OBODoc obodoc = p.parse(new BufferedReader(new FileReader(fn)));
            System.out.println("Loading OBO file... done!");
        return obodoc;
        }catch (Exception ex) {
            System.out.println("--------> error while loading OBO file:\t"+fn);
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Loading map of frames : frame ID -> frame
     * @param doc 
     */
    public void loadMap(OBODoc doc) {
        System.out.println("Time:\t" + System.currentTimeMillis());
        Collection<Frame> flist = doc.getTermFrames();
        for (Frame f : flist) {// loop over list of frame
            String id = f.getId();
            if (id.startsWith("C")&& id.length()==8) { // concept
                // get list of is-a concepts
                Set<String> set = new HashSet<>();
                docMap.put(id, set);
                Collection<Clause> cl = f.getClauses(OBOFormatConstants.OboFormatTag.TAG_IS_A);
                if (cl != null && !cl.isEmpty()) {
                    for (Clause clause : cl) {
                        String cui = (String) clause.getValue();
                        set.add(cui);
                    }

                }
            }
        }
        System.out.println("Loading CUI list  done");
        System.out.println("Time:\t" + System.currentTimeMillis());
    }

    /**
     * Remove is_a relationship to concept that does not exist (due to cyclic removal)
     * @param doc 
     */
    private void removeNullParent(OBODoc doc) {
        int vv = 0;
        for (String cui : docMap.keySet()) {
            Set<String> isaSet = docMap.get(cui);
            if(isaSet!=null){
                Set<String> rm_list = new HashSet<>();
                for(String id:isaSet){
                   if(doc.getTermFrame(id)==null){
                       rm_list.add(id);
                   }
                }
                if(!rm_list.isEmpty()){
                    Frame f = doc.getTermFrame(cui); // get frame with given CUI
                        // remove parent with s (CUI)
                    for(String s: rm_list){
                        removeClause(s, f);
                        vv++;
                    }
                }
            }
            
        }
        System.out.println("Number of null parents removed:\t"+vv);
    }
    /**
     * 
     * @param path 
     */
    public void checkEmptyConcept(String path) {
        doc = loadOBOfile(path);
        if (doc == null) {
            System.out.println("---> Null Doc, please check the path");
            System.exit(1);
        } else {
            loadMap(doc);
            removeNullParent(doc);
        }
        try {
         
            OBOFormatWriter writer = new OBOFormatWriter();
            writer.write(doc, path+"remove");
        }catch (Exception ex) {
            System.out.println("error while writing OBO file");
            ex.printStackTrace();
        }
    }
    
    public void checkCycle(String path){
        doc = loadOBOfile(path);
        if(doc==null){
            System.out.println("---> Null Doc, please check the path");
            System.exit(1);
        }else {
            loadMap(doc);
            int vv=0;
            for(String cui: docMap.keySet()){
                check(cui);
                vv++;
                if(vv>0 && vv % 100000==0){
                    System.out.println(vv+ "has been checked");
                }
            }
            removeNullParent(doc);
            removeFrame(doc);
        }
        System.out.println("Number of cycle removed:\t"+counter);
        System.out.println("Number of clause actually removed:\t"+rmCount);
        try {
            OBOFormatWriter writer = new OBOFormatWriter();
            writer.write(doc, path+"_rm");
            
        }catch (Exception ex) {
            System.out.println("error while writing OBO file");
            ex.printStackTrace();
        }
    }
    /**
     * Remove Frames that do not have semantic type 
     * @param doc 
     */
    private void removeFrame(OBODoc doc) {
        Set<Frame> rm_set = new HashSet<>();
        Collection<Frame> flist = doc.getTermFrames();
        System.out.println("Number of concepts:\t"+flist.size());
        for (Frame f : flist) {// loop over list of frame
            String id = f.getId();
            if (id.startsWith("C")&&id.length()==8) { // concept
                // get list of is-a concepts
                Collection<Clause> cl = f.getClauses(OBOFormatConstants.OboFormatTag.TAG_RELATIONSHIP);
                if (cl == null || cl.isEmpty()) {
                    rm_set.add(f);
                }
            }
        }
        System.out.println("Number of removed concepts:\t"+rm_set.size());
        Collection<Frame> ls = doc.getTermFrames();
        System.out.println("Number of concepts before removing:\t"+ls.size());
        for(Frame f: rm_set){
            ls.remove(f);
        }
        System.out.println("Number of concepts after removing:\t"+ls.size());
        // Write back to file
    }
    
    public void check(String CUI) {
        Set<String> set = docMap.get(CUI);
        Set<String> set1 = new HashSet<>();
        set1.add(CUI);
        List<String> rmList = new ArrayList<>();
        for (String s : set) { // loop over list of parent (CUIs)
            if (check(s, set1)) { // this parent need to be removed
                Frame f = doc.getTermFrame(CUI); // get frame with given CUI
                // remove parent with s (CUI)
                removeClause(s, f);
                rmList.add(s);
                counter++;
            }
        }
        for(String s: rmList){
            set.remove(s);
        }
    }

    public boolean check(String CUI, Set<String> checker) {
        Set<String> set = docMap.get(CUI);
        boolean v = false;
        if (set==null || set.isEmpty()) {
            return false;
        } else {
            // check parent of this CUI
            for (String s : set) {
                if (checker.contains(s)) {
                    return true;
                }
            }
            // now check grand parents of this CUI
            // make new Set, adding this CUI into parent list
            Set<String> set1 = new HashSet<>(checker);
            set1.add(CUI);
            for (String s : set) {
                if( check(s, set1)){
                    return true;
                }
            }

        }
        return v;
    }

    
    int rmCount=0;
    private void removeClause(String cui, Frame f) {
        Collection<Clause> cl = f.getClauses(OBOFormatConstants.OboFormatTag.TAG_IS_A);
        Clause rm = null;
        if (cl != null && !cl.isEmpty()) {
            for (Clause c : cl) {
                if (((String) c.getValue()).equals(cui)) {
                    rm = c;
                    break;
                }
            }
            if (rm != null) {
                Collection<Clause> clauses = f.getClauses();
                boolean remove =clauses.remove(rm);
                if(remove){
                    rmCount++;
                }
            }
        }
    }
    
    
    
    public static void main(String[] args) {
        RemoveCycle removeCycle = new RemoveCycle();
        removeCycle.checkCycle(fileout);
    }
}
