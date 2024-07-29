/*
 ** This file is part of Filius, a network construction and simulation software.
 ** 
 ** Originally created at the University of Siegen, Institute "Didactics of Informatics and E-Learning" by a students'
 * project group: members (2006-2007): André Asschoff, Johannes Bade, Carsten Dittich, Thomas Gerding, Nadja Haßler,
 * Ernst Johannes Klebert, Michell Weyer supervisors: Stefan Freischlad (maintainer until 2009), Peer Stechert Project
 * is maintained since 2010 by Christian Eibl <filius@c.fameibl.de> and Stefan Freischlad Filius is free software: you
 * can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 2 of the License, or (at your option) version 3.
 ** 
 ** Filius is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 ** 
 ** You should have received a copy of the GNU General Public License along with Filius. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package filius.software.vermittlungsschicht;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.NetzwerkInterface;
import filius.software.ProtokollThread;
import filius.software.system.InternetKnotenBetriebssystem;

/**
 * Klasse zur Ueberwachung des Puffers fuer eingehende ARP-Pakete
 * 
 */
public class ARPThread extends ProtokollThread<ArpPaket> {
    private static Logger LOG = LoggerFactory.getLogger(ARPThread.class);

    /**
     * die Implementierung des Address Resolution Protocols mit der Verwaltung der ARP-Eintraege
     */
    private ARP vermittlung;

    /**
     * Konstruktor zur Initialisierung des zu ueberwachenden Puffers und der ARP-Implementierung
     */
    public ARPThread(ARP vermittlung) {
        super(((InternetKnotenBetriebssystem) vermittlung.holeSystemSoftware()).holeEthernet().holeARPPuffer());
        LOG.trace("INVOKED-2 (" + this.hashCode() + ", T" + this.getId() + ") " + getClass()
                + " (ARPThread), constr: ARPThread(" + vermittlung + ")");
        this.vermittlung = vermittlung;
    }

    /**
     * Methode zur Verarbeitung eingehender ARP-Pakete <br />
     * Aus jedem ARP-Paket wird ein neuer Eintrag fuer die ARP-Tabelle erzeugt (unabhaengig davon, ob es eine Anfrage
     * oder eine Antwort ist). Wenn die Anfrage eine eigene IP-Adresse betrifft, wird ein Antwort-Paket verschickt.
     */
    protected void verarbeiteDatenEinheit(ArpPaket arpPaket) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + getClass()
                + " (ARPThread), verarbeiteDatenEinheit(" + arpPaket.toString() + ")");

        InternetKnotenBetriebssystem bs = (InternetKnotenBetriebssystem) vermittlung.holeSystemSoftware();
        NetzwerkInterface nic = vermittlung.getBroadcastNic(arpPaket.getSenderIP());
        if (nic != null && !VermittlungsProtokoll.getSubnetForIp(nic.getIp(), nic.getSubnetzMaske())
                .equals(arpPaket.getSenderIP())) {
            // Aus jedem ARP-Paket, mit dem eine eigene IP-Adresse abgefragt wird oder das eine Antwort ist, wird ein
            // neuer
            // ARP-Eintrag erzeugt
            if (arpPaket.getTargetIP().equals(nic.getIp()) || arpPaket.getOperation() == ArpPaket.REPLY
                    && !arpPaket.getSenderIP().equalsIgnoreCase("0.0.0.0")) {
                LOG.debug("ARP data received, will insert data for sender: {}", arpPaket);
                vermittlung.hinzuARPTabellenEintrag(arpPaket.getSenderIP(), arpPaket.getSenderMAC());
            }

            // wenn die Anfrage eine Anfrage fuer eine eigene
            // IP-Adresse ist, wird eine Antwort verschickt
            if (arpPaket.getTargetMAC().equalsIgnoreCase("ff:ff:ff:ff:ff:ff")
                    && arpPaket.getTargetIP().equalsIgnoreCase(nic.getIp())) {
                bs.holeARP().sendArpReply(nic.getMac(), nic.getIp(), arpPaket.getSenderMAC(), arpPaket.getSenderIP());
            }
        }
    }
}
