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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import filius.gui.anwendungssicht.GUIDesktopWindow;
import filius.gui.anwendungssicht.SimulationControl;
import filius.gui.nachrichtensicht.AggregatedExchangeDialog;
import filius.gui.nachrichtensicht.ExchangeComponent;
import filius.gui.nachrichtensicht.LayeredExchangeDialog;
import filius.gui.netzwerksicht.GUIDesignSidebar;
import filius.gui.netzwerksicht.GUIDocuItem;
import filius.gui.netzwerksicht.GUIDocumentationPanel;
import filius.gui.netzwerksicht.GUIDocumentationSidebar;
import filius.gui.netzwerksicht.GUIKabelItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.gui.netzwerksicht.GUINetworkPanel;
import filius.gui.netzwerksicht.GUIPrintPanel;
import filius.gui.netzwerksicht.JCablePanel;
import filius.gui.netzwerksicht.JDocuElement;
import filius.gui.netzwerksicht.JKonfiguration;
import filius.gui.netzwerksicht.JSidebarButton;
import filius.hardware.Hardware;
import filius.hardware.Kabel;
import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.Host;
import filius.hardware.knoten.InternetKnoten;
import filius.hardware.knoten.Knoten;
import filius.hardware.knoten.Modem;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.hardware.knoten.Switch;
import filius.hardware.knoten.Vermittlungsrechner;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.SzenarioVerwaltung;
import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.system.Betriebssystem;
import filius.software.system.SystemSoftware;

public class GUIContainer implements Serializable, I18n {
    private static Logger LOG = LoggerFactory.getLogger(GUIContainer.class);

    private enum FileType {
        PNG, SVG
    }

    private static final int MIN_DESKTOP_SPACING = 10;
    private static final Integer ACTIVE_LISTENER_LAYER = Integer.valueOf(1);
    private static final Integer INACTIVE_LISTENER_LAYER = Integer.valueOf(-2);
    private static final Integer BACKGROUND_LAYER = Integer.valueOf(-10);
    public static final int NONE = 0;
    public static final int MOVE = 1;
    public static final int LEFT_SIZING = 2;
    public static final int RIGHT_SIZING = 4;
    public static final int UPPER_SIZING = 8;
    public static final int LOWER_SIZING = 16;
    private static final long serialVersionUID = 1L;

    public static final int FLAECHE_BREITE = 2000;
    public static final int FLAECHE_HOEHE = 1500;

    private static GUIContainer ref;

    private GUIMainMenu menu;

    private int width = FLAECHE_BREITE;
    private int height = FLAECHE_HOEHE;

    private GUINetworkPanel networkPanel;
    private JPanel designListenerPanel = new JPanel();
    private JLayeredPane layeredPane = new JLayeredPane();

    private GUIDocumentationSidebar docuSidebar;
    private GUIDocumentationPanel docuPanel;
    private JScrollPane docuSidebarScrollpane;
    private JPanel docuDragPanel = new JPanel();
    private JDocuElement activeDocuElement;

    private GUIDesignSidebar designSidebar;
    private JBackgroundPanel designBackgroundPanel = new JBackgroundPanel();
    private ControlPanel designItemConfig = JKonfiguration.getInstance(null);
    private HelpPanel helpPanel = new HelpPanel();
    private JScrollPane designView;
    private JScrollPane designSidebarScrollpane;
    private JSidebarButton designDragPreview, designCablePreview;
    private JSidebarButton ziel2Label;
    private JCablePanel kabelPanelVorschau;
    private JMarkerPanel designSelection;
    private JMarkerPanel designSelectionArea;

    private JBackgroundPanel simulationBackgroundPanel = new JBackgroundPanel();
    private JScrollPane simulationView;
    private List<GUIDesktopWindow> desktopWindowList = new LinkedList<GUIDesktopWindow>();
    private boolean lastVisibilityStateLauscher;

    /** enthält einen Integerwert dafür welche Ansicht gerade aktiv ist */
    private int activeSite = GUIMainMenu.MODUS_ENTWURF;

    private List<GUIKnotenItem> nodeItems = new LinkedList<GUIKnotenItem>();
    private List<GUIKabelItem> cableItems = new LinkedList<GUIKabelItem>();
    private List<GUIDocuItem> docuItems = new ArrayList<GUIDocuItem>();
    private Set<SystemSoftware> visibleSystems = new HashSet<>();
    private Hardware lastOpenDesignComponent;
    private boolean lastSimulationControlMaximized;

