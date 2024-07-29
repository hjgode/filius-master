/*
 ** This file is part of Filius, a network construction and simulation software.
 ** 
 ** Originally created at the University of Siegen, Institute "Didactics of
 ** Informatics and E-Learning" by a students' project group:
 **     members (2006-2007): 
 **         André Asschoff, Johannes Bade, Carsten Dittich, Thomas Gerding,
 **         Nadja Haßler, Ernst Johannes Klebert, Michell Weyer
 **     supervisors:
 **         Stefan Freischlad (maintainer until 2009), Peer Stechert
 ** Project is maintained since 2010 by Christian Eibl <filius@c.fameibl.de>
 **         and Stefan Freischlad
 ** Filius is free software: you can redistribute it and/or modify
 ** it under the terms of the GNU General Public License as published by
 ** the Free Software Foundation, either version 2 of the License, or
 ** (at your option) version 3.
 ** 
 ** Filius is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied
 ** warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 ** PURPOSE. See the GNU General Public License for more details.
 ** 
 ** You should have received a copy of the GNU General Public License
 ** along with Filius.  If not, see <http://www.gnu.org/licenses/>.
 */
package filius.gui;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.regex.Matcher;

import javax.swing.JEditorPane;

import org.htmlparser.lexer.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.ResourceUtil;

@SuppressWarnings("serial")
public class HelpPanel extends ControlPanel implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(HelpPanel.class);

    private JEditorPane epHtml;

    public HelpPanel() {
        super(ControlPanel.VERTICAL, 350);
        reInit();
        minimieren();
    }

    protected void initContents() {
        epHtml = new JEditorPane("text/html;charset=UTF-8", null);
        epHtml.setText(messages.getString("guihilfe_msg2"));
        epHtml.setEditable(false);
        epHtml.setMinimumSize(new Dimension(1, 1));
        scrollPane.getViewport().removeAll();
        scrollPane.getViewport().add(epHtml);
    }

    public void loadContext(int mode) {
        //File file;
        InputStream sreader;    //changed to be able to read from jar   
        switch (mode) {
        case GUIMainMenu.MODUS_AKTION:
            //file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_simulation"));
            sreader = getClass().getClassLoader().getResourceAsStream( "hilfe/" + messages.getString("hilfedatei_simulation")); 
            break;
        case GUIMainMenu.MODUS_ENTWURF:
            //file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_entwurf"));
            sreader = getClass().getClassLoader().getResourceAsStream( "hilfe/" + messages.getString("hilfedatei_entwurf")); 
            break;
        case GUIMainMenu.MODUS_DOKUMENTATION:
        default:
            //file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_documentation"));
            sreader = getClass().getClassLoader().getResourceAsStream( "hilfe/" + messages.getString("hilfedatei_documentation")); 
            break;
        }
        //LOG.debug("Help file: " + file.getAbsolutePath());

        String gfxBaseUrl = "file:" + ResourceUtil.getResourceUrlEncodedPath("hilfe/gfx/");
        boolean runningFromJar=false;
        
        String protocol = (HelpPanel.class.getResource("HelpPanel.class")).toString();
        if(protocol.startsWith("jar")){
            // run in jar
            runningFromJar=true;
            gfxBaseUrl="file:file:/C:/tools/filius-master/filius-master.jar!/";
        } else if(Objects.equals(protocol, "file")) {
            // run in ide
            runningFromJar=false;
        }

        /*
        URL p = getClass().getClassLoader().getResource("hilfe/gfx/draganddrop.gif" );
        // URL: jar:file:/C:/tools/filius-master/filius-master.jar!/hilfe/gfx/draganddrop.gif
        String pUrl = p.toString().replace("jar:","");
        pUrl = pUrl.replace("draganddrop.gif","");
        LOG.info ("URL: " +p.toString());
        */
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL imageURL = cl.getResource("hilfe/gfx/draganddrop.gif");
        LOG.debug("imageURL: " + imageURL.toString()); // jar:file:/C:/tools/filius-master/filius-master.jar!/hilfe/gfx/draganddrop.gif
        String sImageURL = (imageURL.toString()).replace("draganddrop.gif","");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(sreader))) {
            StringBuffer sb = new StringBuffer();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line);
            }
            String newText = sb.toString();
            
            LOG.info(String.format("Running from JAR is %b", runningFromJar));
            String escapedGfxBaseUrl = Matcher.quoteReplacement(gfxBaseUrl);
            if(!runningFromJar){
                newText = newText.replaceAll("hilfe/gfx/", escapedGfxBaseUrl); // "file:hilfe/gfx/" ); Load img relative to JAR file
            }else{
                newText =newText.replace("<base href=\"file:bilder\">", "");    //remove base href
                newText = newText.replaceAll("hilfe/gfx/" , sImageURL); //"file:hilfe/gfx/" );
            }
            // <img src="file:/C:/tools/filius-master/target/classes/hilfe/gfx/draganddrop.gif">
            epHtml.read(new java.io.StringReader(newText), null);
            epHtml.setCaretPosition(0);
            LOG.debug(newText);
        } catch (FileNotFoundException e) {
            LOG.warn("help contents could not be found", e);
        } catch (IOException e) {
            LOG.warn("error while reading help contents", e);
        }
    }
}
