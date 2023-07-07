package de.unijena.cheminf.mortar.model.Fingerprints;

import javafx.beans.property.Property;

import java.util.List;
import java.util.Map;

public interface IMortarFingerprinter {
    List<Property> settingsProperties();
    Map<String, String> getSettingNameToTooltipTextMap();
    String getFingerprinterName();
    void restoreDefaultSettings(int aDefaultFingerprintDimensionality);
}
