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
package filius.gui.netzwerksicht;

import java.awt.FileDialog;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.event.MouseInputAdapter;

import com.itextpdf.text.DocumentException;

import filius.gui.GUIContainer;
import filius.gui.JMainFrame;
import filius.gui.documentation.ReportGenerator;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.SzenarioVerwaltung;

public class GUIDocumentationSidebar extends GUISidebar implements I18n {

    public static final String TYPE_TEXTFIELD = "textfield";
    public static final String TYPE_RECTANGLE = "rectangle";
    public static final String TYPE_EXPORT = "export";
    public static final String TYPE_REPORT = "report";

    public static final String ADD_TEXT = "gfx/dokumentation/add_text_small.png";
    public static final String ADD_RECTANGLE = "gfx/dokumentation/add_small.png";
    public static final String EXPORT = "gfx/dokumentation/download_small.png";
    public static final String REPORT = "gfx/dokumentation/pdf_small.png";

    private static GUIDocumentationSidebar sidebar;

    public static GUIDocumentationSidebar getGUIDocumentationSidebar() {
        if (sidebar == null) {
            sidebar = new GUIDocumentationSidebar();
        }
        return sidebar;
    }

    @Override
    protected void addItemsToSidebar() {
        ImageIcon icon = new ImageIcon(getClass().getResource("/" + ADD_TEXT));
        JSidebarButton newLabel = new JSidebarButton(messages.getString("docusidebar_msg1"), icon, TYPE_TEXTFIELD);
        buttonList.add(newLabel);
        leistenpanel.add(newLabel);

        icon = new ImageIcon(getClass().getResource("/" + ADD_RECTANGLE));
        newLabel = new JSidebarButton(messages.getString("docusidebar_msg3"), icon, TYPE_RECTANGLE);
        buttonList.add(newLabel);
        leistenpanel.add(newLabel);

        icon = new ImageIcon(getClass().getResource("/" + EXPORT));
        newLabel = new JSidebarButton(messages.getString("docusidebar_msg5"), icon, TYPE_EXPORT);
        newLabel.setToolTipText(messages.getString("docusidebar_msg6"));
        newLabel.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                GUIContainer.getGUIContainer().exportAsImage();
            }
        });
        buttonList.add(newLabel);
        leistenpanel.add(newLabel);

        icon = new ImageIcon(getClass().getResource("/" + REPORT));
        newLabel = new JSidebarButton(messages.getString("docusidebar_msg7"), icon, TYPE_REPORT);
        newLabel.setToolTipText(messages.getString("docusidebar_msg8"));
        newLabel.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                GUIDocumentationSidebar.this.generateReport();
            }
        });
        buttonList.add(newLabel);
        leistenpanel.add(newLabel);
    }

    private void generateReport() {
        FileDialog fileDialog = new FileDialog(JMainFrame.getJMainFrame(), messages.getString("main_msg4"),
                FileDialog.SAVE);
        FilenameFilter pdfFilenameFilter = (dir, name) -> name.endsWith(".pdf") || name.endsWith(".PDF");
        fileDialog.setFilenameFilter(pdfFilenameFilter);

        String path = SzenarioVerwaltung.getInstance().holePfad();
        if (path != null) {
            String szenarioFile = new File(path).getAbsolutePath();
            File preselectedFile = new File(szenarioFile.substring(0, szenarioFile.lastIndexOf(".")) + ".pdf");
            fileDialog.setDirectory(preselectedFile.getParent());
            fileDialog.setFile(preselectedFile.getName());
        }

        fileDialog.setVisible(true);
        if (null != fileDialog.getFile()) {
            String reportFilename;
            boolean nameChanged = false;
            if (fileDialog.getFile().endsWith(".pdf") || fileDialog.getFile().endsWith(".PDF")) {
                reportFilename = fileDialog.getFile();
            } else {
                nameChanged = true;
                reportFilename = fileDialog.getFile() + ".pdf";
            }

            int entscheidung = JOptionPane.YES_OPTION;
            Path reportFilepath = Path.of(fileDialog.getDirectory(), reportFilename);
            if (nameChanged && reportFilepath.toFile().exists()) {
                entscheidung = JOptionPane.showConfirmDialog(JMainFrame.getJMainFrame(),
                        messages.getString("guimainmemu_msg17"), messages.getString("guimainmemu_msg10"),
                        JOptionPane.YES_NO_OPTION);
            }
            if (entscheidung == JOptionPane.YES_OPTION) {
                try {
                    ReportGenerator.getInstance().generateReport(reportFilepath.toString());
                } catch (DocumentException | IOException e) {
                    JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg11"));
                }
            }
        }
    }
}
