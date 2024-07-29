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
package filius.hardware.knoten;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.Hardware;
import filius.hardware.Port;
import filius.software.system.SystemSoftware;

@SuppressWarnings("serial")
public abstract class Knoten extends Hardware {
    private static Logger LOG = LoggerFactory.getLogger(Knoten.class);

    private String name;
    private SystemSoftware systemAnwendung;

    public abstract Port holeFreienPort();

    /**
     * Check whether this given port is one of the node's ports.
     */
    protected abstract boolean hasPort(Port portToLookup);

    protected abstract List<Port> defineConnectedPorts();

    public boolean checkConnected(Knoten knoten) {
        boolean alreadyConnected = false;
        for (Port port : defineConnectedPorts()) {
            if (knoten.hasPort(port)) {
                alreadyConnected = true;
                break;
            }
        }
        return alreadyConnected;
    }

    public String holeAnzeigeName() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (systemAnwendung != null) {
            LOG.debug(
                    "DEBUG: node with SystemSoftware (" + systemAnwendung.hashCode() + ") now has name '" + name + "'");
        }
    }

    public SystemSoftware getSystemSoftware() {
        return systemAnwendung;
    }

    public void setSystemSoftware(SystemSoftware systemAnwendung) {
        this.systemAnwendung = systemAnwendung;
    }
}
