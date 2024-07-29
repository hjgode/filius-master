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
package filius.software.transportschicht;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.exception.ServerSocketException;
import filius.exception.TimeOutException;
import filius.exception.VerbindungsException;
import filius.rahmenprogramm.I18n;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.vermittlungsschicht.IpPaket;

/**
 * Der ServerSocket wird von Server-Anwendungen genutzt, die eine TCP-Verbindung fuer den Datenaustausch nutzen. Der
 * Server-Socket verwaltet dazu eine Liste mit TCP-Sockets, die im Passiv-Modus gestartet werden. D. h., dass ein
 * solcher Socket auf eingehende Verbindungsanfragen wartet. <br />
 * Der Server-Socket leitet ankommende Segmente an Hand von entfernter IP-Adresse und entferntem TCP-Port an den
 * richtigen lokalen TCP-Socket weiter.
 * 
 * @author carsten
 */
public class ServerSocket implements SocketSchnittstelle, I18n {
    private static Logger LOG = LoggerFactory.getLogger(ServerSocket.class);

    /**
     * Tabelle zur Verwaltung der TCP-Sockets. Der Key in der Tabelle besteht aus einem String, der durch zusammensetzen
     * von Sender-IP-Adresse und Sender-TCP-Port besteht (Bsp.: 192.168.0.1:1100). Als Value ist der Socket abgelegt.
     */
    private Hashtable<String, Socket> socketListe = new Hashtable<String, Socket>();

    /**
     * Ein aktuell erstellter Socket, der auf eine eingehende Verbindungsanfrage wartet. Dieser Socket ist noch nicht in
     * die Socketliste eingetragen, weil die IP-Adresse und der TCP-Port des entfernten Sockets vor Eintreffen der
     * Anfrage fuer einen Verbindungsaufbau noch nicht bekannt sind.
     */
    private Socket aktuellerSocket;

    /** Das Betriebssystem */
    private InternetKnotenBetriebssystem betriebssystem;

    /** Das Transport Control Protocol (TCP) oder User Datagram Protocol (UDP) */
    private TransportProtokoll protokoll;

    /**
     * der lokale TCP-Port, an dem Verbindungsanfragen durch diesen Socket angenommen werden.
     */
    private int lokalerPort;

    /**
     * Konstruktor fuer einen Server-Socket. Hier werden die lokalen Attribute initialisiert. Ausserdem wird der lokale
     * TCP-Port fuer diesen Socket reserviert. Wenn das nicht moeglich ist, weil er schon belegt ist, wird eine
     * Exception ausgeloest.
     * 
     * @author carsten
     * @param betriebssystem
     * @param lokalerPort
     *            - Lokaler Port, auf dem der Server laufen soll. Bsp.: http-Anwendungen zumeist auf Port 80
     */
    public ServerSocket(InternetKnotenBetriebssystem betriebssystem, int lokalerPort, int transportProtokoll) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (ServerSocket), constr: ServerSocket("
                + betriebssystem + "," + lokalerPort + "," + transportProtokoll + ")");
        this.betriebssystem = betriebssystem;
        this.lokalerPort = lokalerPort;

