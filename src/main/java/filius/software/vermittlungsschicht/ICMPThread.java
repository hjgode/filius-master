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
package filius.software.vermittlungsschicht;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.Verbindung;
import filius.software.ProtokollThread;
import filius.software.system.InternetKnotenBetriebssystem;

/**
 * Klasse zur Ueberwachung des Puffers fuer eingehende ICMP-Pakete
 * 
 */
public class ICMPThread extends ProtokollThread<IcmpPaket> {
    private static Logger LOG = LoggerFactory.getLogger(ICMPThread.class);

    private ICMP vermittlung;

    /** der von dem Thread zu ueberwachende Puffer */
    private LinkedList<IcmpPaket> rcvdPackets;

    public ICMPThread(ICMP vermittlung) {
        super(((InternetKnotenBetriebssystem) vermittlung.holeSystemSoftware()).holeEthernet().holeICMPPuffer());
        LOG.trace("INVOKED-2 (" + this.hashCode() + ", T" + this.getId() + ") " + getClass()
                + " (ICMPThread), constr: ICMPThread(" + vermittlung + ")");
        this.rcvdPackets = new LinkedList<IcmpPaket>();
        this.vermittlung = vermittlung;
    }

    /**
     * Methode zur Verarbeitung eingehender ICMP-Pakete <br />
     */
    protected void verarbeiteDatenEinheit(IcmpPaket icmpPaket) {
        if (vermittlung.isLocalAddress(icmpPaket.getEmpfaenger())
                || vermittlung.isNetworkLocalBroadcast(icmpPaket.getEmpfaenger(), icmpPaket.getSender())) {
            if (icmpPaket.isEchoRequest()) {
                vermittlung.sendEchoReply(icmpPaket);
            } else {
                addIcmpResponse(icmpPaket);
            }
        } else if (vermittlung.isBroadcast(icmpPaket.getEmpfaenger())) {
            LOG.info("discard icmp broadcast for another network ({})", icmpPaket);
        } else {
            vermittlung.weiterleitenPaket(icmpPaket);
        }
    }

    private void addIcmpResponse(IcmpPaket icmpPaket) {
        synchronized (rcvdPackets) {
            rcvdPackets.add(icmpPaket);
            rcvdPackets.notifyAll();
        }
    }

    public void resetIcmpResponseQueue() {
        synchronized (rcvdPackets) {
            rcvdPackets.clear();
            rcvdPackets.notifyAll();
        }
    }

    public IcmpPaket waitForIcmpResponse() {
        IcmpPaket response = null;
        synchronized (rcvdPackets) {
            try {
                rcvdPackets.wait(Verbindung.holeRTT());
            } catch (InterruptedException e) {}

            if (rcvdPackets.size() > 0) {
                response = rcvdPackets.removeFirst();
            }
        }
        return response;
    }
}
