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
package filius.rahmenprogramm;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.semantic.Version;
import de.skuzzle.semantic.Version.VersionFormatException;
import filius.gui.GUIErrorHandler;
import filius.gui.netzwerksicht.GUIDocuItem;
import filius.gui.netzwerksicht.GUIKabelItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.gui.netzwerksicht.JSidebarButton;

public class SzenarioVerwaltung extends Observable implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(SzenarioVerwaltung.class);

    private boolean geaendert = false;
    private String pfad = null;
    private static SzenarioVerwaltung verwaltung = null;

    private SzenarioVerwaltung() {}

    public static SzenarioVerwaltung getInstance() {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, getInstance()");
        if (verwaltung == null) {
            verwaltung = new SzenarioVerwaltung();
        }
        return verwaltung;
    }

    public void reset() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + ", reset()");
        pfad = null;
        geaendert = false;

        setChanged();
        notifyObservers();
    }

    public void setzeGeaendert() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + ", setzeGeaendert()");
        geaendert = true;

        this.setChanged();
        this.notifyObservers();
    }

    public boolean istGeaendert() {
        return geaendert;
    }

    public String holePfad() {
        return pfad;
    }

    /**
     * Speichern: - der Netzwerkknoten (inkl. Betriebssystem, Anwendungen - auch eigene/erweiterte - und
     * Konfigurationen) - der Verbindungen - der Quelldateien und des Bytecodes von selbst erstellten Anwendungen
     * 
     * Loesungsstrategie: - generell einen eigenen ClassLoader verwenden - XML-Datei fuer Objekte und Dateien aus dem
     * Ordner Anwendungen in einem leeren temporaeren Ordner speichern und daraus ein neues ZIP-Archiv erstellen, dass
     * an beliebigem Ort gespeichert werden kann
     */
    public boolean speichern(String datei, List<GUIKnotenItem> hardwareItems, List<GUIKabelItem> kabelItems,
            List<GUIDocuItem> docuItems) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + ", speichern(" + datei + "," + hardwareItems + ","
                + kabelItems + ")");

        String tmpDir;
        boolean erfolg = true;

        tmpDir = Information.getInformation().getTempPfad() + "projekt" + System.getProperty("file.separator");
        (new File(tmpDir)).mkdirs();

        if (!kopiereVerzeichnis(Information.getInformation().getAnwendungenPfad(), tmpDir + "anwendungen")) {
            LOG.debug("ERROR (" + this.hashCode() + "): Speicherung der eigenen Anwendungen fehlgeschlagen!");
            erfolg = false;
        }

        if (!netzwerkSpeichern(tmpDir + "konfiguration.xml", hardwareItems, kabelItems, docuItems)) {
            LOG.debug("ERROR (" + this.hashCode() + "): Speicherung des Netzwerks fehlgeschlagen!");
            erfolg = false;
        }

        if (!erzeugeZipArchiv(tmpDir, datei)) {
            LOG.debug("ERROR (" + this.hashCode() + "): Speicherung der Projektdatei fehlgeschlagen!");
            erfolg = false;
        }

        if (erfolg) {
            pfad = datei;
            geaendert = false;

            this.setChanged();
            this.notifyObservers();
        }

        loescheDateien(tmpDir);

        return erfolg;
    }

    private static boolean netzwerkSpeichern(String datei, List<GUIKnotenItem> hardwareItems,
            List<GUIKabelItem> kabelItems, List<GUIDocuItem> docuItems) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, netzwerkSpeichern(" + datei + ","
                + hardwareItems + "," + kabelItems + ")");
        XMLEncoder mx = null;
        FileOutputStream fos = null;

        if (Thread.currentThread().getContextClassLoader() != FiliusClassLoader
                .getInstance(Thread.currentThread().getContextClassLoader()))
            Thread.currentThread().setContextClassLoader(
                    FiliusClassLoader.getInstance(Thread.currentThread().getContextClassLoader()));

        try {
            fos = new FileOutputStream(datei);
            mx = new XMLEncoder(new BufferedOutputStream(fos));
            mx.setExceptionListener(new ExceptionListener() {
                public void exceptionThrown(Exception e) {
                    LOG.trace("Error in XML Encoder fetched.", e);
                }
            });

            mx.writeObject(new String("Filius version: " + filius.rahmenprogramm.Information.getVersion()));
            mx.writeObject(hardwareItems);
            mx.writeObject(kabelItems);
            mx.writeObject(docuItems);

            return true;
        } catch (RuntimeException e) {
            LOG.debug("EXCEPTION: java.lang.RuntimeException raised; Java internal problem, not Filius related!");
            return false;
        } catch (FileNotFoundException e2) {
            LOG.debug("File to store could not be found.", e2);
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (mx != null)
                mx.close();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
        }
    }

    public boolean laden(String datei, List<GUIKnotenItem> hardwareItems, List<GUIKabelItem> kabelItems,
            List<GUIDocuItem> docuItems) throws FileNotFoundException {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + getClass() + ", laden(" + datei + "," + hardwareItems + ","
                + kabelItems + ")");

        boolean erfolg = true;
        String tmpDir;

        // LOG.debug("SzenarioVerwaltung: Laden des Projekts aus Datei "+datei);
        tmpDir = Information.getInformation().getTempPfad();

        loescheDateien(tmpDir + "projekt");

        if (erfolg && !loescheVerzeichnisInhalt(Information.getInformation().getAnwendungenPfad())) {
            LOG.debug("ERROR (" + this.hashCode() + "): Loeschen vorhandener Anwendungen fehlgeschlagen");
        }

        if (!entpackeZipArchiv(datei, tmpDir)) {
            LOG.debug("ERROR (" + this.hashCode() + "): Entpacken des Zip-Archivs fehlgeschlagen");
            erfolg = false;
        }

        if (erfolg && !kopiereVerzeichnis(tmpDir + "projekt/anwendungen",
                Information.getInformation().getAnwendungenPfad())) {
            LOG.debug("ERROR (" + this.hashCode() + "): Kopieren der Anwendungen fehlgeschlagen");
        }

        if (erfolg && !netzwerkLaden(tmpDir + "projekt/konfiguration.xml", hardwareItems, kabelItems, docuItems)) {
            LOG.debug("ERROR (" + this.hashCode() + "): Laden der Netzwerkkonfiguration fehlgeschlagen");
            erfolg = false;
        }

        if (erfolg) {
            pfad = datei;
            geaendert = false;

            this.setChanged();
            this.notifyObservers();
        }

        return erfolg;
    }

    private static boolean netzwerkLaden(String datei, List<GUIKnotenItem> hardwareItems, List<GUIKabelItem> kabelItems,
            List<GUIDocuItem> docuItems) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, netzwerkLaden(" + datei + ","
                + hardwareItems + "," + kabelItems + ")");
        Object tmpObject = null;

        if (Thread.currentThread().getContextClassLoader() != FiliusClassLoader
                .getInstance(Thread.currentThread().getContextClassLoader()))
            Thread.currentThread().setContextClassLoader(
                    FiliusClassLoader.getInstance(Thread.currentThread().getContextClassLoader()));

        boolean success = false;
        try (XMLDecoder xmldec = new XMLDecoder(new BufferedInputStream(new FileInputStream(datei)))) {
            xmldec.setExceptionListener(new ExceptionListener() {
                public void exceptionThrown(Exception e) {
                    LOG.debug("", e);
                }
            });

            Information.getInformation().reset();
            tmpObject = xmldec.readObject();

            // in newer versions of Filius the version information is put into
            // the saved file as well
            // WARNING: former versions expect LinkedList as first element in
            // the saved file!
            if (tmpObject instanceof String) {
                String versionInfo = (String) tmpObject;
                int startIdx = versionInfo.indexOf(":") + 2;
                String versionString = versionInfo.substring(startIdx, versionInfo.indexOf(" ", startIdx));
                LOG.debug("File saved by Filius in version '" + versionString + "'");
                Version fileVersion = null;
                try {
                    fileVersion = Version.parseVersion(versionString.trim());
                } catch (VersionFormatException e) {
                    LOG.debug("Version string of project file is not a valid semantic version.");
                }
                Version programVersion = Version
                        .parseVersion(Information.getVersion().substring(0, versionInfo.indexOf(" ")).trim());

                if (fileVersion == null) {
                    LOG.debug(
                            "WARNING: version of scenario file could not be evaluated. Certain elements might not be rendered correctly any more!");
                } else if (fileVersion.compareTo(programVersion) < 0) {
                    LOG.debug("WARNING: current Filius version is newer ("
                            + filius.rahmenprogramm.Information.getVersion()
                            + ") than version of scenario file, such that certain elements might not be rendered correctly any more!");
                } else if (fileVersion.compareTo(programVersion) > 0) {
                    LOG.debug("WARNING: current Filius version is older ("
                            + filius.rahmenprogramm.Information.getVersion()
                            + ") than version of scenario file, such that certain elements might not be rendered correctly!");
                } else {
                    LOG.debug("\t...good, current version of Filius is equal to version of scenario file");
                }
                tmpObject = null;
            } else {
                LOG.debug("WARNING: Version information of Filius scenario file could not be determined!");
                LOG.debug(
                        "WARNING: This usually means, the scenario file was created with Filius before version 1.3.0.");
                LOG.debug("WARNING: Certain elements might not be rendered correctly any more!");
            }

            hardwareItems.clear();
            kabelItems.clear();
            docuItems.clear();

            if (tmpObject == null) {
                tmpObject = xmldec.readObject();
            }
            if (tmpObject instanceof List && !((List) tmpObject).isEmpty()
                    && ((List) tmpObject).get(0) instanceof GUIKnotenItem) {
                List<GUIKnotenItem> tempList = (List<GUIKnotenItem>) tmpObject;
                for (GUIKnotenItem tmpNode : tempList) {
                    if (tmpNode.getImageLabel() == null) {
                        JSidebarButton imageLabel = new JSidebarButton();
                        tmpNode.setImageLabel(imageLabel);
                    }
                    tmpNode.getImageLabel().setTyp(tmpNode.getKnoten().holeHardwareTyp());
                    hardwareItems.add(tmpNode);
                }
            }

            tmpObject = xmldec.readObject();
            if (tmpObject instanceof List && !((List<?>) tmpObject).isEmpty()
                    && ((List<?>) tmpObject).get(0) instanceof GUIKabelItem) {
                List<GUIKabelItem> tempList = (List<GUIKabelItem>) tmpObject;
                for (GUIKabelItem cable : tempList) {
                    kabelItems.add(cable);
                }
            }

            tmpObject = xmldec.readObject();
            if (tmpObject instanceof List && !((List) tmpObject).isEmpty()
                    && ((List) tmpObject).get(0) instanceof GUIDocuItem) {
                List<GUIDocuItem> tempList = (List<GUIDocuItem>) tmpObject;
                for (GUIDocuItem docuItem : tempList) {
                    docuItems.add(docuItem);
                }
            }
            success = true;
        } catch (FileNotFoundException e) {
            GUIErrorHandler.getGUIErrorHandler().DisplayError(messages.getString("rp_szenarioverwaltung_msg5"));
            LOG.debug("", e);
            success = false;
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.debug("Incomplete project file " + datei);
            success = true;
        }
        return success;
    }

    public static boolean erzeugeZipArchiv(String datenOrdner, String archivDatei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, erzeugeZipArchiv(" + datenOrdner + ","
                + archivDatei + ")");
        FileOutputStream out;
        ZipOutputStream zipOut;
        File zipDatei;
        File ordner;

        zipDatei = new File(archivDatei);
        new File(zipDatei.getParent()).mkdirs();

        ordner = new File(datenOrdner);
        if (!ordner.exists())
            return false;

        try {
            zipDatei.createNewFile();
        } catch (IOException e) {
            LOG.debug("", e);
            return false;
        }

        try {
            out = new FileOutputStream(zipDatei);
            zipOut = new ZipOutputStream(out);
            schreibeZipDatei(zipOut, ordner.getName() + "/", ordner.getAbsolutePath());
            try {
                zipOut.close();
                out.close();
            } catch (IOException e) {
                LOG.debug("", e);
                return false;
            }
        } catch (FileNotFoundException e) {
            LOG.debug("", e);
            return false;
        }

        return true;
    }

    private static boolean schreibeZipDatei(ZipOutputStream out, String relPfad, String datei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, schreibeZipDatei(" + out + "," + relPfad
                + "," + datei + ")");
        File path;
        boolean result = true;

        path = new File(datei);
        if (path.isFile()) {
            return schreibeZipEntry(out, relPfad, datei);
        }

        for (File file : path.listFiles()) {
            if (file.isDirectory()) {
                result = schreibeZipDatei(out, relPfad + file.getName() + "/", file.getAbsolutePath() + "/");
            } else {
                result = schreibeZipEntry(out, relPfad + file.getName(), file.getAbsolutePath());
            }
            if (!result)
                return result;
        }

        return result;
    }

    private static boolean schreibeZipEntry(ZipOutputStream out, String relPfad, String datei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, schreibeZipEntry(" + out + "," + relPfad
                + "," + datei + ")");
        ZipEntry zipEntry;
        byte[] buffer = new byte[0xFFFF];
        File quelldatei;

        quelldatei = new File(datei);
        if (!quelldatei.exists())
            return false;

        zipEntry = new ZipEntry(relPfad);
        try (FileInputStream fis = new FileInputStream(quelldatei)) {
            out.putNextEntry(zipEntry);
            for (int len; (len = fis.read(buffer)) != -1;) {
                out.write(buffer, 0, len);
            }
            out.closeEntry();
        } catch (Exception e) {
            LOG.debug("ERROR (static): Datei " + datei + " konnte nicht zu zip-Archiv hinzugefuegt werden.");
            LOG.debug("", e);
            return false;
        }

        return true;
    }

    public static boolean entpackeZipArchiv(String archivDatei, String zielOrdner) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, entpackeZipArchiv(" + archivDatei + ","
                + zielOrdner + ")");
        ZipFile zf;
        File file;
        InputStream is;
        BufferedInputStream bis;
        FileOutputStream fos;
        BufferedOutputStream bos;

        file = new File(archivDatei);
        if (!file.exists()) {
            LOG.debug("ERROR (static): " + archivDatei + " existiert nicht. Entpacken ist fehlgeschlagen!");
            return false;
        }

        file = new File(zielOrdner);
        if (!file.exists()) {
            file.mkdirs();
        }

        try {
            zf = new ZipFile(archivDatei);

            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry target = e.nextElement();

                file = new File(zielOrdner + target.getName());

                if (target.isDirectory())
                    file.mkdirs();
                else {
                    is = zf.getInputStream(target);
                    bis = new BufferedInputStream(is);

                    new File(file.getParent()).mkdirs();

                    fos = new FileOutputStream(file);
                    bos = new BufferedOutputStream(fos);

                    final int EOF = -1;

                    for (int c; (c = bis.read()) != EOF;)
                        bos.write((byte) c);
                    bos.close();
                    fos.close();

                    is.close();
                    bis.close();
                }

            }

            zf.close();
        } catch (FileNotFoundException e) {
            LOG.debug("EXCEPTION (static): zipfile not found");
            return false;
        } catch (ZipException e) {
            LOG.debug("EXCEPTION (static): zip error...");
            return false;
        } catch (IOException e) {
            LOG.debug("EXCEPTION (static): IO error...");
            return false;
        }
        return true;
    }

    public static boolean loescheVerzeichnisInhalt(String verzeichnis) {
        // LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung,
        // loescheVerzeichnisInhalt("+verzeichnis+")");
        File path;
        File file;
        File[] fileListe;

        path = new File(verzeichnis);

        if (path.exists()) {
            fileListe = path.listFiles();
            for (int i = 0; i < fileListe.length; i++) {
                file = fileListe[i];
                if (file.isDirectory()) {
                    if (!loescheDateien(file.getAbsolutePath())) {
                        LOG.debug(
                                "ERROR (static): Ordner " + file.getAbsolutePath() + " konnte nicht geloescht werden.");
                        return false;
                    }
                } else if (!file.delete()) {
                    LOG.debug("ERROR (static): Datei " + file.getAbsolutePath() + " konnte nicht geloescht werden.");
                    return false;
                } else {

                }
            }
        }
        return true;
    }

    public static boolean loescheDateien(String datei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, loescheDateien(" + datei + ")");
        File path;

        path = new File(datei);

        if (!loescheVerzeichnisInhalt(datei))
            return false;

        if (path.delete()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean kopiereVerzeichnis(String quelle, String ziel) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, kopiereVerzeichnis(" + quelle + "," + ziel
                + ")");
        File quellOrdner, zielOrdner, tmp;

        quellOrdner = new File(quelle);
        zielOrdner = new File(ziel);

        if (!quellOrdner.exists())
            return false;

        if (!zielOrdner.exists())
            zielOrdner.mkdirs();
        for (File file : quellOrdner.listFiles()) {
            if (file.isDirectory()) {
                tmp = new File(zielOrdner.getAbsolutePath() + "/" + file.getName());
                kopiereVerzeichnis(file.getAbsolutePath(), tmp.getAbsolutePath());
            } else
                kopiereDatei(file.getAbsolutePath(), zielOrdner.getAbsolutePath() + "/" + file.getName());

        }

        return true;

    }

    public static boolean saveStream(InputStream source, String zieldatei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, saveStream(" + source + "," + zieldatei
                + ")");
        File destfile;
        FileOutputStream fos = null;
        byte[] buffer;
        boolean result = true;

        destfile = new File(zieldatei);

        if (source == null || destfile.exists())
            result = false;
        else {
            try {
                fos = new FileOutputStream(destfile);

                buffer = new byte[0xFFFF];

                for (int len; (len = source.read(buffer)) != -1;)
                    fos.write(buffer, 0, len);

            } catch (IOException e) {
                LOG.debug("", e);
                result = false;
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {}
            }
        }

        return result;
    }

    public static boolean kopiereDatei(String quelldatei, String zieldatei) {
        LOG.trace("INVOKED (static) filius.rahmenprogramm.SzenarioVerwaltung, kopiereDatei(" + quelldatei + ","
                + zieldatei + ")");
        File srcfile, destfile;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        byte[] buffer;
        boolean result = true;

        srcfile = new File(quelldatei);
        destfile = new File(zieldatei);

        if (!srcfile.exists() || destfile.exists())
            result = false;
        else {
            try {
                fis = new FileInputStream(srcfile);
                fos = new FileOutputStream(destfile);

                buffer = new byte[0xFFFF];

                for (int len; (len = fis.read(buffer)) != -1;)
                    fos.write(buffer, 0, len);

            } catch (IOException e) {
                LOG.debug("", e);
                result = false;
            } finally {
                if (fis != null)
                    try {
                        fis.close();
                    } catch (IOException e) {}
                if (fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {}
            }
        }

        return result;
    }
}