        if (transportProtokoll == IpPaket.TCP) {
            protokoll = betriebssystem.holeTcp();
        } else {
            protokoll = betriebssystem.holeUdp();
        }
    }

    public int getLocalPort() {
        return this.lokalerPort;
    }

    public Hashtable<String, Socket> getSockets() {
        return socketListe;
    }

    /**
     * Methode zum Eintragen eines neuen Sockets in die Socket-Liste
     */
    public void eintragenSocket(Socket socket) {
        String ziel = socket.holeZielIPAdresse() + ":" + socket.holeZielPort();
        socketListe.put(ziel, socket);
        LOG.debug("[port={}] socket for remote client {} added.", lokalerPort, ziel);
    }

    /**
     * Methode zum Austragen eines geschlossenen Sockets aus der Liste der Sockets. Wenn der letzte Socket ausgetragen
     * wurde und kein Socket auf einen Verbindungsaufbau wartet, dann ist der Server-Socket damit beendet. Der Port wird
     * dann freigegeben.
     */
    public void austragenSocket(Socket socket) {
        String ziel = socket.holeZielIPAdresse() + ":" + socket.holeZielPort();
        socketListe.remove(ziel);
        LOG.debug("[port={}] socket for remote client {} removed.", lokalerPort, ziel);

        if (socketListe.isEmpty() && aktuellerSocket == null) {
            protokoll.gibPortFrei(lokalerPort);
        }
    }

    /**
     * Mit dieser Methode wird ein neuer 'lauschender' TCP-Socket erzeugt und zurueck gegeben. Diese Methode
     * <b>blockiert</b> den Thread, bis eine Verbindung zu dem Socket aufgebaut wurde! Der Eintrag in die Socketliste
     * wird vom TCP-Socket nach erfolgreichem Verbindungsaufbau initiiert.
     * 
     * @throws ServerSocketException
     *             - Diese Exception wird geworfen, wenn auf dem angeforderten Port schon eine Anwendung laeuft.
     */
    public synchronized Socket oeffnen() throws VerbindungsException, ServerSocketException {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (ServerSocket), oeffnen()");
        // Falls schon eine Anwendung auf dem vorgeschlagenen Port laeuft
        // wird eine Exception ausgeloest
        if (!protokoll.reservierePort(lokalerPort, this)) {
            throw new ServerSocketException(messages.getString("sw_serversocket_msg1") + " " + lokalerPort + " "
                    + messages.getString("sw_serversocket_msg2"));
        }

        if (protokoll instanceof TCP) {
            aktuellerSocket = new TCPSocket(betriebssystem, lokalerPort);
        } else {
            aktuellerSocket = new UDPSocket(betriebssystem, lokalerPort);
        }

        try {
            aktuellerSocket.verbinden();
        } catch (TimeOutException e) {
            aktuellerSocket = null;
            LOG.debug("[port={}] unexpected timeout exception while listening for incoming connection.", lokalerPort,
                    e);
        }

        if (aktuellerSocket != null && aktuellerSocket.istVerbunden()) {
            return aktuellerSocket;
        } else {
            return null;
        }
    }

    /**
     * Ankommende Segmente werden von dieser Methode an den richtigen Socket weitergegeben. Wenn kein Scoket aus der
     * Socketliste zur entfernten IP-Adresse und dem entfernten TCP-Port passt, wird das Segment an den
     * 'aktuellerSocket' weitergegeben. Dieser Socket wurde gerade erst erzeugt und wartet auf eingehende
     * Verbindungsanfragen.
     */
    public void hinzufuegen(String startIp, int startPort, Object segment) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (ServerSocket), hinzufuegen(" + startIp + ","
                + startPort + "," + segment + ")");
        String start;
        Socket socket;

        start = startIp + ":" + startPort;
        if (socketListe.containsKey(start)) {
            socket = (Socket) socketListe.get(start);
            socket.hinzufuegen(startIp, startPort, segment);
        } else if (aktuellerSocket != null) {
            aktuellerSocket.hinzufuegen(startIp, startPort, segment);
        }
    }

    /**
     * Methode zum Schliessen des Server-Sockets. Das heisst, dass keine Verbindungsanfragen mehr angenommen werden. Die
     * von dem Server-Socket verwalteten Sockets werden dadurch <b>nicht</b> geschlossen! <br />
     * Der reservierte Port wird deshalb auch erst dann freigegeben, wenn der letzte der verwalteten Sockets geschlossen
     * wurde. <br />
     * Diese Methode blockiert nicht, weil der aktuelle Socket immer ein Socket ist, der sich im Zustand LISTEN
     * befindet. Wenn ein Socket in diesem Zustand geschlossen wird, blockiert dieser nicht!
     */
    public void schliessen() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (ServerSocket), schliessen()");

        if (aktuellerSocket != null) {
            aktuellerSocket.schliessen();
            protokoll.gibPortFrei(lokalerPort);
        }
    }

    /**
     * Diese Methode wird beim Wechsel vom Aktions- zum Entwurfsmodus aufgerufen, damit moeglicherweise blockierte
     * Threads beendet werden koennen. <br />
     * Diese Methode ist <b>nicht blockierend</b>!
     */
    public void beenden() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (ServerSocket), beenden()");
        if (aktuellerSocket != null)
            aktuellerSocket.beenden();
    }
}
