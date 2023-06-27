package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.fragmentFingerprinter.IFragmentFingerprinter;
import javafx.beans.property.Property;

import java.util.List;
import java.util.Map;

public interface IFingerprintClustering { // TODO
    List<Property> settingsProperties();
    Map<String, String> getSettingNameToTooltipTextMap();
    String getClusteringName();
    void restoreDefaultSettings();
    IFingerprintClustering copy();
}
