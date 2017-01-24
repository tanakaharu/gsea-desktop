/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XComboChart;
import edu.mit.broad.genome.reports.RichDataframe;
import edu.mit.broad.genome.reports.api.PicFile;

import org.apache.ecs.Document;
import org.apache.ecs.html.*;
import org.apache.log4j.Logger;
import org.genepattern.heatmap.image.HeatMap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * @note IMP IMP IMP: Wrap all ecs struff so that client code doesnt need to know
 * what implementation of html library we use.
 * Also, this is a decorator -- hide complexity of ECS and only expose API that is useful for
 * report generation in an orderly (though as biased0 manner.
 * <p/>
 * <p/>
 * <p/>
 * ####### NOTES ON ECS AND HTML TAGS ########
 * <p/>
 * CAPTION:
 * The CAPTION tag is used to provide a caption for a TABLE. This caption can either appear above or
 * below the table. This can be indicated with the ALIGN attribute. It is usually centered with respect to the table itself
 * and usually appears in bold or is emphasized in some other way.
 * The tag should appear directly below the TABLE tag, before the first TR.
 * Although you can use all text-level markup inside a CAPTION, it should be brief,
 * so don't include images or large amounts of text in it.
 * <p/>
 * DIV: Logical Division: A generic tag used to format large blocks of text.
 * The DIV tag is used to mark up divisions in a document.
 * It can enclose paragraphs, headers and other block elements.
 * Currently, you can only use it to set the default alignment for all enclosed block elements.
 * Future standards will most likely include more options for DIV
 * <p/>
 * DOCTYPE
 * Use it!!
 * <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN"
 * "http://www.w3.org/TR/REC-html40/strict.dtd">
 * On the other hand, a "loose" or transitional HTML4 document could have:
 * <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
 * <p/>
 * These element, by the way, usually appear on the very first line of a document.
 * <p/>
 * <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
 * @see GREAT best practices guide: http://developer.apple.com/internet/webcontent/bestwebdev.html
 * @see Lst of all HTML elements: http://www.htmlhelp.com/reference/wilbur/quickref.html
 *      <p/>
 *      #### DESIGN ####
 *      <p/>
 *      - every page has a:
 *      1) Title
 *      2) Page Name (shows in browser title bar)
 *      3) Auto linking to the CSS
 *      4) List report folder contents link (simple file explorer / lis of files)
 *      <p/>
 *      - INDEX Pages are special. The are the starting point and are displayed in the report window
 *      Apart from the generic page functionality they have:
 *      1) Auto link to the report parameters file (.rpt)
 *      2) Generated on timestamp
 *      3) footer with a line
 *      4) navigation bar at the very top rather than one on the left.
 *      (Might consider the left one later)
 *      5) Footer with a line break and saying generated by xtools.FOO at time sss
 *      To get more information on xtools see http:...
 * @see http://developer.apple.com/internet/webcontent/bestwebdev.html
 *      <p/>
 *      #### API / FUNCTIONAL REQUIREMENTS ####
 *      <p/>
 *      - auto make a CSS link for every page (can add support for a setCSS API later if needed)
 *      - method to add a RichDataframe easily
 *      - method to add images easily
 *      - maybe methods that add table/images should be a BLOCK method - they auto get
 *      boxed with a caption and a title and placed at tge center with a nice border etc.
 *      (client code shouldnt have to worry)
 *      <p/>
 *      - dont worry too much about formating - realize that anyone making a presentation/public report is
 *      almost certainly gping to need to tweak the output to their goals.
 *      <p/>
 *      #### THINGS TO KEEP IN MIND ####
 *      <p/>
 *      - this class is a container for results. The API should be simple!
 *      - all links etc should be relative
 *      - dont make assumptions on network connection being available al the time
 *      - additionally, the entire contents of the folder should be "zippable" for a complete
 *      download of the report folder.
 *      - try to make no assumptions on what stream is to be written out - if running on a servlte one might
 *      want to write to outputstream rather than a specific file.
 * @see http://www.oreillynet.com/pub/a/network/2000/04/14/doctype/index.html
 *      <p/>
 *      <p/>
 *      #### NOTES ON CSS ####
 *      http://jigsaw.w3.org/css-validator/validator-uri.html
 *      <p/>
 */
