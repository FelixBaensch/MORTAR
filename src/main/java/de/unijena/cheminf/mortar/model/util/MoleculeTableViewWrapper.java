/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MoleculeTableViewWrapper {

    private ImageView moleculeImageView;
    private BooleanProperty isSelected;
    private StringProperty moleculeName;

    public MoleculeTableViewWrapper(){
        this.moleculeImageView = new ImageView();
        this.moleculeName = new SimpleStringProperty("Werbung");
        this.isSelected = new SimpleBooleanProperty(false);
    }

    public ImageView getMoleculeImageView(){
        return this.moleculeImageView;
    }
    public boolean isMoleculeSelected(){
        return this.isSelected.getValue();
    }
    public BooleanProperty isSelectedProperty(){
        return this.isSelected;
    }
    public String getMoleculeName(){
        return this.moleculeName.getValue();
    }
    public StringProperty moleculeNameProperty(){
        return this.moleculeName;
    }
}
