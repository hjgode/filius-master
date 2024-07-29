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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.gui.nachrichtensicht.ExchangeComponent;
import filius.gui.netzwerksicht.GUIKabelItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.gui.netzwerksicht.JCablePanel;
import filius.gui.netzwerksicht.JKonfiguration;
import filius.gui.netzwerksicht.JSidebarButton;
import filius.hardware.Kabel;
import filius.hardware.NetzwerkInterface;
import filius.hardware.Port;
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
import filius.rahmenprogramm.SzenarioVerwaltung;
import filius.software.system.Betriebssystem;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.system.SwitchFirmware;

public class GUIEvents implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(GUIEvents.class);

    private int auswahlx, auswahly, auswahlx2, auswahly2, mausposx, mausposy;

    private int startPosX, startPosY;

    private int shiftX, shiftY;

    private GUIKabelItem neuesKabel;

    private static GUIEvents ref;

    private JSidebarButton aktiveslabel = null;

    private boolean aufmarkierung = false;

    private List<GUIKnotenItem> markedlist;

    private GUIKnotenItem aktivesItem;
    private Map<Knoten, Boolean> statusIsWANPort = new HashMap<>();
    private boolean lastStatusIsWANPort;
    private GUIKnotenItem lastGateway;

    private JCablePanel kabelPanelVorschau;

    private GUIEvents() {
        markedlist = new LinkedList<GUIKnotenItem>();
    }

    public static GUIEvents getGUIEvents() {
        if (ref == null) {
            ref = new GUIEvents();
        }

        return ref;
    }

    public void mausReleased() {
        GUIContainer c = GUIContainer.getGUIContainer();

        List<GUIKnotenItem> itemlist = c.getKnotenItems();
        JMarkerPanel auswahl = c.getAuswahl();
        JMarkerPanel markierung = c.getMarkierung();

        SzenarioVerwaltung.getInstance().setzeGeaendert();

        if (auswahl.isVisible()) {
            int tx, ty, twidth, theight;
            int minx = 999999, miny = 999999, maxx = 0, maxy = 0;
            markedlist = new LinkedList<GUIKnotenItem>();
            for (GUIKnotenItem tempitem : itemlist) {
                tx = tempitem.getImageLabel().getX();
                twidth = tempitem.getImageLabel().getWidth();
                ty = tempitem.getImageLabel().getY();
                theight = tempitem.getImageLabel().getHeight();

                int itemPosX = tx + twidth / 2;
                int itemPosY = ty + theight / 2;

                if (itemPosX >= auswahl.getX() && itemPosX <= auswahl.getX() + auswahl.getWidth()
                        && itemPosY >= auswahl.getY() && itemPosY <= auswahl.getY() + auswahl.getHeight()) {
                    minx = Math.min(tx, minx);
                    maxx = Math.max(tx + twidth, maxx);
                    miny = Math.min(ty, miny);
                    maxy = Math.max(ty + theight, maxy);

                    markedlist.add(tempitem);
                }
            }
            if (!this.markedlist.isEmpty()) {
                markierung.setBounds(minx, miny, maxx - minx, maxy - miny);
                markierung.setVisible(true);
            }
            auswahl.setVisible(false);
        }
    }

    public void mausDragged(MouseEvent e) {
        // do not allow dragging while cable connector is visible, i.e., during
        // cable assignment
        if (GUIContainer.getGUIContainer().getKabelvorschau().isVisible()) {
            return;
        }

        GUIContainer c = GUIContainer.getGUIContainer();
        JMarkerPanel auswahl = c.getAuswahl();
        JSidebarButton dragVorschau = c.getDragVorschau();

        SzenarioVerwaltung.getInstance().setzeGeaendert();

        // Einzelnes Item verschieben
        if (!c.isMarkerVisible()) {
            if (aktiveslabel != null && !dragVorschau.isVisible()) {
                int neuX = e.getX() + shiftX;
                if (neuX < 0) {
                    neuX = 0;
                } else {
                    int maxX = GUIContainer.getGUIContainer().getWidth() - aktiveslabel.getWidth();
                    if (neuX > maxX) {
                        neuX = maxX - 1;
                    }
                }

                int neuY = e.getY() + shiftY;
                if (neuY < 0) {
                    neuY = 0;
                } else {
                    int maxY = GUIContainer.getGUIContainer().getHeight() - aktiveslabel.getHeight();
                    if (neuY > maxY) {
                        neuY = maxY - 1;
                    }
                }

                aktiveslabel.setLocation(neuX, neuY);
                c.updateCables();
            } else {
                mausposx = e.getX();
                mausposy = e.getY();
                if (!auswahl.isVisible()) {
                    auswahlx = mausposx;
                    auswahly = mausposy;
                    auswahlx2 = auswahlx;
                    auswahly2 = auswahly;

                    auswahl.setBounds(auswahlx, auswahly, auswahlx2 - auswahlx, auswahly2 - auswahly);
                    auswahl.setVisible(true);
                } else {
                    auswahlx2 = mausposx;
                    auswahly2 = mausposy;

                    auswahl.setBounds(auswahlx, auswahly, auswahlx2 - auswahlx, auswahly2 - auswahly);

                    if (mausposx < auswahlx) {
                        auswahl.setBounds(auswahlx2, auswahly, auswahlx - auswahlx2, auswahly2 - auswahly);
                    }
                    if (mausposy < auswahly) {
                        auswahl.setBounds(auswahlx, auswahly2, auswahlx2 - auswahlx, auswahly - auswahly2);
                    }
                    if (mausposy < auswahly && mausposx < auswahlx) {
                        auswahl.setBounds(auswahlx2, auswahly2, auswahlx - auswahlx2, auswahly - auswahly2);
                    }
                }
            }
        }
        // Items im Auswahlrahmen verschieben
        else if (!dragVorschau.isVisible()) {
            /* Verschieben mehrerer ausgewaehlter Objekte */
            if (aufmarkierung && markedlist.size() > 0 && e.getX() >= 0 && e.getX() <= c.getWidth() && e.getY() >= 0
                    && e.getY() <= c.getHeight()) {

                int verschiebungx = startPosX - e.getX();
                startPosX = e.getX();
                int verschiebungy = startPosY - e.getY();
                startPosY = e.getY();

                c.moveMarker(-verschiebungx, -verschiebungy, markedlist);
            } else {
                LOG.debug("Out of Boundaries!");
            }
        }
    }

    public void mausPressedDesignMode(MouseEvent e) {
        GUIContainer c = GUIContainer.getGUIContainer();
        JMarkerPanel auswahl = c.getAuswahl();

        SzenarioVerwaltung.getInstance().setzeGeaendert();
        updateAktivesItem(e.getX(), e.getY());

        if (GUIContainer.getGUIContainer().getMarkierung().inBounds(e.getX(), e.getY())) {
            if (GUIContainer.getGUIContainer().isMarkerVisible()) {
                aufmarkierung = true;
                startPosX = e.getX();
                startPosY = e.getY();
            }
        } else {
            aufmarkierung = false;
            GUIContainer.getGUIContainer().hideMarker();
            auswahl.setBounds(0, 0, 0, 0);
        }

        // Wurde die rechte Maustaste betaetigt?
        if (e.getButton() == 3) {
            if (aktivesItem != null && aktiveslabel != null) {
                hideAuxPanel(false);

                if (!c.getKabelvorschau().isVisible()) {
                    kontextMenueEntwurfsmodus(e.getX(), e.getY());
                } else {
                    resetAndHideCablePreview();
                }
            } else {
                GUIKabelItem cableItem = findClickedCable(e);
                if ((kabelPanelVorschau == null || !kabelPanelVorschau.isVisible())
                        && GUIContainer.getGUIContainer().getActiveSite() == GUIMainMenu.MODUS_ENTWURF
                        && cableItem != null) {
                    contextMenuCable(cableItem, e.getX(), e.getY());
                } else {
                    resetAndHideCablePreview();
                }
            }
        }
        // Wurde die linke Maustaste betaetigt?
        else if (e.getButton() == 1) {
            // eine neue Kabelverbindung erstellen
            if (aktivesItem != null && aktiveslabel != null) {
                if (GUIContainer.getGUIContainer().getKabelvorschau().isVisible()) {
                    // hide property panel (JKonfiguration)
                    GUIContainer.getGUIContainer().getProperty().minimieren();

                    if (aktivesItem.getKnoten() instanceof Knoten) {
                        Knoten tempKnoten = (Knoten) aktivesItem.getKnoten();
                        boolean success = true;
                        if (tempKnoten instanceof Gateway) {
                            if (lastStatusIsWANPort && !((Gateway) tempKnoten).checkWANPortUnconnected()) {
                                GUIErrorHandler.getGUIErrorHandler()
                                        .DisplayError(messages.getString("guievents_msg22"));
                                success = false;
                            } else if (!lastStatusIsWANPort && !((Gateway) tempKnoten).checkLANPortUnconnected()) {
                                GUIErrorHandler.getGUIErrorHandler()
                                        .DisplayError(messages.getString("guievents_msg23"));
                                success = false;
                            } else {
                                statusIsWANPort.put(lastGateway.getKnoten(), lastStatusIsWANPort);
                            }
                        } else {
                            Port anschluss = tempKnoten.holeFreienPort();
                            if (anschluss == null) {
                                success = false;
                                GUIErrorHandler.getGUIErrorHandler().DisplayError(messages.getString("guievents_msg1"));
                            }
                        }
                        if (success && null != neuesKabel && null != neuesKabel.getKabelpanel().getZiel1()) {
                            Knoten quellKnoten = neuesKabel.getKabelpanel().getZiel1().getKnoten();
                            if (tempKnoten.getSystemSoftware().wireless() && !(quellKnoten instanceof Switch)
                                    || quellKnoten.getSystemSoftware().wireless() && !(tempKnoten instanceof Switch)) {
                                success = false;
                                GUIErrorHandler.getGUIErrorHandler()
                                        .DisplayError(messages.getString("guievents_msg26"));
                            }
                            if (tempKnoten.checkConnected(quellKnoten)) {
                                success = false;
                                GUIErrorHandler.getGUIErrorHandler()
                                        .DisplayError(messages.getString("guievents_msg12"));
                            }
                        }
                        if (success) {
                            processCableConnection(e.getX(), e.getY());
                        }
                    }
                } else {
                    // einen Knoten zur Bearbeitung der Eigenschaften
                    // auswaehlen
                    if (GUIContainer.getGUIContainer().getKabelvorschau().isVisible()) {
                        resetAndHideCablePreview();
                    }

                    c.setProperty(aktivesItem.getKnoten());
                    c.getProperty().updateAttribute();
                    if (e.getClickCount() == 2) {
                        GUIContainer.getGUIContainer().getProperty().maximieren();
                    }
                    if (!aktiveslabel.isSelektiert()) {
                        aktiveslabel.setSelektiert(true);
                        // Die Verschiebung speichern für spätere Verwendung in mausDragged
                        shiftX = aktiveslabel.getX() - e.getX();
                        shiftY = aktiveslabel.getY() - e.getY();
                    }
                }
            } else {
                // wurde Maus ueber leerem Bereich betaetigt? -> Markierung
                // sichtbar machen
                auswahl.setVisible(false);
                hideAuxPanel(false);
            }
        }
    }

    protected void hideAuxPanel(boolean onlyHelp) {
        if (!onlyHelp) {
            GUIContainer.getGUIContainer().getProperty().minimieren();
        }
        GUIContainer.getGUIContainer().hideHelp();
    }

    public void cancelMultipleSelection() {
        aufmarkierung = false;
        GUIContainer.getGUIContainer().hideMarker();
        GUIContainer.getGUIContainer().getAuswahl().setBounds(0, 0, 0, 0);
    }

    public void connectNodes(GUIKnotenItem start, GUIKnotenItem dest) {
        if (null != start && null != dest) {
            GUIKabelItem connection = new GUIKabelItem();
            connection.getKabelpanel().setZiel1(start);
            connection.getKabelpanel().setZiel2(dest);

        }
    }

    private void processCableConnection(int currentPosX, int currentPosY) {
        if (null == neuesKabel.getKabelpanel().getZiel1()) {
            neuesKabel.getKabelpanel().setZiel1(aktivesItem);
            setConnectionPreviewStart(currentPosX, currentPosY);
        } else if (null != neuesKabel.getKabelpanel().getZiel1() && null == neuesKabel.getKabelpanel().getZiel2()
                && neuesKabel.getKabelpanel().getZiel1() != aktivesItem) {
            createConnection(neuesKabel.getKabelpanel().getZiel1(), aktivesItem);
            resetAndShowCablePreview(currentPosX, currentPosY);
        }
    }

    private void setConnectionPreviewStart(int currentPosX, int currentPosY) {
        LOG.debug("plug cable to first component {}", aktivesItem.getKnoten().getName());
        GUIContainer.getGUIContainer().getKabelvorschau()
                .setIcon(new ImageIcon(getClass().getResource("/gfx/allgemein/ziel2.png")));
        kabelPanelVorschau = new JCablePanel();
        GUIContainer.getGUIContainer().getDesignpanel().add(kabelPanelVorschau);
        kabelPanelVorschau.setZiel1(aktivesItem);
        GUIContainer.getGUIContainer().setZiel2Label(new JSidebarButton());
        GUIKnotenItem pseudoItem = new GUIKnotenItem();
        pseudoItem.setImageLabel(GUIContainer.getGUIContainer().getZiel2Label());

        GUIContainer.getGUIContainer().getZiel2Label().setBounds(currentPosX, currentPosY, 8, 8);
        kabelPanelVorschau.setZiel2(pseudoItem);
        kabelPanelVorschau.setVisible(true);
        GUIContainer.getGUIContainer().setKabelPanelVorschau(kabelPanelVorschau);
    }

    private GUIKabelItem findClickedCable(MouseEvent e) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + ", clickedCable(" + e + ")");
        // Falls kein neues Objekt erstellt werden soll
        int xPos = GUIContainer.getGUIContainer().getRealXPos(e.getX());
        int yPos = GUIContainer.getGUIContainer().getRealYPos(e.getY());

        for (GUIKabelItem tempitem : GUIContainer.getGUIContainer().getCableItems()) {
            // item clicked, i.e., mouse pointer within item bounds
            if (tempitem.getKabelpanel().clicked(xPos, yPos)) {
                // mouse pointer really close to the drawn line, too
                return tempitem;
            }
        }
        return null;
    }

    private void updateAktivesItem(int posX, int posY) {
        // Falls kein neues Objekt erstellt werden soll
        aktiveslabel = null;
        aktivesItem = null;

        if (!GUIContainer.getGUIContainer().isMarkerVisible()) {
            for (GUIKnotenItem tempitem : GUIContainer.getGUIContainer().getKnotenItems()) {
                JSidebarButton templabel = tempitem.getImageLabel();
                templabel.setSelektiert(false);
                templabel.revalidate();
                templabel.updateUI();

                if (templabel.inBounds(posX, posY)) {
                    aktivesItem = tempitem;
                    aktiveslabel = tempitem.getImageLabel();
                }
            }
        }
        if (aktiveslabel != null && aktiveslabel.getParent() != null) {
            aktiveslabel.getParent().setComponentZOrder(aktiveslabel, 0);
        }
    }

    public void updateGatewayPort(int posX, int posY) {
        for (GUIKnotenItem tempitem : GUIContainer.getGUIContainer().getKnotenItems()) {
            JSidebarButton templabel = tempitem.getImageLabel();
            if (templabel.inBounds(posX, posY) && tempitem.getKnoten() instanceof Gateway) {
                if (posX - templabel.getX() < templabel.getWidth() / 2) {
                    lastStatusIsWANPort = true;
                    templabel.setTemporaryText(messages.getString("guievents_msg24"));
                } else {
                    lastStatusIsWANPort = false;
                    templabel.setTemporaryText(messages.getString("guievents_msg25"));
                }
                LOG.trace(posX - templabel.getX() + " / " + templabel.getWidth());
                lastGateway = tempitem;
            } else if (lastGateway != null && templabel.equals(lastGateway.getImageLabel())) {
                templabel.resetText();
                lastGateway = null;
            }
        }
    }

    public GUIKnotenItem getActiveItem() {
        return aktivesItem;
    }

    /*
     * method called in case of new item creation in GUIContainer, such that this creation process will be registered
     * and the according item is marked active
     */
    public void setNewItemActive(GUIKnotenItem item) {
        aktivesItem = item;
    }

    public Kabel createConnection(GUIKnotenItem component1, GUIKnotenItem component2) {
        LOG.debug("connect componente {} <-> {}", component1.getKnoten().getName(), component2.getKnoten().getName());
        Port anschluss1 = null;
        boolean wireless = false;
        if (component1.getKnoten() instanceof Modem) {
            Modem vrOut = (Modem) component1.getKnoten();
            anschluss1 = vrOut.getErstenAnschluss();
        } else if (component1.getKnoten() instanceof Vermittlungsrechner) {
            Vermittlungsrechner r = (Vermittlungsrechner) component1.getKnoten();
            anschluss1 = r.holeFreienPort();
        } else if (component1.getKnoten() instanceof Switch) {
            Switch sw = (Switch) component1.getKnoten();
            anschluss1 = ((SwitchFirmware) sw.getSystemSoftware()).getKnoten().holeFreienPort();
        } else if (component1.getKnoten() instanceof Gateway) {
            if (statusIsWANPort.getOrDefault(component1.getKnoten(), Boolean.TRUE)) {
                anschluss1 = ((Gateway) component1.getKnoten()).holeWANInterface().getPort();
            } else {
                anschluss1 = ((Gateway) component1.getKnoten()).holeLANInterface().getPort();
            }
        } else if (component1.getKnoten() instanceof InternetKnoten) {
            NetzwerkInterface nic1 = (NetzwerkInterface) ((InternetKnoten) component1.getKnoten())
                    .getNetzwerkInterfaces().get(0);
            anschluss1 = nic1.getPort();
            wireless = nic1.isWireless();
        }

        Port anschluss2 = null;
        if (component2.getKnoten() instanceof Modem) {
            Modem vrOut = (Modem) component2.getKnoten();
            anschluss2 = vrOut.getErstenAnschluss();
        } else if (component2.getKnoten() instanceof Vermittlungsrechner) {
            Vermittlungsrechner r = (Vermittlungsrechner) component2.getKnoten();
            anschluss2 = r.holeFreienPort();
        } else if (component2.getKnoten() instanceof Switch) {
            Switch sw = (Switch) component2.getKnoten();
            anschluss2 = ((SwitchFirmware) sw.getSystemSoftware()).getKnoten().holeFreienPort();
        } else if (component2.getKnoten() instanceof Gateway) {
            if (statusIsWANPort.getOrDefault(component2.getKnoten(), Boolean.TRUE)) {
                anschluss2 = ((Gateway) component2.getKnoten()).holeWANInterface().getPort();
            } else {
                anschluss2 = ((Gateway) component2.getKnoten()).holeLANInterface().getPort();
            }
        } else if (component2.getKnoten() instanceof InternetKnoten) {
            NetzwerkInterface nic2 = (NetzwerkInterface) ((InternetKnoten) component2.getKnoten())
                    .getNetzwerkInterfaces().get(0);
            anschluss2 = nic2.getPort();
            wireless = wireless || nic2.isWireless();
        }

        Kabel connection = new Kabel(anschluss1, anschluss2);
        connection.setWireless(wireless);

        GUIKabelItem kabelItem = new GUIKabelItem();
        kabelItem.getKabelpanel().setZiel1(component1);
        kabelItem.getKabelpanel().setZiel2(component2);

        kabelItem.setDasKabel(connection);
        GUIContainer.getGUIContainer().getDesignpanel().add(kabelItem.getKabelpanel());
        kabelItem.getKabelpanel().updateBounds();
        GUIContainer.getGUIContainer().getDesignpanel().updateUI();
        GUIContainer.getGUIContainer().getCableItems().add(kabelItem);
        return connection;
    }

    public void resetAndHideCablePreview() {
        resetCableTool();
        hideCableToolPanel();
    }

    private void hideCableToolPanel() {
        GUIContainer.getGUIContainer().getKabelvorschau().setVisible(false);
    }

    private void resetCableTool() {
        GUIContainer.getGUIContainer().getKabelvorschau()
                .setIcon(new ImageIcon(getClass().getResource("/gfx/allgemein/ziel1.png")));
        neuesKabel = new GUIKabelItem();
        if (kabelPanelVorschau != null) {
            kabelPanelVorschau.setVisible(false);
        }
    }

    public void resetAndShowCablePreview(int currentPosX, int currentPosY) {
        resetCableTool();
        showCableToolPanel(currentPosX, currentPosY);
        cancelMultipleSelection();
    }

    private void showCableToolPanel(int currentPosX, int currentPosY) {
        JSidebarButton cablePreview = GUIContainer.getGUIContainer().getKabelvorschau();
        cablePreview.setBounds(currentPosX, currentPosY, cablePreview.getWidth(), cablePreview.getHeight());
        cablePreview.setVisible(true);
    }

    /**
     * @author Johannes Bade & Thomas Gerding
     * 
     *         Bei rechter Maustaste auf ein Item (bei Laufendem Aktionsmodus) wird ein Kontextmenü angezeigt, in dem
     *         z.B. der Desktop angezeigt werden kann.
     * 
     * @param templabel
     *            Item auf dem das Kontextmenü erscheint
     * @param e
     *            MouseEvent (Für Position d. Kontextmenü u.a.)
     */
    public void kontextMenueAktionsmodus(final GUIKnotenItem knotenItem, int posX, int posY) {
        if (knotenItem != null) {
            if (knotenItem.getKnoten() instanceof InternetKnoten) {

                JPopupMenu popmen = new JPopupMenu();

                ActionListener al = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (e.getActionCommand().equals("desktopanzeigen")) {
                            GUIContainer.getGUIContainer().showDesktop(knotenItem);
                        }

                        if (e.getActionCommand().startsWith("datenaustausch")) {
                            String macAddress = e.getActionCommand().substring(15);
                            datenAustauschAnzeigen(knotenItem, macAddress);
                        }

                    }
                };

                JMenuItem pmVROUTKonf = new JMenuItem(messages.getString("guievents_msg2"));
                pmVROUTKonf.setActionCommand("vroutkonf");
                pmVROUTKonf.addActionListener(al);

                JMenuItem pmDesktopAnzeigen = new JMenuItem(messages.getString("guievents_msg3"));
                pmDesktopAnzeigen.setActionCommand("desktopanzeigen");
                pmDesktopAnzeigen.addActionListener(al);
                if (knotenItem.getKnoten() instanceof Rechner || knotenItem.getKnoten() instanceof Notebook) {
                    popmen.add(pmDesktopAnzeigen);
                }

                InternetKnoten node = (InternetKnoten) knotenItem.getKnoten();
                for (NetzwerkInterface nic : node.getNetzwerkInterfaces()) {
                    JMenuItem pmDatenAustauschAnzeigen = new JMenuItem(
                            messages.getString("guievents_msg4") + " (" + nic.getIp() + ")");
                    pmDatenAustauschAnzeigen.setActionCommand("datenaustausch-" + nic.getMac());
                    pmDatenAustauschAnzeigen.addActionListener(al);

                    popmen.add(pmDatenAustauschAnzeigen);
                }

                knotenItem.getImageLabel().add(popmen);
                popmen.setVisible(true);
                popmen.show(knotenItem.getImageLabel(), posX, posY);
            }
        }
    }

    private void datenAustauschAnzeigen(GUIKnotenItem item, String macAddress) {
        InternetKnotenBetriebssystem bs;
        ExchangeComponent exchangeDialog = GUIContainer.getGUIContainer().getExchangeDialog();

        if (item.getKnoten() instanceof InternetKnoten) {
            bs = (InternetKnotenBetriebssystem) ((InternetKnoten) item.getKnoten()).getSystemSoftware();
            exchangeDialog.addTable(bs, macAddress);
            ((JDialog) exchangeDialog).setVisible(true);
        }
    }

    /**
     * @author Johannes Bade & Thomas Gerding
     * 
     *         Bei rechter Maustaste auf ein Item (bei Laufendem Entwurfsmodus) wird ein Kontextmenü angezeigt, in dem
     *         z.B. das Item gelöscht, kopiert oder ausgeschnitten werden kann.
     * @param e
     *            MouseEvent (Für Position d. Kontextmenü u.a.)
     */
    private void kontextMenueEntwurfsmodus(int posX, int posY) {
        String textKabelEntfernen;

        updateAktivesItem(posX, posY);

        if (aktivesItem != null) {
            if (aktivesItem.getKnoten() instanceof Rechner || aktivesItem.getKnoten() instanceof Notebook) {
                textKabelEntfernen = messages.getString("guievents_msg5");
            } else {
                textKabelEntfernen = messages.getString("guievents_msg6");
            }

            final JMenuItem pmShowConfig = new JMenuItem(messages.getString("guievents_msg11"));
            pmShowConfig.setActionCommand("showconfig");
            final JMenuItem pmKabelEntfernen = new JMenuItem(textKabelEntfernen);
            pmKabelEntfernen.setActionCommand("kabelentfernen");
            final JMenuItem pmLoeschen = new JMenuItem(messages.getString("guievents_msg7"));
            pmLoeschen.setActionCommand("del");

            JPopupMenu popmen = new JPopupMenu();
            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    if (e.getActionCommand() == pmLoeschen.getActionCommand()) {
                        itemLoeschen(aktiveslabel, aktivesItem);
                    } else if (e.getActionCommand() == pmKabelEntfernen.getActionCommand()) {
                        removeAllConnectionsFromActiveItem();
                    } else if (e.getActionCommand() == pmShowConfig.getActionCommand()) {
                        GUIContainer.getGUIContainer().setProperty(aktivesItem.getKnoten());
                        GUIContainer.getGUIContainer().getProperty().maximieren();
                    }
                }
            };

            pmLoeschen.addActionListener(al);
            pmKabelEntfernen.addActionListener(al);
            pmShowConfig.addActionListener(al);

            popmen.add(pmShowConfig);
            popmen.add(pmKabelEntfernen);
            popmen.add(pmLoeschen);

            GUIContainer.getGUIContainer().getDesignpanel().add(popmen);
            popmen.setVisible(true);
            popmen.show(GUIContainer.getGUIContainer().getDesignpanel(), posX, posY);
        }
    }

    /**
     * context menu in case of clicking on single cable item --> used for deleting a single cable
     */
    private void contextMenuCable(final GUIKabelItem cable, int posX, int posY) {
        if (!cable.getDasKabel().getWireless()) {
            final JMenuItem pmRemoveCable = new JMenuItem(messages.getString("guievents_msg5"));
            pmRemoveCable.setActionCommand("removecable");

            JPopupMenu popmen = new JPopupMenu();
            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand() == pmRemoveCable.getActionCommand()) {
                        removeConnection(cable);
                    }
                }
            };

            pmRemoveCable.addActionListener(al);
            popmen.add(pmRemoveCable);

            GUIContainer.getGUIContainer().getDesignpanel().add(popmen);
            popmen.setVisible(true);
            popmen.show(GUIContainer.getGUIContainer().getDesignpanel(), posX, posY);
        }
    }

    /**
     * 
     * Löscht das durch loeschlabel angegebene Item NOTE: made public for using del key to delete items without local
     * context menu action (cf. JMainFrame)
     */
    public void itemLoeschen(JSidebarButton loeschlabel, GUIKnotenItem loeschitem) {
        loeschlabel.setVisible(false);
        GUIContainer.getGUIContainer().setProperty(null);
        ListIterator<GUIKabelItem> iteratorAlleKabel = GUIContainer.getGUIContainer().getCableItems().listIterator();
        GUIKabelItem kabel = new GUIKabelItem();
        LinkedList<GUIKabelItem> loeschKabel = new LinkedList<GUIKabelItem>();

        // Zu löschende Elemente werden in eine temporäre Liste gepackt
        while (iteratorAlleKabel.hasNext()) {
            kabel = (GUIKabelItem) iteratorAlleKabel.next();
            if (kabel.getKabelpanel().getZiel1().equals(loeschitem)
                    || kabel.getKabelpanel().getZiel2().equals(loeschitem)) {
                loeschKabel.add(kabel);
            }
        }

        // Temporäre Liste der zu löschenden Kabel wird iteriert und dabei
        // werden die Kabel aus der globalen Kabelliste gelöscht
        // und vom Panel entfernt
        ListIterator<GUIKabelItem> iteratorLoeschKabel = loeschKabel.listIterator();
        while (iteratorLoeschKabel.hasNext()) {
            kabel = iteratorLoeschKabel.next();

            this.removeConnection(kabel);
        }

        GUIContainer.getGUIContainer().removeNodeItem(loeschitem);
        GUIContainer.getGUIContainer().getDesignpanel().remove(loeschlabel);
        GUIContainer.getGUIContainer().getDesignpanel().updateUI();
        GUIContainer.getGUIContainer().updateViewport();
    }

    /** remove the connection from model and view */
    public void removeConnection(GUIKabelItem cable) {
        LOG.trace("INVOKED filius.gui.GUIEvents, removeConnection(" + cable + ")");
        if (cable == null)
            return;

        cable.getDasKabel().anschluesseTrennen();
        GUIContainer.getGUIContainer().getCableItems().remove(cable);
        GUIContainer.getGUIContainer().getDesignpanel().remove(cable.getKabelpanel());

        JKonfiguration.getInstance(cable.getKabelpanel().getZiel1().getKnoten()).updateAttribute();
        JKonfiguration.getInstance(cable.getKabelpanel().getZiel2().getKnoten()).updateAttribute();
        GUIContainer.getGUIContainer().updateViewport();
    }

    /** Entfernt alle Verbindungen, die am aktuell selektierten Knoten angeschlossen sind. */
    private void removeAllConnectionsFromActiveItem() {
        // Zu löschende Elemente werden in eine temporäre Liste gepackt
        LinkedList<GUIKabelItem> loeschListe = new LinkedList<GUIKabelItem>();
        for (GUIKabelItem tempKabel : GUIContainer.getGUIContainer().getCableItems()) {
            if (tempKabel.getKabelpanel().getZiel1().equals(aktivesItem)) {
                loeschListe.add(tempKabel);
            }

            if (tempKabel.getKabelpanel().getZiel2().equals(aktivesItem)) {
                loeschListe.add(tempKabel);
            }
        }

        // Temporäre Liste der zu löschenden Kabel wird iteriert und dabei
        // werden die Kabel aus der globalen Kabelliste gelöscht
        // und vom Panel entfernt
        boolean wireless = false;
        for (GUIKabelItem tempKabel : loeschListe) {
            this.removeConnection(tempKabel);
            if (tempKabel.getDasKabel().getWireless()) {
                wireless = true;
            }
        }
        if (wireless && aktivesItem.getKnoten() instanceof Host) {
            ((Betriebssystem) aktivesItem.getKnoten().getSystemSoftware()).setSsid(null);
        }
        JKonfiguration.getInstance(aktivesItem.getKnoten()).updateAttribute();
    }

    public void mausPressedActionMode(MouseEvent e) {
        if (e.getButton() == 1) {
            hideAuxPanel(false);
        }
    }

    public void mausPressedDocuMode(MouseEvent e) {
        if (e.getButton() == 1) {
            hideAuxPanel(true);
        }
    }
}
