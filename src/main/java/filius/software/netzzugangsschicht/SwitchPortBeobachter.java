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
package filius.software.netzzugangsschicht;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.Port;
import filius.hardware.knoten.Switch;
import filius.software.ProtokollThread;
import filius.software.system.SwitchFirmware;

/**
 * Der Switch-Portbeobachter dient dazu, eingehende Frames an einem Switch-Anschluss abzuholen und an den richtigen
 * Anschluss weiterzuleiten.
 */
public class SwitchPortBeobachter extends ProtokollThread<EthernetFrame> {
    private static Logger LOG = LoggerFactory.getLogger(SwitchPortBeobachter.class);

    /** Die Switch-Firmware */
    private SwitchFirmware switchFirmware;

    /** Der Switch-Anschluss, der ueberwacht wird */
    private Port anschluss;

    // private boolean threadRunning = false;

    /**
     * Der Konstruktor initialisiert den Eingangspuffer durch Aufruf des Konstruktors der Oberklasse und initialisert
     * die Switch-Firmware und den zu ueberwachenden Anschluss.
     */
    public SwitchPortBeobachter(SwitchFirmware switchFirmware, Port anschluss) {
        super(anschluss.holeEingangsPuffer());
        LOG.trace("INVOKED-2 (" + this.hashCode() + ", T" + this.getId() + ") " + getClass()
                + " (SwitchPortBeobachter), constr: SwitchPortBeobachter(" + switchFirmware + "," + anschluss + ")");

        this.switchFirmware = switchFirmware;
        this.anschluss = anschluss;
    }

    /**
     * Methode zur Weiterleitung der Frames zu dem richtigen Anschluss des Switch.
     * <ol>
     * <li>Dazu wird zunaechst geprueft, ob der Frame bereits weitergeleitet wurde. Wenn es eine Wiederholung ist, die
     * durch einen Zyklus im Netzwerk entstanden sein kann, wird der Frame verworfen.</li>
     * <li>Dann wird der Frame zu der von der Firmware verwalteten Liste der weitergeleiteten Frames hinzugefuegt.</li>
     * <li>Die SAT-Tabelle wird ergaenzt.</li>
     * <li>Der Frame wird an einen Anschluss weitergeleitet, wenn fuer die Ziel-MAC-Adresse ein Eintrag in der SAT
     * existiert.</li>
     * <li>Wenn kein SAT-Eintrag existiert wird der Frame als Broadcast an alle Anschluesse des Switch
     * weitergeleitet.</li>
     * </ol>
     */
    public void verarbeiteDatenEinheit(EthernetFrame etp) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + getClass()
                + " (SwitchPortBeobachter), verarbeiteDatenEinheit(" + etp.toString() + ")");

        if (!switchFirmware.holeDurchgelaufeneFrames().contains(etp)) {
            switchFirmware.holeDurchgelaufeneFrames().add(etp);
            Date letztes_update = new Date();
            switchFirmware.hinzuSatEintrag(etp.getQuellMacAdresse(), anschluss, letztes_update);

            Port zielAnschluss = switchFirmware.holeAnschlussFuerMAC(etp.getZielMacAdresse());
            if (zielAnschluss != null) {
                synchronized (zielAnschluss.holeAusgangsPuffer()) {
                    zielAnschluss.holeAusgangsPuffer().add(etp);
                    zielAnschluss.holeAusgangsPuffer().notify();
                }
            } else {
                for (Port aktiverAnschluss : ((Switch) switchFirmware.getKnoten()).getAnschluesse()) {
                    if (!aktiverAnschluss.isPortFrei() && (aktiverAnschluss != anschluss)) {
                        synchronized (aktiverAnschluss.holeAusgangsPuffer()) {
                            aktiverAnschluss.holeAusgangsPuffer().add(etp);
                            aktiverAnschluss.holeAusgangsPuffer().notify();
                        }
                    }
                }
            }
        }
    }
}