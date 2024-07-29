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
package filius.hardware;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.netzzugangsschicht.EthernetFrame;

public class SimplexVerbindung implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(SimplexVerbindung.class);

    private boolean threadRunning = true;
    private Verbindung verbindung = null;
    private Port anschluss1 = null;
    private Port anschluss2 = null;

    /**
     * @author carsten
     * @param sender
     *            - Sender der einseitigen Verbindung
     * @param empfaenger
     *            - Empfaenger der einseitigen Verbindung
     * @param verbindung
     *            - Verbindung, auf der die einseitige Kommunikation gestartet wird
     * 
     *            Dieser Konstruktor wird innerhalb der Verbindung aufgerufen und in einem Thread gestartet. Davon gibt
     *            es zwei Verbindungen, die die bidirektionale Verbindung zwischen den beiden Hardwares herstellt.
     */
    public SimplexVerbindung(Port anschluss1, Port anschluss2, Verbindung verbindung) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (SimplexVerbindung), constr: SimplexVerbindung("
                + anschluss1 + "," + anschluss2 + "," + verbindung + ")");
        this.anschluss1 = anschluss1;
        this.anschluss2 = anschluss2;
        this.verbindung = verbindung;
    }

    /**
     * @author carsten Diese run-Methode des Threads (nur Runnable!) sorgt fuer die einzelnen Kommunikationen auf einer
     *         Verbindung in beide Richtungen
     */
    public void run() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (SimplexVerbindung), run()");
        EthernetFrame frame;

        while (threadRunning) {
            synchronized (anschluss1.holeAusgangsPuffer()) {
                if (anschluss1.holeAusgangsPuffer().size() < 1) {
                    try {
                        verbindung.setAktiv(false);
                        anschluss1.holeAusgangsPuffer().wait();
                    } catch (InterruptedException e) {}
                }
                if (anschluss1.holeAusgangsPuffer().size() > 0) {
                    frame = (EthernetFrame) anschluss1.holeAusgangsPuffer().removeFirst();
                    boolean dropFrame = Verbindung.isDrop();
                    if (dropFrame) {
                        verbindung.setFailure();
                    } else {
                        verbindung.setAktiv(true);
                    }

                    synchronized (this) {
                        try {
                            Thread.sleep(Verbindung.holeVerzoegerung());
                        } catch (InterruptedException e) {}
                    }
                    if (!dropFrame) {
                        synchronized (anschluss2.holeEingangsPuffer()) {
                            anschluss2.holeEingangsPuffer().add(SerializationUtils.clone(frame));
                            anschluss2.holeEingangsPuffer().notify();
                        }
                    } else {
                        Lauscher.getLauscher().addDroppedDataUnit(frame);
                        LOG.debug("Frame dropped.");
                    }
                }
            }
        }
    }

    public Port getPort1() {
        return anschluss1;
    }

    public void setPort1(Port anschluss1) {
        this.anschluss1 = anschluss1;
    }

    public Port getPort2() {
        return anschluss2;
    }

    public void setPort2(Port anschluss2) {
        this.anschluss2 = anschluss2;
    }

    public void anschluesseTrennen() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (SimplexVerbindung), anschluesseTrennen()");
        anschluss1.entferneVerbindung();
        anschluss2.entferneVerbindung();
        this.setThreadRunning(false);
    }

    public Verbindung getVerbindung() {
        return verbindung;
    }

    public void setVerbindung(Verbindung verbindung) {
        this.verbindung = verbindung;
    }

    public boolean isThreadRunning() {
        return threadRunning;
    }

    public void setThreadRunning(boolean threadRunning) {
        this.threadRunning = threadRunning;
    }

}
