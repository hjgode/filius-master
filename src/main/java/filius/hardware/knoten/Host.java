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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.NetzwerkInterface;
import filius.software.system.Betriebssystem;

@SuppressWarnings("serial")
public abstract class Host extends InternetKnoten {
    private static Logger LOG = LoggerFactory.getLogger(Host.class);

    private boolean useIPAsName = false;

    private boolean useMACAsName = false;

    public boolean isUseIPAsName() {
        return useIPAsName;
    }

    public void setUseIPAsName(boolean useIPAsName) {
        this.useIPAsName = useIPAsName;
    }

    public boolean isUseMACAsName() {
        return useMACAsName;
    }

    public void setUseMACAsName(boolean useMACAsName) {
        this.useMACAsName = useMACAsName;
    }

    public Host() {
        super();
        LOG.trace("INVOKED-2 (" + this.hashCode() + ") " + getClass() + " (Host), constr: Host()");

        this.setzeAnzahlAnschluesse(1);
        this.setSystemSoftware(new Betriebssystem());
        getSystemSoftware().setKnoten(this);
        LOG.debug("DEBUG:  Host " + this.hashCode() + " has OS " + getSystemSoftware().hashCode());
    }

    @Override
    public String holeAnzeigeName() {
        if (useIPAsName && useMACAsName) {
            return getNetzwerkInterfaces().get(0).getIp() + " (" + getNetzwerkInterfaces().get(0).getMac() + ")";
        } else if (useIPAsName) {
            return getNetzwerkInterfaces().get(0).getIp();
        } else if (useMACAsName) {
            return getNetzwerkInterfaces().get(0).getMac();
        } else {
            return getName();
        }
    }

    public void setIpAdresse(String ip) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + " (Host), setIpAdresse(" + ip + ")");
        NetzwerkInterface nic = (NetzwerkInterface) this.getNetzwerkInterfaces().get(0);
        nic.setIp(ip);
    }

    public String getMac() {
        NetzwerkInterface nic = (NetzwerkInterface) this.getNetzwerkInterfaces().get(0);
        return nic.getMac();
    }
}
