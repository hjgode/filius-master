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

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;

import filius.gui.JMainFrame;
import filius.hardware.knoten.Switch;
import filius.rahmenprogramm.I18n;
import filius.software.system.SwitchFirmware;

@SuppressWarnings("serial")
public class SatViewer extends JDialog implements I18n, PropertyChangeListener {

    private Switch sw;
    private DefaultTableModel dtm;

    public SatViewer(Switch sw) {
        super(JMainFrame.getJMainFrame(), messages.getString("guievents_msg8") + " " + sw.holeAnzeigeName());
        this.sw = sw;
        init();
        updateSat();
    }

    private void init() {
        setBounds(100, 100, 400, 280);

        ImageIcon icon = new ImageIcon(getClass().getResource("/gfx/hardware/switch.png"));
        setIconImage(icon.getImage());

        dtm = new DefaultTableModel(0, 3);
        JTable tableSATNachrichten = new JTable(dtm);
        DefaultTableColumnModel dtcm = (DefaultTableColumnModel) tableSATNachrichten.getColumnModel();
        dtcm.getColumn(0).setHeaderValue(messages.getString("guievents_msg9"));
        dtcm.getColumn(0).setPreferredWidth(150);
        dtcm.getColumn(1).setHeaderValue(messages.getString("guievents_msg10"));
        dtcm.getColumn(1).setPreferredWidth(100);
        dtcm.getColumn(2).setHeaderValue(messages.getString("guievents_msg27"));
        dtcm.getColumn(2).setPreferredWidth(150);

        JPopupMenu menu = new JPopupMenu();

        JMenuItem resetMenuItem = new JMenuItem(messages.getString("guievents_msg28"));
        resetMenuItem.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                menu.setVisible(false);
                ((SwitchFirmware) sw.getSystemSoftware()).loescheSAT();
            }
        });
        menu.add(resetMenuItem);

        tableSATNachrichten.setComponentPopupMenu(menu);

        JScrollPane spSAT = new JScrollPane(tableSATNachrichten);
        getContentPane().add(spSAT);
    }

    public Switch getSwitch() {
        return sw;
    }

    private void updateSat() {
        dtm.setRowCount(0);
        for (Vector<String> zeile : ((SwitchFirmware) sw.getSystemSoftware()).holeSAT()) {
            dtm.addRow(zeile);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == "sat_entry") {
            updateSat();
        }
    }
}