public class HtmlPage implements Page {

    /**
     * @todo turn off title in the jgraph plots - instead make that the caption in the html thing
     * ditto for the tables??
     * <p/>
     * split off a links to more inof page: call this LinkTable
     * consider auto-truncating long html tables???
     * <p/>
     * add API for adding a block of bullet points
     * add API for adding a block of text
     */

    private String fName; // short file safe name

    private String fTitle; // longer english name

    private Document fDoc;

    private Logger log = XLogger.getLogger(HtmlPage.class);

    private int fPicCnt = 1;

    /**
     * contains PicFile objects
     */
    private java.util.List<PicFile> fPicFiles;

    /**
     * Class constructor
     *
     * @param name  the unique and short name
     * @param title the full desc/title shows up in the browser title bar
     */
    public HtmlPage(final String name, final String title) {
        this.fTitle = title;
        this.fName = name;
        this.fPicFiles = new ArrayList<PicFile>();

        this.fDoc = new Document();

        HtmlFormat.setCommonDocThings(this.fTitle, this.fDoc);
    }

    public String getName() {
        return fName;
    }

    public String getExt() {
        return Constants.HTML;
    }

    // ------------------------------------------------------------------------- //
    // ---------------------------- THE API METHODS TO ADD CONTENT ------------ //
    // ------------------------------------------------------------------------ //

    /**
     * Very powerful (and dangerous) call.
     * Access to the base HTML page -> can ne used in those small number of cases where
     * the generic API here isnt sufficient. Use with care.
     *
     * @return
     */
    public Document getDoc() {
        return fDoc;
    }

    boolean sectionStarted = false;

    public void addError(final String msg, final Throwable t) {
        log.error(msg, t);
        Div error = HtmlFormat.Divs.error();
        error.addElement(msg);
        error.addElement(new BR());
        error.addElement(TraceUtils.getAsString(t));
        addBlock(error);
    }

    public void addError(final String msg) {
        Div error = HtmlFormat.Divs.error();
        error.addElement(msg);
        addBlock(error);
    }

    public void addBreak() {
        fDoc.appendBody(new BR()); // always add a break after every block
    }

    /**
     * Adds a chart
     *
     * @param xchart
     * @param saveInDir TODO
     * @param createSvgs
     */
    // core addChart method
    public void addChart(final XChart xchart, final int width, final int height, File saveInDir, boolean createSvgs) {
        try {
            PicFile pf = new PicFile(xchart, width, height, fPicCnt++, saveInDir, createSvgs);
            fPicFiles.add(pf);
            addBlock(pf.createIMG());
        } catch (Throwable t) {
            addError("Trouble saving image", t);
        }
    }

    public void addChart(final XComboChart combo, final int width, final int height, File saveInDir, boolean createSvgs) {
        addChart(combo.getCombinedChart(), width, height, saveInDir, createSvgs);
    }

    public void addHeatMap(final String title, final String caption, final HeatMap heatMap, File saveInDir, boolean createSvgs) {
        try {
            PicFile pf = new PicFile(NamingConventions.createSafeFileName(title), title, caption, heatMap, fPicCnt++, saveInDir, createSvgs);
            fPicFiles.add(pf);
            addBlock(pf.createIMG());
        } catch (Throwable t) {
            addError("Trouble saving image", t);
        }
    }
    
    // the core add block/div method
    public void addBlock(final Div div) {
        this.addBlock(div, true); // always add a break after every block
    }

    public void addBlock(final Div div, final boolean addBreakAfter) {
        fDoc.appendBody(div);
        if (addBreakAfter) {
            fDoc.appendBody(new BR());
        }
    }

