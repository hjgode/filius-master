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
package filius.gui.anwendungssicht;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.software.dns.DNSServer;
import filius.software.dns.ResourceRecord;
import filius.software.vermittlungsschicht.IP;

@SuppressWarnings({ "serial", "deprecation" })
public class GUIApplicationDNSServerWindow extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationDNSServerWindow.class);

    private JTextField aDomainField;
    private JTextField mxURLField;
    private JTextField aIpField;
    private JTextField mxMaildomainField;
    private JTextField nsDomainField;
    private JTextField nsDomainServerField;

    private JCheckBox recResolution;
    private JButton buttonStart;

    private DNSConfigTable aRecordsTable;
    private DNSConfigTable mxRecordsTable;
    private DNSConfigTable nsRecordsTable;

    public GUIApplicationDNSServerWindow(final GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);

        initialisiereKomponenten();
        aktualisieren();
    }

    private void initialisiereKomponenten() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(createServerControlBox(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(messages.getString("dnsserver_msg2"),
                new ImageIcon(getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), createAPanel());
        tabbedPane.addTab(messages.getString("dnsserver_msg3"),
                new ImageIcon(getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), createMXPanel());
        tabbedPane.addTab(messages.getString("dnsserver_msg15"),
                new ImageIcon(getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), createNSPanel());
        Box hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        hBox.add(tabbedPane);
        contentPane.add(hBox, BorderLayout.CENTER);
        add(contentPane, BorderLayout.CENTER);
    }

    private Box createServerControlBox() {
        Box hBox;
        buttonStart = new JButton(messages.getString("dnsserver_msg1"));
        buttonStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (((DNSServer) holeAnwendung()).isAktiv()) {
                    ((DNSServer) holeAnwendung()).setAktiv(false);
                } else {
                    ((DNSServer) holeAnwendung()).setAktiv(true);
                }
            }
        });
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        hBox.add(buttonStart);

        recResolution = new JCheckBox();
        recResolution.setText(messages.getString("dnsserver_msg20"));
        DNSServer dnsServer = (DNSServer) this.holeAnwendung();
        recResolution.setSelected(dnsServer.isRecursiveResolutionEnabled());
        recResolution.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox checkBox = GUIApplicationDNSServerWindow.this.recResolution;
                boolean activated = checkBox.isSelected();
                DNSServer dnsServer = (DNSServer) GUIApplicationDNSServerWindow.this.holeAnwendung();
                dnsServer.setRecursiveResolutionEnabled(activated);
                checkBox.setSelected(dnsServer.isRecursiveResolutionEnabled());
            }
        });
        hBox.add(Box.createHorizontalStrut(20));
        hBox.add(recResolution);
        return hBox;
    }

    private JPanel createAPanel() {
        Box vBox, hBox;
        DefaultTableModel tabellenModell;
        TableColumnModel tcm;
        JScrollPane scrollPane;

        JPanel aPanel = new JPanel(new BorderLayout());

        vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel aDomainLabel = new JLabel(messages.getString("dnsserver_msg4"));
        aDomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(aDomainLabel);
        hBox.add(Box.createHorizontalStrut(5));

        aDomainField = new JTextField();
        aDomainField.setPreferredSize(new Dimension(275, 25));
        aDomainField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkFQDN(aDomainField);
            }
        });
        hBox.add(aDomainField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel aIpLabel = new JLabel(messages.getString("dnsserver_msg5"));
        aIpLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(aIpLabel);
        hBox.add(Box.createHorizontalStrut(5));

        aIpField = new JTextField(this.holeAnwendung().getSystemSoftware().primaryIPAdresse());
        aIpField.setPreferredSize(new Dimension(275, 25));
        aIpField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkIP(aIpField);
            }
        });
        hBox.add(aIpField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JButton aAddButton = new JButton(messages.getString("dnsserver_msg6"));
        aAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (checkFQDN(aDomainField) && checkIP(aIpField) && IP.ipCheck(aIpField.getText()) != null) {
                    ((DNSServer) holeAnwendung()).hinzuRecord(aDomainField.getText(), ResourceRecord.ADDRESS,
                            aIpField.getText());
                    aDomainField.setText("");
                    aIpField.setText("");
                    updateARecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(messages.getString("dnsserver_msg21"));
                }
            }
        });
        hBox.add(aAddButton);
        hBox.add(Box.createHorizontalStrut(5));

        JButton buttonEntfernen = new JButton(messages.getString("dnsserver_msg7"));
        buttonEntfernen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int zeilenNummer = aRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = aRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer) holeAnwendung()).loescheResourceRecord(domainname, ResourceRecord.ADDRESS);
                    LOG.debug("GUIApplicationDNSServerWindow: A-Eintrag " + domainname + " geloescht");

                    updateARecordsTable();
                }
            }
        });
        hBox.add(buttonEntfernen);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        tabellenModell = new DefaultTableModel(0, 2);
        aRecordsTable = new DNSConfigTable(tabellenModell, true, "A");
        aRecordsTable.setParentGUI(this);
        aRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        aRecordsTable.setRowHeight(30);
        aRecordsTable.setShowGrid(false);
        aRecordsTable.setFillsViewportHeight(true);
        aRecordsTable.setBackground(Color.WHITE);
        aRecordsTable.setShowHorizontalLines(true);

        tcm = aRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg8"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg9"));
        scrollPane = new JScrollPane(aRecordsTable);

        vBox.add(scrollPane);
        aPanel.add(vBox, BorderLayout.CENTER);

        return aPanel;
    }

    private JPanel createMXPanel() {
        Box vBox, hBox;
        DefaultTableModel tabellenModell;
        TableColumnModel tcm;
        JScrollPane scrollPane;

        JPanel mxPanel = new JPanel(new BorderLayout());

        vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel mxMaildomainLabel = new JLabel(messages.getString("dnsserver_msg10"));
        mxMaildomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(mxMaildomainLabel);
        hBox.add(Box.createHorizontalStrut(5));

        mxMaildomainField = new JTextField();
        mxMaildomainField.setPreferredSize(new Dimension(275, 25));
        mxMaildomainField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkFQDN(mxMaildomainField);
            }
        });
        hBox.add(mxMaildomainField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel mxURLLabel = new JLabel(messages.getString("dnsserver_msg11"));
        mxURLLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(mxURLLabel);
        hBox.add(Box.createHorizontalStrut(5));

        mxURLField = new JTextField();
        mxURLField.setPreferredSize(new Dimension(275, 25));
        mxURLField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkFQDN(mxURLField);
            }
        });
        hBox.add(mxURLField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JButton mxAddButton = new JButton(messages.getString("dnsserver_msg6"));
        mxAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (checkFQDN(mxMaildomainField) && checkFQDN(mxURLField)) {
                    ((DNSServer) holeAnwendung()).hinzuRecord(mxMaildomainField.getText(), ResourceRecord.MAIL_EXCHANGE,
                            mxURLField.getText());
                    mxMaildomainField.setText("");
                    mxURLField.setText("");
                    updateMXRecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(messages.getString("dnsserver_msg23"));
                }
            }
        });
        hBox.add(mxAddButton);

        JButton buttonMXEntfernen = new JButton(messages.getString("dnsserver_msg7"));
        buttonMXEntfernen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int zeilenNummer = mxRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = mxRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer) holeAnwendung()).loescheResourceRecord(domainname, ResourceRecord.MAIL_EXCHANGE);
                    LOG.debug("GUIApplicationDNSServerWindow: MX-Eintrag " + domainname + " geloescht");
                    updateMXRecordsTable();
                }
            }
        });
        hBox.add(buttonMXEntfernen);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        tabellenModell = new DefaultTableModel(0, 2);
        mxRecordsTable = new DNSConfigTable(tabellenModell, true, "MX");
        mxRecordsTable.setParentGUI(this); // tell the table who presents its
                                           // values, such that the back-end DNS
                                           // server can be found for adapting
                                           // resource entries
        mxRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        mxRecordsTable.setRowHeight(30);
        mxRecordsTable.setShowGrid(false);
        mxRecordsTable.setFillsViewportHeight(true);
        mxRecordsTable.setBackground(Color.WHITE);
        mxRecordsTable.setShowHorizontalLines(true);

        tcm = mxRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg12"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg13"));

        scrollPane = new JScrollPane(mxRecordsTable);
        vBox.add(scrollPane);
        mxPanel.add(vBox, BorderLayout.CENTER);

        return mxPanel;
    }

    private JPanel createNSPanel() {
        Box vBox, hBox;
        DefaultTableModel tabellenModell;
        TableColumnModel tcm;
        JScrollPane scrollPane;

        JPanel nsPanel = new JPanel(new BorderLayout());

        vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel nsDomainLabel = new JLabel(messages.getString("dnsserver_msg16"));
        nsDomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(nsDomainLabel);
        hBox.add(Box.createHorizontalStrut(5));

        nsDomainField = new JTextField();
        nsDomainField.setPreferredSize(new Dimension(275, 25));
        nsDomainField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkFQDN(nsDomainField);
            }
        });
        hBox.add(nsDomainField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JLabel nsDomainServerLabel = new JLabel(messages.getString("dnsserver_msg17"));
        nsDomainServerLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(nsDomainServerLabel);
        hBox.add(Box.createHorizontalStrut(5));

        nsDomainServerField = new JTextField();
        nsDomainServerField.setPreferredSize(new Dimension(275, 25));
        nsDomainServerField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkFQDN(nsDomainServerField);
            }
        });
        hBox.add(nsDomainServerField);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        JButton nsAddButton = new JButton(messages.getString("dnsserver_msg6"));
        nsAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (checkFQDN(nsDomainField) && checkFQDN(nsDomainServerField)) {
                    ((DNSServer) holeAnwendung()).hinzuRecord(nsDomainField.getText(), ResourceRecord.NAME_SERVER,
                            nsDomainServerField.getText());
                    nsDomainField.setText("");
                    nsDomainServerField.setText("");
                    updateNSRecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(messages.getString("dnsserver_msg22"));
                }
            }
        });
        hBox.add(nsAddButton);

        JButton nsRemoveButton = new JButton(messages.getString("dnsserver_msg7"));
        nsRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int zeilenNummer = nsRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = nsRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer) holeAnwendung()).loescheResourceRecord(domainname, ResourceRecord.NAME_SERVER);
                    LOG.debug("GUIApplicationDNSServerWindow: NS-Eintrag " + domainname + " geloescht");
                    updateNSRecordsTable();
                }
            }
        });
        hBox.add(nsRemoveButton);

        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));

        tabellenModell = new DefaultTableModel(0, 2);
        nsRecordsTable = new DNSConfigTable(tabellenModell, true, "NS");
        nsRecordsTable.setParentGUI(this);
        nsRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        nsRecordsTable.setRowHeight(30);
        nsRecordsTable.setShowGrid(false);
        nsRecordsTable.setFillsViewportHeight(true);
        nsRecordsTable.setBackground(Color.WHITE);
        nsRecordsTable.setShowHorizontalLines(true);

        tcm = nsRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg18"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg19"));

        scrollPane = new JScrollPane(nsRecordsTable);
        vBox.add(scrollPane);
        nsPanel.add(vBox, BorderLayout.CENTER);
        return nsPanel;
    }

    public void updateARecordsTable() {
        updateRecordsTable(aRecordsTable);
    }

    public void updateNSRecordsTable() {
        updateRecordsTable(nsRecordsTable);
    }

    public void updateMXRecordsTable() {
        updateRecordsTable(mxRecordsTable);
    }

    private synchronized void updateRecordsTable(DNSConfigTable table) {
        String type = table.getType();

        List<ResourceRecord> tempListe = ((DNSServer) holeAnwendung()).holeResourceRecords();
        List<ResourceRecord> recordsOfSelectedType = new ArrayList<ResourceRecord>();
        for (ResourceRecord rr : tempListe) {
            if (rr.getType().equals(type)) {
                recordsOfSelectedType.add(rr);
            }
        }
        ((DefaultTableModel) table.getModel()).setRowCount(recordsOfSelectedType.size());
        int row = 0;
        for (ResourceRecord rr : recordsOfSelectedType) {
            table.setValueAt(rr.getDomainname(), row, 0);
            table.setValueAt(rr.getRdata(), row, 1);
            row++;
        }
    }

    /**
     * Funktion die während der Eingabe ueberprueft ob die bisherige Eingabe einen korrekten Wert darstellt.
     */
    private boolean checkIP(JTextField feld) {
        feld.setText(feld.getText().trim());
        boolean validAddress = EingabenUeberpruefung.isGueltig(feld.getText(), EingabenUeberpruefung.musterIpAdresse);
        if (validAddress) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
        }
        return validAddress;
    }

    private boolean checkFQDN(JTextField feld) {
        feld.setText(feld.getText().trim());
        boolean validFqdn = EingabenUeberpruefung.isGueltig(feld.getText(), EingabenUeberpruefung.musterDomain);
        if (validFqdn) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
        }
        return validFqdn;
    }

    private void aktualisieren() {
        if (((DNSServer) holeAnwendung()).isAktiv()) {
            buttonStart.setText(messages.getString("dnsserver_msg14"));
        } else {
            buttonStart.setText(messages.getString("dnsserver_msg1"));
        }
        updateARecordsTable();
        updateMXRecordsTable();
        updateNSRecordsTable();
    }

    public void update(Observable arg0, Object arg1) {
        if ("A".equals(arg1)) {
            updateARecordsTable();
        } else if ("NS".equals(arg1)) {
            updateNSRecordsTable();
        } else if ("MX".equals(arg1)) {
            updateMXRecordsTable();
        } else if (arg1 != null) {
            aktualisieren();
        }
    }
}
