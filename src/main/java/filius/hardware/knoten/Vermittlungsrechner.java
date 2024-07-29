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
//Netzwerkziel, Netzwerkmaske, ZielIp, Schnittstelle
package filius.hardware.knoten;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.rahmenprogramm.I18n;
import filius.software.system.VermittlungsrechnerBetriebssystem;

@SuppressWarnings("serial")
public class Vermittlungsrechner extends InternetKnoten implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(Vermittlungsrechner.class);

    public static final String TYPE = messages.getString("hw_vermittlungsrechner_msg1");

    @Override
    public String holeHardwareTyp() {
        return TYPE;
    }

    public Vermittlungsrechner() {
        super();
        LOG.trace("INVOKED-2 (" + this.hashCode() + ") " + getClass()
                + " (Vermittlungsrechner), constr: Vermittlungsrechner()");

        this.setzeAnzahlAnschluesse(1);
        this.setSystemSoftware(new VermittlungsrechnerBetriebssystem());
        getSystemSoftware().setKnoten(this);
        this.setName(TYPE);
    }

    public List<String> getMacs() {
        List<String> macs = new ArrayList<String>();
        int count = getNetzwerkInterfaces().size();
        for (int i = 0; i < count; i++)
            macs.add(getNetzwerkInterfaces().get(0).getMac());
        return macs;
    }
}