    /**
     * @param title
     * @param keyValTable
     */
    public void addTable(final String title, final KeyValTable keyValTable) {
        Table table = keyValTable.getTable();
        String stitle = _noNull(title);
        table.addElement(HtmlFormat.Titles.table(stitle));
        Div div = HtmlFormat.Divs.keyValTable();
        div.addElement(table);
        this.addBlock(div);
    }

    public void addTable(final String title, final Table table) {
        Div div = HtmlFormat.Divs.dataTable();
        table.addElement(HtmlFormat.Titles.table(title));
        div.addElement(table);
        this.addBlock(div);
    }

    //-----------------------------------------------------------------//
    // rich rich text
    // row name is NOT the same as row numbering
    public void addTable(final RichDataframe rdf,
                         final String plainTxtFileName,
                         final boolean showRowNameCol,
                         final boolean numberRows) {

        Table table = new Table();
        table.setBorder(1);

        int ncols = rdf.getNumCol();
        final RichDataframe.MetaData metaData = rdf.getMetaData();

        if (numberRows) {
            ncols++; // an additional one for the row names if requested
        }

        if (showRowNameCol) {
            ncols++;
        }

        table.setCols(ncols);

        if (numberRows) {
            table.addElement(HtmlFormat.THs.richTable(""));
        }

        if (showRowNameCol) {
            table.addElement(HtmlFormat.THs.richTable(Constants.NAME));
        }

        // add the col names
        for (int c = 0; c < rdf.getNumCol(); c++) {
            table.addElement(HtmlFormat.THs.richTable(rdf.getColumnName(c)));
        }

        for (int r = 0; r < rdf.getNumRow(); r++) {
            TR tr = new TR(); // start a new row

            if (numberRows) {
                tr.addElement(HtmlFormat.TDs.lessen(r + 1 + "")); // @note hardcoded
            }

            if (showRowNameCol) {
                tr.addElement(HtmlFormat._td(rdf.getRowName(r)));
            }

            for (int c = 0; c < rdf.getNumCol(); c++) {
                Object obj = rdf.getElementObj(r, c);
                //System.out.println("c: " + c + " " + obj);
                TD td;
                if (obj != null) {

                    String s;
                    if (metaData != null) {
                        s = metaData.adjustPrecision(obj, c).toString();
                    } else {
                        s = obj.toString();
                    }

                    td = HtmlFormat._td(s, rdf.getElementColor(r, c), rdf.getElementLink(r, c));

                    // additional formatting
                    if (metaData != null) {
                        String align = metaData.getColumnAlignment(c);
                        if (align != null) {
                            td.setAlign(align);
                        }
                    }

                } else {
                    td = new TD(); // empty to keep the synch up
                }

                tr.addElement(td);
            }

            table.addElement(tr); // close-up the row
        }

        String title_safe = Constants.NA;

        if (rdf.getMetaData() != null) {
            title_safe = _noNull(rdf.getMetaData().getTitle());
        }

        if (plainTxtFileName != null) {
            A a = new A(plainTxtFileName, "[plain text format]");
            title_safe += "&nbsp" + a.toString();
            table.addElement(HtmlFormat.Titles.table(title_safe));
        }

        Div div = HtmlFormat.Divs.richTable();
        div.addElement(table);
        this.addBlock(div);
    }

    private String _noNull(final String s) {
        if (s == null) {
            return Constants.NA;
        } else {
            return s;
        }
    }

    // specified html code added AS IS
    public void addHtml(final String code) {
        fDoc.getHtml().addElement(code);
    }

    public PicFile[] getPicFiles() {
        return fPicFiles.toArray(new PicFile[fPicFiles.size()]);
    }
    
    public void write(final OutputStream os) throws IOException {
        // Write out the HTML
        fDoc.output(os);
        os.close();
    }
} // End HtmlPage