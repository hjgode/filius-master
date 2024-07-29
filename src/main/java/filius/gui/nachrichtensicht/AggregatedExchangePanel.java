/*
 ** This file is part of Filius, a network construction and simulation software.
 ** 
 ** Originally created at the University of Siegen, Institute "Didactics of
 ** Informatics and E-Learning" by a students' project group:
 **     members (2006-2007): 
 **         AndrÃ© Asschoff, Johannes Bade, Carsten Dittich, Thomas Gerding,
 **         Nadja HaÃŸler, Ernst Johannes Klebert, Michell Weyer
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
package filius.gui.nachrichtensicht;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import filius.hardware.knoten.Host;
import filius.hardware.knoten.InternetKnoten;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.system.SystemSoftware;

/**
 * This class is used to show exchanged messages between components. Its functionality shall be akin to that of
 * wireshark.
 * 
 * @author stefan
 */
@SuppressWarnings({ "serial", "deprecation" })
public class AggregatedExchangePanel extends JTabbedPane implements AggregatedExchangeComponent, I18n, Observer {

    private Hashtable<String, JPanel> openedTabs = new Hashtable<String, JPanel>();
    private Hashtable<String, InternetKnotenBetriebssystem> systems = new Hashtable<String, InternetKnotenBetriebssystem>();
    private Hashtable<String, AggregatedMessageTable> tabellen = new Hashtable<String, AggregatedMessageTable>();

