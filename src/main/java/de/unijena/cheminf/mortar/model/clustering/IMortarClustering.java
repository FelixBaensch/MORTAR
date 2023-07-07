package de.unijena.cheminf.mortar.model.clustering;

import javafx.beans.property.Property;

import java.util.List;
import java.util.Map;

public interface IMortarClustering { // TODO
    List<Property> settingsProperties();
    Map<String, String> getSettingNameToTooltipTextMap();
    String getClusteringName();
    void restoreDefaultSettings();
    IMortarClustering copy();
}