    private GUIContainer(int width, int height) {
        this.width = width;
        this.height = height;

        docuPanel = new GUIDocumentationPanel(width, height);
        docuPanel.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                if (activeSite == GUIMainMenu.MODUS_DOKUMENTATION) {
                    GUIEvents.getGUIEvents().mausPressedDocuMode(e);
                }
            }
        });

        networkPanel = new GUINetworkPanel(width, height);
        networkPanel.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                if (activeSite == GUIMainMenu.MODUS_AKTION) {
                    GUIEvents.getGUIEvents().mausPressedActionMode(e);
                }
            }
        });
    }

    public List<GUIKnotenItem> getKnotenItems() {
        return nodeItems;
    }

    public void removeNodeItem(GUIKnotenItem item) {
        nodeItems.remove(item);
        Knoten node = item.getKnoten();
        if (node instanceof InternetKnoten) {
            for (NetzwerkInterface nic : ((InternetKnoten) node).getNetzwerkInterfaces()) {
                Lauscher.getLauscher().removeIdentifier(nic.getMac());
            }
        }
    }

    public JSidebarButton getZiel2Label() {
        return ziel2Label;
    }

    public void setZiel2Label(JSidebarButton ziel2Label) {
        this.ziel2Label = ziel2Label;
    }

    public void nachrichtenDialogAnzeigen() {
        ((JDialog) getExchangeDialog()).setVisible(true);
    }

    public ExchangeComponent getExchangeDialog() {
        ExchangeComponent exchangeDialog;
        if (Information.getInformation().isOldExchangeDialog()) {
            exchangeDialog = LayeredExchangeDialog.getInstance(JMainFrame.getJMainFrame());
        } else {
            exchangeDialog = AggregatedExchangeDialog.getInstance(JMainFrame.getJMainFrame());
        }
        return exchangeDialog;
    }

    /**
     * Die Prozedur wird erst aufgerufen wenn der Container c zugewiesen wurde. Sie Initialisiert die einzelnen Panels
     * und weist die ActionListener zu.
     * 
     * @author Johannes Bade & Thomas Gerding
     */
    private void initialisieren() {
        layeredPane.setSize(width, height);
        layeredPane.setMinimumSize(new Dimension(width, height));
        layeredPane.setPreferredSize(new Dimension(width, height));

        designListenerPanel.setSize(width, height);
        designListenerPanel.setMinimumSize(new Dimension(width, height));
        designListenerPanel.setPreferredSize(new Dimension(width, height));
        designListenerPanel.setOpaque(false);
        layeredPane.add(designListenerPanel, ACTIVE_LISTENER_LAYER);

        /*
         * auswahl: area covered during mouse pressed, i.e., area with components to be selected
         */
        designSelection = new JMarkerPanel();
        designSelection.setBounds(0, 0, 0, 0);
        designSelection.setBackgroundImage("gfx/allgemein/auswahl.png");
        designSelection.setOpaque(false);
        designSelection.setVisible(true);
        layeredPane.add(designSelection, JLayeredPane.DRAG_LAYER);

        /*
         * Kabelvorschau wird erstellt und dem Container hinzugefuegt. Wird Anfangs auf Invisible gestellt, und nur bei
         * Verwendung sichtbar gemacht.
         */
        designCablePreview = new JSidebarButton("", new ImageIcon(getClass().getResource("/gfx/allgemein/ziel1.png")),
                null);
        designCablePreview.setVisible(false);
        layeredPane.add(designCablePreview, JLayeredPane.DRAG_LAYER);

        /*
         * Die Vorschau für das drag&drop, die das aktuelle Element anzeigt wird initialisiert und dem layeredpane
         * hinzugefügt. Die Visibility wird jedoch auf false gestellt, da ja Anfangs kein drag&drop vorliegt.
         */
        designDragPreview = new JSidebarButton();
        designDragPreview.setVisible(false);
        layeredPane.add(designDragPreview, JLayeredPane.DRAG_LAYER);

        /*
         * Hauptmenü wird erstellt und dem Container hinzugefügt
         */
        menu = new GUIMainMenu();
        JMainFrame.getJMainFrame().addToContentPane(menu.getMenupanel(), BorderLayout.NORTH);

        JMainFrame.getJMainFrame().addToContentPane(helpPanel, BorderLayout.EAST);

        setProperty(null);

        /* sidebar wird erstellt und anschliessend dem Container c zugefüt */
        designSidebar = GUIDesignSidebar.getGUIDesignSidebar();
        docuSidebar = GUIDocumentationSidebar.getGUIDocumentationSidebar();

        /* markierung: actual area covering selected objects */
        designSelectionArea = new JMarkerPanel();
        designSelectionArea.setBounds(0, 0, 0, 0);
        designSelectionArea.setBackgroundImage("gfx/allgemein/markierung.png");
        designSelectionArea.setOpaque(false);
        designSelectionArea.setVisible(false);
        designSelectionArea.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        layeredPane.add(designSelectionArea, JLayeredPane.DRAG_LAYER);

        /* scrollpane für das Linke Panel (sidebar) */
        designSidebarScrollpane = new JScrollPane(designSidebar.getLeistenpanel());
        designSidebarScrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (Information.isLowResolution()) {
            designSidebarScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            designSidebarScrollpane.getVerticalScrollBar().setUnitIncrement(10);
        } else {
            designSidebarScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }

        docuSidebarScrollpane = new JScrollPane(docuSidebar.getLeistenpanel());
        docuSidebarScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        /* scrollpane für das Mittlere Panel */
        layeredPane.add(networkPanel, JLayeredPane.DEFAULT_LAYER);

        designBackgroundPanel.setBackgroundImage("gfx/allgemein/entwurfshg.png");
        designBackgroundPanel.setBounds(0, 0, width, height);

        docuDragPanel.setBounds(0, 0, width, height);
        docuDragPanel.setOpaque(false);
        layeredPane.add(docuDragPanel, JLayeredPane.DRAG_LAYER);

        layeredPane.add(docuPanel, INACTIVE_LISTENER_LAYER);

        designView = new JScrollPane(layeredPane);
        designView.getVerticalScrollBar().setUnitIncrement(10);

        simulationBackgroundPanel.setBackgroundImage("gfx/allgemein/simulationshg.png");
        simulationBackgroundPanel.setBounds(0, 0, width, height);
        simulationView = new JScrollPane();
        simulationView.getVerticalScrollBar().setUnitIncrement(10);

        /*
         * Wird auf ein Item der Sidebar geklickt, so wird ein neues Vorschau-Label mit dem entsprechenden Icon
         * erstellt.
         * 
         * Wird die Maus auf dem Entwurfspanel losgelassen, während ein Item gedragged wird, so wird eine neue
         * Komponente erstellt.
         */
        designSidebarScrollpane.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {

                JSidebarButton button = designSidebar.findButtonAt(e.getX(),
                        e.getY() + designSidebarScrollpane.getVerticalScrollBar().getValue());
                if (button != null) {
                    neueVorschau(button.getTyp(),
                            e.getX() + GUIContainer.getGUIContainer().getXOffset() - designSidebarScrollpane.getWidth(),
                            e.getY() + GUIContainer.getGUIContainer().getYOffset());
                    // The exact location depends on the size of the designDragPreview which is determined
                    // only in method neueVorschau. Hence the call below.
                    designDragPreview.setLocation(designDragPreview.getX() - designDragPreview.getWidth() / 2,
                            designDragPreview.getY() - designDragPreview.getHeight() / 2);
                    GUIEvents.getGUIEvents().resetAndHideCablePreview();
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (designDragPreview.isVisible() && e.getX() >= designSidebarScrollpane.getWidth()
                        && e.getX() <= designView.getWidth() && e.getY() >= 0 && e.getY() <= designView.getHeight()) {
                    int xPosMainArea = e.getX() - designDragPreview.getWidth() / 2 - designSidebarScrollpane.getWidth();
                    int yPosMainArea = e.getY() - (designDragPreview.getHeight()) / 2;
                    neuerKnoten(xPosMainArea, yPosMainArea, designDragPreview);
                }
                designDragPreview.setVisible(false);
            }
        });

        /*
         * Sofern die Drag & Drop Vorschau sichtbar ist, wird beim draggen der Maus die entsprechende Vorschau auf die
         * Mausposition verschoben.
         */
        designSidebarScrollpane.addMouseMotionListener(new MouseInputAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (designDragPreview.isVisible()) {
                    designDragPreview.setLocation(
                            e.getX() + GUIContainer.getGUIContainer().getXOffset() - designDragPreview.getWidth() / 2
                                    - designSidebarScrollpane.getWidth(),
                            e.getY() + GUIContainer.getGUIContainer().getYOffset() - designDragPreview.getHeight() / 2);
                }
            }
        });

        docuSidebarScrollpane.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                JSidebarButton button = docuSidebar.findButtonAt(e.getX(), e.getY());
                if (button != null) {
                    if (GUIDocumentationSidebar.TYPE_RECTANGLE.equals(button.getTyp())) {
                        activeDocuElement = new JDocuElement(false, true);
                    } else if (GUIDocumentationSidebar.TYPE_TEXTFIELD.equals(button.getTyp())) {
                        activeDocuElement = new JDocuElement(true, true);
                    }
                    activeDocuElement.setSelected(true);
                    activeDocuElement.setLocation(
                            e.getX() - designSidebarScrollpane.getWidth() + GUIContainer.getGUIContainer().getXOffset(),
                            e.getY() + GUIContainer.getGUIContainer().getYOffset());
                    docuDragPanel.add(activeDocuElement);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.getX() > docuSidebarScrollpane.getWidth() && activeDocuElement != null) {
                    docuItems.add(GUIDocuItem.createDocuItem(activeDocuElement));
                    SzenarioVerwaltung.getInstance().setzeGeaendert();
                    activeDocuElement.setSelected(false);
                    docuDragPanel.remove(activeDocuElement);
                    docuPanel.add(activeDocuElement);
                    activeDocuElement.requestFocusInWindow();
                    GUIContainer.this.updateViewport();
                } else if (activeDocuElement != null) {
                    docuDragPanel.remove(activeDocuElement);
                    docuDragPanel.updateUI();
                }
                activeDocuElement = null;
            }
        });

        docuSidebarScrollpane.addMouseMotionListener(new MouseInputAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (activeDocuElement != null) {
                    activeDocuElement.setLocation(
                            e.getX() + GUIContainer.getGUIContainer().getXOffset() - activeDocuElement.getWidth() / 2
                                    - designSidebarScrollpane.getWidth(),
                            e.getY() + GUIContainer.getGUIContainer().getYOffset() - activeDocuElement.getHeight() / 2);
                }
            }
        });

        /*
         * Erzeugen und transformieren des Auswahlrahmens, und der sich darin befindenden Objekte.
         */
        designListenerPanel.addMouseMotionListener(new MouseInputAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
                    GUIEvents.getGUIEvents().mausDragged(e);
                }
            }
        });

        designListenerPanel.addMouseListener(new MouseInputAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
                    GUIEvents.getGUIEvents().mausReleased();
                }
            }

            public void mousePressed(MouseEvent e) {
                if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
                    GUIEvents.getGUIEvents().mausPressedDesignMode(e);
                }
            }
        });

        designListenerPanel.addMouseMotionListener(new MouseInputAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (designCablePreview.isVisible()) {
                    designCablePreview.setBounds(e.getX(), e.getY(), designCablePreview.getWidth(),
                            designCablePreview.getHeight());
                    if (ziel2Label != null) {
                        ziel2Label.setLocation(e.getX(), e.getY());
                    }
                    if (kabelPanelVorschau != null) {
                        kabelPanelVorschau.updateBounds();
                    }
                    LOG.trace(e.getX() + " / " + e.getY());
                    GUIEvents.getGUIEvents().updateGatewayPort(e.getX(), e.getY());
                }
            }
        });

        JMainFrame.getJMainFrame().setVisible(true);
        menu.selectMode(GUIMainMenu.MODUS_ENTWURF);
    }

    public void exportAsImage() {
        FileDialog fileDialog = new FileDialog(JMainFrame.getJMainFrame(), messages.getString("main_msg4"),
                FileDialog.SAVE);
        FilenameFilter filiusFilenameFilter = (dir, name) -> name.endsWith(".png") || name.endsWith(".PNG");
        fileDialog.setFilenameFilter(filiusFilenameFilter);

        String path = SzenarioVerwaltung.getInstance().holePfad();
        if (path != null) {
            Path scenarioPath = Path.of(path);
            fileDialog.setDirectory(scenarioPath.getParent().toString());

            String scenarioFilename = scenarioPath.getFileName().toString();
            fileDialog.setFile(scenarioFilename.substring(0, scenarioFilename.lastIndexOf(".")) + ".png");
        }

        fileDialog.setVisible(true);
        if (null != fileDialog.getFile()) {
            String imagePath;
            boolean nameChanged = false;
            if (fileDialog.getFile().endsWith(".png") || fileDialog.getFile().endsWith(".PNG")) {
                imagePath = Path.of(fileDialog.getDirectory(), fileDialog.getFile()).toString();
            } else {
                nameChanged = true;
                imagePath = Path.of(fileDialog.getDirectory(), fileDialog.getFile()).toString() + ".png";
            }

            int entscheidung = JOptionPane.YES_OPTION;
            if (nameChanged && new File(imagePath).exists()) {
                entscheidung = JOptionPane.showConfirmDialog(JMainFrame.getJMainFrame(),
                        messages.getString("guimainmemu_msg17"), messages.getString("guimainmemu_msg10"),
                        JOptionPane.YES_NO_OPTION);
            }
            if (entscheidung == JOptionPane.YES_OPTION) {
                try {
                    exportAsPNG(imagePath);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg11"));
                }
            }
            LOG.debug("export to file: " + imagePath);
            updateViewport();
        }
    }

    private void exportAsPNG(String imagePath) throws IOException {
        BufferedImage printArea = createNetworkImage();
        try (ImageOutputStream outputStream = new FileImageOutputStream(new File(imagePath))) {
            ImageIO.write(printArea, "png", outputStream);
        }
    }

    public BufferedImage createNetworkImage() {
        GUIPrintPanel printPanel = prepareExportPanel();
        BufferedImage bi = new BufferedImage(printPanel.getWidth(), printPanel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        printPanel.paint(g);
        g.dispose();
        BufferedImage printArea = bi.getSubimage(printPanel.getClipX(), printPanel.getClipY(),
                printPanel.getClipWidth(), printPanel.getClipHeight());
        return printArea;
    }

    private void exportAsSVG(String imagePath) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        // Get a DOMImplementation.
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
        ctx.setComment("Generated by Filius");
        ctx.setEmbeddedFontsOn(true);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx, false);

        GUIPrintPanel printPanel = prepareExportPanel();
        printPanel.paint(svgGenerator);
        svgGenerator.dispose();

        try (Writer out = new OutputStreamWriter(new FileOutputStream(new File(imagePath)), "UTF-8");) {
            svgGenerator.stream(out, true);
        }
    }

    private GUIPrintPanel prepareExportPanel() {
        GUIPrintPanel printPanel = new GUIPrintPanel(width, height, SzenarioVerwaltung.getInstance().holePfad());
        printPanel.updateViewport(nodeItems, cableItems, docuItems);
        return printPanel;
    }

    /**
     * Erstellt ein neues Item. Der Dateiname des Icons wird über den String "komponente" angegeben, die Position über x
     * und y. Das Item wird anschließend dem Entwurfspanel hinzugefügt, und das Entwurfspanel wird aktualisiert.
     * 
     * @author Johannes Bade & Thomas Gerding
     * 
     * @param komponente
     * @param x
     * @param y
     * @return boolean
     */
    private boolean neuerKnoten(int x, int y, JSidebarButton label) {
        Knoten neuerKnoten = null;
        GUIKnotenItem item;
        JSidebarButton templabel;
        ImageIcon tempIcon = null;

        SzenarioVerwaltung.getInstance().setzeGeaendert();

        ListIterator<GUIKnotenItem> it = nodeItems.listIterator();
        while (it.hasNext()) {
            item = (GUIKnotenItem) it.next();
            item.getImageLabel().setSelektiert(false);
        }

        // Auswahlrahmen löschen
        GUIEvents.getGUIEvents().cancelMultipleSelection();

        if (label.getTyp().equals(Switch.TYPE)) {
            neuerKnoten = new Switch();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.SWITCH));
        } else if (label.getTyp().equals(Rechner.TYPE)) {
            neuerKnoten = new Rechner();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.RECHNER));
        } else if (label.getTyp().equals(Notebook.TYPE)) {
            neuerKnoten = new Notebook();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.NOTEBOOK));
        } else if (label.getTyp().equals(Vermittlungsrechner.TYPE)) {
            neuerKnoten = new Vermittlungsrechner();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.VERMITTLUNGSRECHNER));

            Object[] possibleValues = { "2", "3", "4", "5", "6", "7", "8" };
            Object selectedValue = JOptionPane.showInputDialog(JMainFrame.getJMainFrame(),
                    messages.getString("guicontainer_msg1"), messages.getString("guicontainer_msg2"),
                    JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
            if (selectedValue != null) {
                ((Vermittlungsrechner) neuerKnoten).setzeAnzahlAnschluesse(Integer.parseInt((String) selectedValue));
            } else {
                ((Vermittlungsrechner) neuerKnoten).setzeAnzahlAnschluesse(2);
            }
        } else if (label.getTyp().equals(Modem.TYPE)) {
            neuerKnoten = new Modem();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.MODEM));
        } else if (label.getTyp().equals(Gateway.TYPE)) {
            neuerKnoten = new Gateway();
            tempIcon = new ImageIcon(getClass().getResource("/" + GUIDesignSidebar.GATEWAY));
        } else {
            LOG.debug("ERROR (" + this.hashCode() + "): " + "unbekannter Hardwaretyp " + label.getTyp()
                    + " konnte nicht erzeugt werden.");
        }

        if (tempIcon != null && neuerKnoten != null) {
            templabel = new JSidebarButton(tempIcon, neuerKnoten.holeHardwareTyp());
            templabel.setBounds((x >= 0 ? x : 0) + designView.getHorizontalScrollBar().getValue(),
                    (y >= 0 ? y : 0) + designView.getVerticalScrollBar().getValue(), templabel.getWidth(),
                    templabel.getHeight());
            // The text must be assigned after construction.
            // This is required for the icon not to move when the text is longer than the width of the icon
            templabel.initTextAndUpdateLocation(neuerKnoten.holeAnzeigeName());

            item = new GUIKnotenItem();
            item.setKnoten(neuerKnoten);
            item.setImageLabel(templabel);

            setProperty(neuerKnoten);
            item.getImageLabel().setSelektiert(true);
            nodeItems.add(item);

            networkPanel.add(templabel);
            networkPanel.repaint();

            GUIEvents.getGUIEvents().setNewItemActive(item);

            return true;
        } else {
            return false;
        }

    }

    /**
     * 
     * Entwurfsmuster: Singleton
     * 
     * Da die GUI nur einmal erstellt werden darf, und aus verschiedenen Klassen auf sie zugegriffen wird, ist diese als
     * Singleton realisiert.
     * 
     * getGUIContainer() ruft den private Konstruktor auf, falls dies noch nicht geschehen ist, ansonsten wird die
     * Referenz auf die Klasse zurückgegeben.
     * 
     * @author Johannes Bade & Thomas Gerding
     * 
     * @return ref
     */
    public static GUIContainer getGUIContainer() {
        return getGUIContainer(FLAECHE_BREITE, FLAECHE_HOEHE);
    }

    public static GUIContainer getGUIContainer(int width, int height) {
        if (ref == null) {
            ref = new GUIContainer(width, height);
            if (ref == null) {
                LOG.debug("ERROR (static) getGUIContainer(): Fehler!!! ref==null");
            }
            ref.initialisieren();
        }
        return ref;
    }

    private void neueVorschau(String hardwareTyp, int x, int y) {
        String tmp;

        LOG.debug("GUIContainer: die Komponenten-Vorschau wird erstellt.");
        if (hardwareTyp.equals(Kabel.TYPE)) {
            tmp = GUIDesignSidebar.KABEL;
        } else if (hardwareTyp.equals(Switch.TYPE)) {
            tmp = GUIDesignSidebar.SWITCH;
        } else if (hardwareTyp.equals(Rechner.TYPE)) {
            tmp = GUIDesignSidebar.RECHNER;
        } else if (hardwareTyp.equals(Notebook.TYPE)) {
            tmp = GUIDesignSidebar.NOTEBOOK;
        } else if (hardwareTyp.equals(Vermittlungsrechner.TYPE)) {
            tmp = GUIDesignSidebar.VERMITTLUNGSRECHNER;
        } else if (hardwareTyp.equals(Modem.TYPE)) {
            tmp = GUIDesignSidebar.MODEM;
        } else if (hardwareTyp.equals(Gateway.TYPE)) {
            tmp = GUIDesignSidebar.GATEWAY;
        } else {
            tmp = null;
            LOG.debug("GUIContainer: ausgewaehlte Hardware-Komponente unbekannt!");
        }

        designDragPreview.setTyp(hardwareTyp);
        designDragPreview.setIcon(new ImageIcon(getClass().getResource("/" + tmp)));
        designDragPreview.setBounds(x, y, designDragPreview.getWidth(), designDragPreview.getHeight());
        designDragPreview.setVisible(true);
    }

    /**
     * Prüft ob ein Punkt (definiert durch die Parameter x & y) auf einem Objekt (definiert durch den Parameter komp)
     * befindet.
     * 
     * @author Johannes Bade
     */
    public boolean aufObjekt(Component komp, int x, int y) {
        if (x > komp.getX() && x < komp.getX() + komp.getWidth() && y > komp.getY()
                && y < komp.getY() + komp.getHeight()) {
            return true;
        }
        return false;
    }

    /**
     * Löscht alle Elemente der Item- und Kabelliste und frischt den Viewport auf. Dies dient dem Reset vor dem Laden
     * oder beim Erstellen eines neuen Projekts.
     * 
     * @author Johannes Bade & Thomas Gerding
     */
    public void clearAllItems() {
        nodeItems.clear();
        cableItems.clear();
        docuItems.clear();
        desktopWindowList.clear();
        Lauscher.getLauscher().reset();
        updateViewport();
    }

    public void updateViewport() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (GUIContainer), updateViewport()");
        networkPanel.updateViewport(nodeItems, cableItems);
        networkPanel.updateUI();
        if (activeSite == GUIMainMenu.MODUS_AKTION) {
            docuPanel.updateViewport(docuItems, false);
        } else {
            docuPanel.updateViewport(docuItems, activeSite == GUIMainMenu.MODUS_DOKUMENTATION);
        }
        docuPanel.updateUI();
    }

    /**
     * 
     * Geht die Liste der Kabel durch und ruft bei diesen updateBounds() auf. So werden die Kabel neu gezeichnet.
     * 
     * @author Thomas Gerding & Johannes Bade
     * 
     */
    public void updateCables() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (GUIContainer), updateCables()");
        ListIterator<GUIKabelItem> it = cableItems.listIterator();
        while (it.hasNext()) {
            GUIKabelItem tempCable = (GUIKabelItem) it.next();
            tempCable.getKabelpanel().updateBounds();
        }
    }

    public JSidebarButton getKabelvorschau() {
        return designCablePreview;
    }

    public int getActiveSite() {
        return activeSite;
    }

    public void setActiveSite(int activeSite) {
        this.activeSite = activeSite;

        for (Component background : layeredPane.getComponentsInLayer(BACKGROUND_LAYER)) {
            layeredPane.remove(background);
        }
        helpPanel.loadContext(activeSite);
        if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
            closeDialogs();
            designView.getVerticalScrollBar().setValue(simulationView.getVerticalScrollBar().getValue());
            designView.getHorizontalScrollBar().setValue(simulationView.getHorizontalScrollBar().getValue());
            JMainFrame.getJMainFrame().removeFromContentPane(this.docuSidebarScrollpane);
            JMainFrame.getJMainFrame().addToContentPane(this.designSidebarScrollpane, BorderLayout.WEST);
            JMainFrame.getJMainFrame().removeFromContentPane(this.simulationView);
            layeredPane.setLayer(docuPanel, INACTIVE_LISTENER_LAYER);
            layeredPane.setLayer(designListenerPanel, ACTIVE_LISTENER_LAYER);
            layeredPane.add(designBackgroundPanel, BACKGROUND_LAYER);
            designView.setViewportView(layeredPane);
            JMainFrame.getJMainFrame().addToContentPane(this.designView, BorderLayout.CENTER);
            JMainFrame.getJMainFrame().addToContentPane(designItemConfig, BorderLayout.SOUTH);
            setProperty(lastOpenDesignComponent);
            docuDragPanel.setVisible(false);
            designSidebarScrollpane.updateUI();
            designView.updateUI();
        } else if (activeSite == GUIMainMenu.MODUS_DOKUMENTATION) {
            closeDialogs();
            designView.getVerticalScrollBar().setValue(simulationView.getVerticalScrollBar().getValue());
            designView.getHorizontalScrollBar().setValue(simulationView.getHorizontalScrollBar().getValue());
            JMainFrame.getJMainFrame().removeFromContentPane(this.designSidebarScrollpane);
            JMainFrame.getJMainFrame().addToContentPane(this.docuSidebarScrollpane, BorderLayout.WEST);
            JMainFrame.getJMainFrame().removeFromContentPane(this.simulationView);
            layeredPane.add(designBackgroundPanel, BACKGROUND_LAYER);
            layeredPane.setLayer(docuPanel, ACTIVE_LISTENER_LAYER);
            layeredPane.setLayer(designListenerPanel, INACTIVE_LISTENER_LAYER);
            designView.setViewportView(layeredPane);
            designSelection.setVisible(false);
            designSelectionArea.setVisible(false);
            docuDragPanel.setVisible(true);
            JMainFrame.getJMainFrame().addToContentPane(this.designView, BorderLayout.CENTER);
            JMainFrame.getJMainFrame().removeFromContentPane(designItemConfig);
            docuSidebarScrollpane.updateUI();
            designView.updateUI();
        } else if (activeSite == GUIMainMenu.MODUS_AKTION) {
            reopenDialogs();
            simulationView.getVerticalScrollBar().setValue(designView.getVerticalScrollBar().getValue());
            simulationView.getHorizontalScrollBar().setValue(designView.getHorizontalScrollBar().getValue());
            layeredPane.setLayer(docuPanel, INACTIVE_LISTENER_LAYER);
            layeredPane.setLayer(designListenerPanel, INACTIVE_LISTENER_LAYER);
            layeredPane.add(simulationBackgroundPanel, BACKGROUND_LAYER);
            simulationView.setViewportView(layeredPane);
            JMainFrame.getJMainFrame().removeFromContentPane(this.docuSidebarScrollpane);
            JMainFrame.getJMainFrame().removeFromContentPane(this.designSidebarScrollpane);
            designSelection.setVisible(false);
            designSelectionArea.setVisible(false);
            docuDragPanel.setVisible(false);
            JMainFrame.getJMainFrame().removeFromContentPane(this.designView);
            JMainFrame.getJMainFrame().addToContentPane(this.simulationView, BorderLayout.CENTER);
            JMainFrame.getJMainFrame().addToContentPane(designItemConfig, BorderLayout.SOUTH);
            setProperty(null);
            simulationView.updateUI();
        }
        GUIEvents.getGUIEvents().resetAndHideCablePreview();
        JMainFrame.getJMainFrame().invalidate();
        JMainFrame.getJMainFrame().validate();
        updateViewport();
    }

    public List<GUIKabelItem> getCableItems() {
        return cableItems;
    }

    public void setCablelist(List<GUIKabelItem> cablelist) {
        this.cableItems = cablelist;
    }

    public int getAbstandLinks() {
        int abstand = 0;
        abstand = GUIContainer.getGUIContainer().getSidebar().getLeistenpanel().getWidth();
        return abstand;
    }

    public int getAbstandOben() {
        int abstand = 0;
        abstand = GUIContainer.getGUIContainer().getMenu().getMenupanel().getHeight();
        return abstand;
    }

    @Deprecated
    public JMarkerPanel getMarkierung() {
        return designSelectionArea;
    }

    public void hideMarker() {
        designSelectionArea.setVisible(false);
    }

    public boolean isMarkerVisible() {
        return designSelectionArea.isVisible();
    }

    public void moveMarker(int incX, int incY, List<GUIKnotenItem> markedlist) {
        int newMarkerX = designSelectionArea.getX() + incX;
        if (newMarkerX + designSelectionArea.getWidth() >= width || newMarkerX < 0) {
            incX = 0;
        }
        int newMarkerY = designSelectionArea.getY() + incY;
        if (newMarkerY + designSelectionArea.getHeight() >= height || newMarkerY < 0) {
            incY = 0;
        }
        designSelectionArea.setBounds(designSelectionArea.getX() + incX, designSelectionArea.getY() + incY,
                designSelectionArea.getWidth(), designSelectionArea.getHeight());

        for (GUIKnotenItem knotenItem : markedlist) {
            JSidebarButton templbl = knotenItem.getImageLabel();
            templbl.setLocation(templbl.getX() + incX, templbl.getY() + incY);
        }
        updateCables();
    }

    public JMarkerPanel getAuswahl() {
        return designSelection;
    }

    public JSidebarButton getDragVorschau() {
        return designDragPreview;
    }

    public ControlPanel getProperty() {
        return designItemConfig;
    }

    public void showDesktop(GUIKnotenItem hardwareItem) {
        ListIterator<GUIDesktopWindow> it;
        Betriebssystem bs;
        GUIDesktopWindow tmpDesktop = null;
        boolean fertig = false;

        if (hardwareItem != null && hardwareItem.getKnoten() instanceof Host) {
            bs = (Betriebssystem) ((Host) hardwareItem.getKnoten()).getSystemSoftware();

            it = desktopWindowList.listIterator();
            while (!fertig && it.hasNext()) {
                tmpDesktop = it.next();
                if (bs == tmpDesktop.getBetriebssystem()) {
                    tmpDesktop.setVisible(true);
                    fertig = true;
                }
            }

            if (!fertig) {
                tmpDesktop = new GUIDesktopWindow(bs);
                setDesktopPos(tmpDesktop);
                tmpDesktop.setVisible(true);
                tmpDesktop.toFront();
                fertig = true;
            }

            if (tmpDesktop != null) {
                this.desktopWindowList.add(tmpDesktop);
            }
        }
    }

    private void setDesktopPos(GUIDesktopWindow tmpDesktop) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension desktopSize = tmpDesktop.getSize();
        int numberOfDesktopsPerRow = (int) (screenSize.getWidth() / desktopSize.getWidth());
        int numberOfDesktopsPerColumn = (int) (screenSize.getHeight() / desktopSize.getHeight());
        int totalNumberOfDesktops = numberOfDesktopsPerRow * numberOfDesktopsPerColumn;
        int xPos = MIN_DESKTOP_SPACING;
        int yPos = MIN_DESKTOP_SPACING;
        if (desktopWindowList.size() < totalNumberOfDesktops
                && !Information.getDesktopWindowMode().equals(GUIDesktopWindow.Mode.STACK)) {
            if (Information.getDesktopWindowMode().equals(GUIDesktopWindow.Mode.COLUMN)) {
                xPos = MIN_DESKTOP_SPACING
                        + (desktopWindowList.size() / numberOfDesktopsPerColumn) * (int) desktopSize.getWidth();
                yPos = MIN_DESKTOP_SPACING
                        + (desktopWindowList.size() % numberOfDesktopsPerColumn) * (int) desktopSize.getHeight();
            } else {
                xPos = MIN_DESKTOP_SPACING
                        + (desktopWindowList.size() % numberOfDesktopsPerRow) * (int) desktopSize.getWidth();
                yPos = MIN_DESKTOP_SPACING
                        + (desktopWindowList.size() / numberOfDesktopsPerRow) * (int) desktopSize.getHeight();
            }
        } else {
            int overlappingDesktops = Information.getDesktopWindowMode().equals(GUIDesktopWindow.Mode.STACK)
                    ? desktopWindowList.size()
                    : desktopWindowList.size() - totalNumberOfDesktops;
            xPos = (overlappingDesktops + 1) * 20;
            yPos = (overlappingDesktops + 1) * 20;
            if (xPos + desktopSize.getWidth() > screenSize.getWidth()) {
                xPos = MIN_DESKTOP_SPACING;
            }
            if (yPos + desktopSize.getHeight() > screenSize.getHeight()) {
                yPos = MIN_DESKTOP_SPACING;
            }
        }
        tmpDesktop.setBounds(xPos, yPos, tmpDesktop.getWidth(), tmpDesktop.getHeight());
    }

    public void closeDialogs() {
        visibleSystems.clear();
        for (GUIDesktopWindow window : desktopWindowList) {
            if (window.isVisible()) {
                visibleSystems.add(window.getBetriebssystem());
            }
            window.setVisible(false);
        }
        SatViewerControl.getInstance().hideViewer();
        lastVisibilityStateLauscher = ((JDialog) getExchangeDialog()).isVisible();
        ((JDialog) GUIContainer.getGUIContainer().getExchangeDialog()).setVisible(false);
    }

    public void reopenDialogs() {
        Set<SystemSoftware> systemSoftware = new HashSet<>();
        for (GUIKnotenItem node : nodeItems) {
            systemSoftware.add(node.getKnoten().getSystemSoftware());
        }
        Set<GUIDesktopWindow> desktopsToRemove = new HashSet<>();
        for (GUIDesktopWindow window : desktopWindowList) {
            if (!systemSoftware.contains(window.getBetriebssystem())) {
                desktopsToRemove.add(window);
            } else if (visibleSystems.contains(window.getBetriebssystem())) {
                window.setVisible(true);
            }
        }

        for (GUIDesktopWindow window : desktopsToRemove) {
            desktopWindowList.remove(window);
        }

        SatViewerControl.getInstance().reshowViewer(systemSoftware);
        if (lastVisibilityStateLauscher) {
            ((JDialog) getExchangeDialog()).setVisible(true);
        }
        JMainFrame.getJMainFrame().setVisible(true);
    }

    public void setProperty(Hardware hardwareItem) {
        if (designItemConfig != null) {
            designItemConfig.doUnselectAction();
            JMainFrame.getJMainFrame().removeFromContentPane(designItemConfig);
        }
        if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
            if (designItemConfig instanceof SimulationControl) {
                lastSimulationControlMaximized = designItemConfig.isMaximiert();
            }
            designItemConfig = JKonfiguration.getInstance(hardwareItem);
        } else {
            if (designItemConfig instanceof JKonfiguration && designItemConfig.isMaximiert()) {
                lastOpenDesignComponent = ((JKonfiguration) designItemConfig).holeHardware();
            } else {
                lastOpenDesignComponent = null;
            }
            designItemConfig = new SimulationControl();
        }
        JMainFrame.getJMainFrame().addToContentPane(designItemConfig, BorderLayout.SOUTH);
        designItemConfig.updateAttribute();
        designItemConfig.updateUI();
        if (hardwareItem != null || (designItemConfig instanceof SimulationControl && lastSimulationControlMaximized)) {
            designItemConfig.maximieren();
        } else {
            designItemConfig.minimieren();
        }
    }

    public JScrollPane getScrollPane() {
        return designView;
    }

    public int getXOffset() {
        if (activeSite == GUIMainMenu.MODUS_AKTION) {
            return simulationView.getHorizontalScrollBar().getValue();
        }
        return designView.getHorizontalScrollBar().getValue();
    }

    public int getYOffset() {
        if (activeSite == GUIMainMenu.MODUS_AKTION) {
            return simulationView.getVerticalScrollBar().getValue();
        }
        return designView.getVerticalScrollBar().getValue();
    }

    public int getRealXPos(int x) {
        if (activeSite == GUIMainMenu.MODUS_AKTION) {
            return SwingUtilities.convertPoint(simulationView, new Point(x, 0), simulationView.getViewport()).x;
        }
        return SwingUtilities.convertPoint(designView, new Point(x, 0), designView.getViewport()).x;
    }

    public int getRealYPos(int y) {
        if (activeSite == GUIMainMenu.MODUS_AKTION) {
            return SwingUtilities.convertPoint(simulationView, new Point(0, y), simulationView.getViewport()).y;
        }
        return SwingUtilities.convertPoint(designView, new Point(0, y), designView.getViewport()).y;
    }

    public JScrollPane getSidebarScrollpane() {
        if (activeSite == GUIMainMenu.MODUS_ENTWURF) {
            return designSidebarScrollpane;
        }
        return docuSidebarScrollpane;
    }

    public GUINetworkPanel getDesignpanel() {
        return networkPanel;
    }

    public JComponent getSimpanel() {
        return simulationView;
    }

    public GUIDesignSidebar getSidebar() {
        return designSidebar;
    }

    public GUIMainMenu getMenu() {
        return menu;
    }

    public void setKabelPanelVorschau(JCablePanel kabelPanelVorschau) {
        this.kabelPanelVorschau = kabelPanelVorschau;
    }

    public JSidebarButton getLabelforKnoten(Knoten node) {
        List<GUIKnotenItem> list = getKnotenItems();
        for (int i = 0; i < list.size(); i++) {
            if (((GUIKnotenItem) list.get(i)).getKnoten().equals(node)) {
                return ((GUIKnotenItem) list.get(i)).getImageLabel();
            }
        }
        return null;
    }

    public List<GUIDocuItem> getDocuItems() {
        docuPanel.removeElementsFocus(); // Necessary for the size of the selected textArea (if any) to be correctly
                                         // saved
        return docuItems;
    }

    public void removeDocuElement(JDocuElement elem) {
        for (GUIDocuItem item : docuItems) {
            if (item.asDocuElement().equals(elem)) {
                docuItems.remove(item);
                break;
            }
        }
    }

    public void removeCableItem(GUIKabelItem cableItem) {
        cableItems.remove(cableItem);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void showHelp() {
        helpPanel.maximieren();
    }

    public void hideHelp() {
        helpPanel.minimieren();
    }

}
