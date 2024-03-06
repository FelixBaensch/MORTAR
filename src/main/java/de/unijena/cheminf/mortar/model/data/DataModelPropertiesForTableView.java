package de.unijena.cheminf.mortar.model.data;

/**
 * Enum for the data model properties that are displayed in the TableViews
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public enum DataModelPropertiesForTableView {
    NAME ("name"),
    UNIQUE_SMILES ("uniqueSmiles"),
    PARENT_MOLECULE_NAME ("parentMoleculeName"),
    ABSOLUTE_FREQUENCY ("absoluteFrequency"),
    ABSOLUTE_PERCENTAGE ("absolutePercentage"),
    MOLECULE_FREQUENCY ("moleculeFrequency"),
    MOLECULE_PERCENTAGE ("moleculePercentage");

    private String text;
    DataModelPropertiesForTableView(String aText) {
        this.text = aText;
    }
    public String getText() {
        return this.text;
    }
    public static DataModelPropertiesForTableView fromString(String aText) {
        for (DataModelPropertiesForTableView property : DataModelPropertiesForTableView.values()) {
            if(property.text.equalsIgnoreCase(aText)) {
                return property;
            }
        }
        return null;
    }
}