    /**
     * Diese Methode fuegt eine Tabelle hinzu
     */
    @Override
    public void addTable(SystemSoftware system, String identifier) {
        final AggregatedMessageTable tabelle;

        system.addObserver(this);
        final MessageDetailsPanel detailsPanel = new MessageDetailsPanel(identifier);

        if (openedTabs.get(identifier) == null) {
            tabelle = new AggregatedMessageTable(this, identifier);
            tabelle.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (tabelle.getSelectedRow() >= 0) {
                        detailsPanel.update(tabelle.getValueAt(tabelle.getSelectedRow(), 0));
                    } else {
                        detailsPanel.clear();
                    }
                }
            });
            JPanel panel = new JPanel(new BorderLayout());

            JScrollPane scrollPane;
            scrollPane = new JScrollPane(tabelle);
            tabelle.setScrollPane(scrollPane);

            JSplitPane splitPane = new JSplitPane();
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setTopComponent(scrollPane);
            splitPane.setBottomComponent(new JScrollPane(detailsPanel));

            panel.add(splitPane, BorderLayout.CENTER);

            add(panel);
            setSelectedComponent(panel);

            TabTitle title = new TabTitle(this, identifier);
            setTabComponentAt(getSelectedIndex(), title);

            openedTabs.put(identifier, panel);
            systems.put(identifier, (InternetKnotenBetriebssystem) system);
            tabellen.put(identifier, tabelle);

            if (openedTabs.size() > 0) {
                setVisible(true);
            }

            updateTabTitle();
        } else {
            // if there is already a tab opened for this system set it to selected
            setSelectedComponent(openedTabs.get(identifier));
            tabellen.get(identifier).update();
        }
    }

    void updateTabTitle() {
        for (int i = 0; i < getTabCount(); i++) {
            for (String identifier : openedTabs.keySet()) {
                if (getComponentAt(i).equals(openedTabs.get(identifier))) {
                    SystemSoftware system = systems.get(identifier);
                    String ipAddress = ((InternetKnoten) system.getKnoten()).getNetzwerkInterfaceByMac(identifier)
                            .getIp();
                    String tabTitle;
                    if (system.getKnoten() instanceof Host && ((Host) system.getKnoten()).isUseIPAsName()) {
                        tabTitle = ipAddress;
                    } else {
                        tabTitle = system.getKnoten().holeAnzeigeName() + " - " + ipAddress;
                    }
                    TabTitle titlePanel = (TabTitle) getTabComponentAt(i);
                    titlePanel.setTitle(tabTitle);
                    break;
                }
            }
        }
    }

    void clearUnavailableComponents() {
        for (Entry<String, InternetKnotenBetriebssystem> system : systems.entrySet()) {
            if (!system.getValue().isStarted()) {
                removeTable(system.getKey());
            }
        }
    }

    private void removeTable(String mac) {
        removeTable(mac, openedTabs.get(mac));
    }

    @Override
    public void removeTable(String mac, JPanel panel) {
        if (mac != null) {
            openedTabs.remove(mac);
            tabellen.remove(mac);
            remove(panel);
            if (openedTabs.size() == 0) {
                setVisible(false);
            }

        }
    }

    public String getTabTitle(String interfaceId) {
        String title = interfaceId.replaceAll(":", "-");
        for (int i = 0; i < getTabCount(); i++) {
            Component tab = getComponentAt(i);
            if (tab == openedTabs.get(interfaceId)) {
                title = ((TabTitle) getTabComponentAt(i)).getTitle();
                break;
            }
        }
        return title;
    }

    @Override
    public void reset() {}

    private class TabTitle extends JPanel {
        private JLabel label;

        TabTitle(AggregatedExchangePanel parent, String identifier) {
            setOpaque(false);
            label = new JLabel();
            add(label, BorderLayout.WEST);
            JButton btnClose = new JButton("X");
            btnClose.setUI(new BasicButtonUI());
            btnClose.setForeground(Color.GRAY);
            btnClose.setBorder(BorderFactory.createEmptyBorder());
            btnClose.setPreferredSize(new Dimension(18, 18));
            btnClose.setToolTipText(messages.getString("buttontabcomponent_msg1"));
            add(btnClose, BorderLayout.EAST);
            btnClose.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    parent.removeTable(identifier);
                }
            });
        }

        void setTitle(String title) {
            label.setText(title);
        }

        String getTitle() {
            return label.getText();
        }
    }

    private class MessageDetailsPanel extends JPanel {
        private String macAddress;

        public MessageDetailsPanel(String macAddress) {
            this.macAddress = macAddress;
            this.setLayout(new BorderLayout());
            this.setBackground(Color.WHITE);
        }

        public void clear() {
            removeAll();
            updateUI();
        }

        public void update(Object messageNo) {
            if (messageNo != null) {
                Object[][] daten = Lauscher.getLauscher().getDaten(macAddress, false, 0);
                int number = Integer.parseInt(messageNo.toString());
                int dataSetNo = 0;
                int currNo = 0;
                for (; dataSetNo < daten.length; dataSetNo++) {
                    currNo = Integer.parseInt(daten[dataSetNo][0].toString());
                    if (currNo == number)
                        break;
                }

                Object[] dataSet = daten[dataSetNo];
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(messages.getString("rp_lauscher_msg1")
                        + ": " + dataSet[0] + " / " + messages.getString("rp_lauscher_msg2") + ": " + dataSet[1]);
                for (; dataSetNo < daten.length
                        && Integer.parseInt(daten[dataSetNo][0].toString()) == number; dataSetNo++) {
                    dataSet = daten[dataSetNo];
                    DefaultMutableTreeNode layerNode = new DefaultMutableTreeNode(dataSet[5], true);
                    DefaultMutableTreeNode dateNode;
                    DefaultMutableTreeNode labelNode;
                    if (dataSet[2] != null && !dataSet[2].toString().isEmpty()) {
                        String srcLabel = String.format("%-15s", messages.getString("rp_lauscher_msg3") + ": ");
                        dateNode = new DefaultMutableTreeNode(srcLabel + dataSet[2]);
                        layerNode.add(dateNode);
                    }
                    if (dataSet[3] != null && !dataSet[3].toString().isEmpty()) {
                        String destLabel = String.format("%-15s", messages.getString("rp_lauscher_msg4") + ": ");
                        dateNode = new DefaultMutableTreeNode(destLabel + dataSet[3]);
                        layerNode.add(dateNode);
                    }
                    if (dataSet[4] != null && !dataSet[4].toString().isEmpty()) {
                        String protocolLabel = String.format("%-15s", messages.getString("rp_lauscher_msg5") + ": ");
                        dateNode = new DefaultMutableTreeNode(protocolLabel + dataSet[4]);
                        layerNode.add(dateNode);
                    }
                    if (dataSet[6] != null && !dataSet[6].toString().isEmpty()) {
                        String contentLabel = messages.getString("rp_lauscher_msg7");
                        if (dataSet[5] == Lauscher.PROTOKOLL_SCHICHTEN[3]) {
                            contentLabel += " (" + dataSet[6].toString().length() + " Bytes)";
                        }
                        contentLabel += ": ";
                        if (dataSet[6].toString().contains("\n") || dataSet[6].toString().length() > 60) {
                            labelNode = new DefaultMutableTreeNode(contentLabel);
                            dateNode = new DefaultMutableTreeNode(dataSet[6]);
                            labelNode.add(dateNode);
                            layerNode.add(labelNode);
                        } else {
                            dateNode = new DefaultMutableTreeNode(String.format("%-15s", contentLabel) + dataSet[6]);
                            layerNode.add(dateNode);
                        }
                    }
                    rootNode.add(layerNode);
                }
                JTree detailsTree = new JTree(rootNode);
                for (int i = 0; i < detailsTree.getRowCount(); i++) {
                    detailsTree.expandRow(i);
                }
                detailsTree.setCellRenderer(new MultiLineCellRenderer());
                this.removeAll();
                this.add(detailsTree, BorderLayout.WEST);
                this.updateUI();
            }
        }
    }

    // This code is based on an example published at
    // http://www.java2s.com/Code/Java/Swing-Components/MultiLineTreeExample.htm
    class MultiLineCellRenderer extends JPanel implements TreeCellRenderer {
        protected JLabel icon;

        protected TreeTextArea text;

        public MultiLineCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            icon = new JLabel() {
                public void setBackground(Color color) {
                    if (color instanceof ColorUIResource)
                        color = null;
                    super.setBackground(color);
                }
            };
            add(icon);
            add(Box.createHorizontalStrut(4));
            add(text = new TreeTextArea());
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);
            setEnabled(tree.isEnabled());
            text.setText(stringValue);
            text.setSelect(isSelected);
            text.setFocus(hasFocus);
            return this;
        }

        public Dimension getPreferredSize() {
            Dimension iconD = icon.getPreferredSize();
            Dimension textD = text.getPreferredSize();
            int height = iconD.height < textD.height ? textD.height : iconD.height;
            return new Dimension(iconD.width + textD.width, height);
        }

        public void setBackground(Color color) {
            if (color instanceof ColorUIResource)
                color = null;
            super.setBackground(color);
        }

        class TreeTextArea extends JTextArea {
            Dimension preferredSize;

            TreeTextArea() {
                setLineWrap(true);
                setWrapStyleWord(true);
                setOpaque(true);
                Font font = getFont();
                setFont(new Font(Font.MONOSPACED, Font.BOLD, font.getSize()));
            }

            public void setBackground(Color color) {
                if (color instanceof ColorUIResource)
                    color = null;
                super.setBackground(color);
            }

            public void setPreferredSize(Dimension d) {
                if (d != null) {
                    preferredSize = d;
                }
            }

            public Dimension getPreferredSize() {
                return preferredSize;
            }

            public void setText(String str) {
                Font font = getFont();
                FontMetrics fm = getToolkit().getFontMetrics(font);
                BufferedReader br = new BufferedReader(new StringReader(str));
                String line;
                int maxWidth = 0, lines = 0;
                try {
                    while ((line = br.readLine()) != null) {
                        int width = SwingUtilities.computeStringWidth(fm, line);
                        if (maxWidth < width) {
                            maxWidth = width;
                        }
                        lines++;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                lines = (lines < 1) ? 1 : lines;
                int height = fm.getHeight() * lines;
                setPreferredSize(new Dimension(maxWidth + 12, height));
                super.setText(str);
            }

            void setSelect(boolean isSelected) {
                Color bColor;
                if (isSelected) {
                    bColor = UIManager.getColor("Tree.selectionBackground");
                } else {
                    bColor = UIManager.getColor("Tree.textBackground");
                }
                super.setBackground(bColor);
            }

            void setFocus(boolean hasFocus) {
                if (hasFocus) {
                    Color lineColor = UIManager.getColor("Tree.selectionBorderColor");
                    setBorder(BorderFactory.createLineBorder(lineColor));
                } else {
                    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                }
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        updateTabTitle();
    }
}
