/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.preference;

/**
 * TODO:
 * - Add lz4 compression (and zip compression?)
 */

import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.MiscUtil;
import de.unijena.cheminf.mortar.model.util.StringSortWrapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Central container for managing preference objects.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class PreferenceContainer implements Comparable<PreferenceContainer> {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * A string for persistence methods when one preference container's persisted form has ended.
     * IMPORTANT: If you change this, hard-code "Container_End" in reloadVersion1000() and writeRepresentation and declare a new version.
     */
    private static final String CONTAINER_END = "Container_End";

    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(PreferenceContainer.class.getName());

    /**
     * Valid extensions for persistent preference container files, specifying the compression method to use.
     * DO NOT ALTER THEIR ORDER! In constructCompressedPrintWriterForContainerFile(String aFilePathname) the respective
     * array position is mapped to a compression type.
     */
    private static final String[] VALID_FILE_EXTENSIONS = {".txt", ".gzip"};
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * The container's non-unique time stamp.
     */
    private String timeStamp;

    /**
     * Globally unique identifier of this object.
     */
    private String guid;

    /**
     * File instance representing the persistent preference container file.
     */
    private File containerFile;

    /**
     * The main preference collection of this container; preference GUID strings are mapped to their IPreference objects.
     */
    private ConcurrentSkipListMap<String,IPreference> preferenceMasterMap;

    /**
     * A map that holds sets of this container's preferences categorized by their types.
     */
    private ConcurrentSkipListMap<PreferenceType,ConcurrentSkipListSet<IPreference>> preferenceTypeMap;

    /**
     * A set that holds all preferences of this container wrapped in StringSortWrapper objects with their names as sort
     * strings.
     */
    private ConcurrentSkipListSet<StringSortWrapper<IPreference>> preferenceNameWrapperSet;

    /**
     * A map that holds sets of this container's preferences categorized by their names.
     */
    private ConcurrentSkipListMap<String, ConcurrentSkipListSet<IPreference>> preferenceNameMap;

    /**
     * An array of all preferences of this container sorted by their names in ascending order for caching.
     */
    private IPreference[] preferencesSortedNameAscendingCache;

    /**
     * An array of all preferences of this container sorted by their names in descending order for caching.
     */
    private IPreference[] preferencesSortedNameDescendingCache;

    /**
     * The number of preference objects in this container for caching.
     */
    private Integer sizeCache;

    /**
     * An array of all preference objects for caching.
     */
    private IPreference[] allPreferencesCache;

    /**
     * Map for caching arrays produced by getPreferences(aPreferenceName).
     */
    private ConcurrentSkipListMap<String, IPreference[]> preferenceNameMapCache;

    /**
     * Map for caching arrays produced by getPreferences(aPreferenceType).
     */
    private ConcurrentSkipListMap<PreferenceType, IPreference[]> preferenceTypeMapCache;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Contructors">
    /**
     * Initializes an empty preference container with the given file pathname.
     *
     * @param aContainerFilePathname pathname denoting the file where the container will write its persistent representation to;
     * @throws IllegalArgumentException if aContainerFilePathname is not valid
     */
    public PreferenceContainer(String aContainerFilePathname) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!PreferenceContainer.isValidContainerFilePathname(aContainerFilePathname)) {
            throw new IllegalArgumentException("File pathname " + aContainerFilePathname + " is no valid path.");
        }
        //</editor-fold>
        this.initializeCollections();
        this.clearCache();
        this.containerFile = new File(aContainerFilePathname);
        this.timeStamp = MiscUtil.getTimestampInStandardFormat();
        this.guid = MiscUtil.getGloballyUniqueID();
    }

    /**
     * (Re-)instantiates a new PreferenceContainer object from a (potentially compressed) line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     * <br>The preferences in this container are read in successively.
     * <br>The time consumption of this method scales linear with O(size).
     *
     * @param aFile to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     * @throws SecurityException if a security manager exists and its SecurityManager.checkRead(java.lang.String)
     * method denies read access to the file
     */
    public PreferenceContainer(File aFile) throws IOException, SecurityException {
        BufferedReader tmpReader = this.constructDecompressedBufferedReaderForContainerFile(aFile.getPath());
        try {
            this.containerFile = aFile;
            //Can throw IOException
            String tmpVersion = tmpReader.readLine();
            switch (tmpVersion) {
                case "1.0.0.0":
                    this.reloadVersion1000(tmpReader);
                    break;
                //case "1.0.0.1":
                //...
                //break;
                default:
                    throw new Exception("Invalid version.");
            }
            tmpReader.close();
        } catch (Exception anException) {
            PreferenceContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            tmpReader.close();
            throw new IOException("Preference container can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    /**
     * Returns the current version of this class.
     *
     * @return the current version
     */
    public String getVersion() {
        return PreferenceContainer.VERSION;
    }

    /**
     * Returns the non-unique time stamp of this PreferenceContainer object.
     *
     * @return the non-unique time stamp of this PreferenceContainer instance
     */
    public String getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * Returns the GUID of this PreferenceContainer.
     *
     * @return the GUID of this PreferenceContainer instance
     */
    public String getGUID() {
        return this.guid;
    }

    /**
     * Returns the pathname of the written container file, ending with the file name.
     *
     * @return full pathname of the persistent file; The resulting string uses the default name-separator character
     * to separate the names in the name sequence.
     */
    public String getContainerFilePathname() {
        return this.containerFile.getPath();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets a new file path for this container where it will write its persistent representation to.
     * <br>Check validity of a given path using the static validation method in this class.
     *
     * @param aFilePathname the new file pathname
     * @throws IllegalArgumentException if the given file pathname is not valid
     */
    public void setContainerFilePathname(String aFilePathname) throws IllegalArgumentException {
        boolean tmpIsValidFilePathname = PreferenceContainer.isValidContainerFilePathname(aFilePathname);
        if (!tmpIsValidFilePathname) {
            throw new IllegalArgumentException("Given file pathname is invalid.");
        }
        this.containerFile = new File(aFilePathname);
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    //<editor-fold defaultstate="collapsed" desc="Preference-related methods">
    /**
     * Returns the number of preferences contained in this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>NOTE 1: If this container contains more than Integer.MAX_VALUE elements, it returns Integer.MAX_VALUE.
     * <br>NOTE 2: Beware that, unlike in most collections, this method is NOT a constant-time operation. Because of the
     * asynchronous nature of the ConcurrentSkipListMap, determining the current number of elements requires traversing
     * them all to count them. Additionally, it is possible for the size to change during execution of this method, in
     * which case the returned result will be inaccurate. Thus, this method is typically not very useful in concurrent
     * applications.
     * <br>The result of this method is cached.
     *
     * @return the number of preferences in this container
     */
    public int getSize() {
        if (Objects.isNull(this.sizeCache)) {
            this.sizeCache = this.preferenceMasterMap.size();
        }
        return this.sizeCache;
    }

    /**
     * Checks whether this container currently contains any preferences.
     *
     * @return true, if this container does not contain any preferences at the moment
     */
    public boolean isEmpty() {
        return this.preferenceMasterMap.isEmpty();
    }

    /**
     * Returns all preferences of this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>A collection representing the map vales (preferences) is created (with weakly consistent iterator) and turned
     * into an array
     * <br>The time consumption of this method scales linear with O(size).
     * <br>The result of this method is cached.
     *
     * @return an array of all preferences in this container
     */
    public IPreference[] getPreferences() {
        if (this.isEmpty()) {
            return new IPreference[0];
        }
        if (Objects.isNull(this.allPreferencesCache)) {
            this.allPreferencesCache = this.preferenceMasterMap.values().toArray(new IPreference[this.preferenceMasterMap.size()]);
        }
        return this.allPreferencesCache;
    }

    /**
     * Returns all preferences with the given name present in this container.
     * <br>The preference management based on names of this class is backed by a ConcurrentSkipListMap that holds
     * ConcurrentSkipListSets as values.
     * <br>The set containing preferences of the specified name is extracted from the map on average with log(size) (worst
     * case O(size)) and the creation of an array containing the set elements scales linear with O(size).
     * <br>The result of this method is cached.
     *
     * @param aPreferenceName name of the preferences
     * @return an array of all preferences with the given name contained in this container; may be empty if no preferences
     * of the given name are present in this container
     * @throws NullPointerException if the given name is 'null'
     */
    public IPreference[] getPreferences(String aPreferenceName) throws NullPointerException {
        Objects.requireNonNull(aPreferenceName, "The given name is 'null'.");
        if (this.isEmpty()) {
            return new IPreference[0];
        }
        if (this.containsPreferenceName(aPreferenceName)) {
            if (!this.preferenceNameMapCache.containsKey(aPreferenceName)) {
                ConcurrentSkipListSet<IPreference> tmpSet = this.preferenceNameMap.get(aPreferenceName);
                this.preferenceNameMapCache.put(aPreferenceName, tmpSet.toArray(new IPreference[tmpSet.size()]));
            }
            return this.preferenceNameMapCache.get(aPreferenceName);
        } else {
            return new IPreference[0];
        }
    }

    /**
     * Returns an array holding all preferences of this container with the given type.
     * <br>The preference management based on types of this class is backed by a ConcurrentSkipListMap that holds
     * ConcurrentSkipListSets as values.
     * <br>The set containing preferences of the specified type is extracted from the map on average with log(size) (worst
     * case O(size)) and the creation of an array containing the set elements scales linear with O(size).
     * <br>The result of this method is cached.
     *
     * @param aType the type of preferences that should be pulled from this container
     * @return an array of all preferences in this container with the given type; may be empty
     * @throws NullPointerException if aType is 'null'
     */
    public IPreference[] getPreferences(PreferenceType aType) throws NullPointerException {
        Objects.requireNonNull(aType, "Given type is 'null'.");
        if (this.isEmpty()) {
            return new IPreference[0];
        }
        if (this.preferenceTypeMap.containsKey(aType)) {
            if (!this.preferenceTypeMapCache.containsKey(aType)) {
                ConcurrentSkipListSet<IPreference> tmpSet = this.preferenceTypeMap.get(aType);
                this.preferenceTypeMapCache.put(aType, tmpSet.toArray(new IPreference[tmpSet.size()]));
            }
            return this.preferenceTypeMapCache.get(aType);
        } else {
            return new IPreference[0];
        }
    }

    /**
     * Returns all preferences of this container sorted in ascending order of their names.
     * <br>The preference management ordered by name of this class is backed by a ConcurrentSkipListSet that holds
     * all the preferences encapsulated in StringSortWrapper objects. So they are constantly ordered by their names in
     * ascending (alphabetical) order.
     * <br>The creation of an array containing the preferences in ascending order of their names therefore does not
     * require additional sorting and scales linear with O(size).
     * <br>The result of this method is cached.
     *
     * @return a sorted array of all preferences in this container in ascending order of their names
     */
    public IPreference[] getPreferencesSortedNameAscending() {
        if (this.isEmpty()) {
            return new IPreference[0];
        }
        if (Objects.isNull(this.preferencesSortedNameAscendingCache)) {
            if (!Objects.isNull(this.preferencesSortedNameDescendingCache)) {
                List<IPreference> tmpDescendingList = Arrays.asList(this.preferencesSortedNameDescendingCache);
                Collections.reverse(tmpDescendingList);
                this.preferencesSortedNameAscendingCache = tmpDescendingList.toArray(new IPreference[tmpDescendingList.size()]);
            } else {
                StringSortWrapper[] tmpArray = this.preferenceNameWrapperSet.toArray(new StringSortWrapper[this.preferenceNameWrapperSet.size()]);
                IPreference[] tmpPreferenceArray = new IPreference[tmpArray.length];
                for (int i = 0; i < tmpArray.length; i++) {
                    tmpPreferenceArray[i] = (IPreference) tmpArray[i].getWrappedObject();
                }
                this.preferencesSortedNameAscendingCache = tmpPreferenceArray;
            }

        }
        return this.preferencesSortedNameAscendingCache;
    }

    /**
     * Returns all preferences of this container sorted in descending order of their names.
     * <br>The preference management ordered by name of this class is backed by a ConcurrentSkipListSet that holds
     * all the preferences encapsulated in StringSortWrapper objects. So they are constantly ordered by their names in
     * ascending (alphabetical) order.
     * <br>The creation of an array containing the preferences in ascending order of their names therefore does not
     * require additional sorting and scales linear with O(size). Obtaining the preferences sorted by their names in
     * descending order on the other hand requires additional reversing of this array, an operation that runs in linear
     * time.
     * <br>The result of this method is cached.
     *
     * @return a sorted array of all preferences in this container in descending order of their names
     */
    public IPreference[] getPreferencesSortedNameDescending() {
        if (this.isEmpty()) {
            return new IPreference[0];
        }
        if (Objects.isNull(this.preferencesSortedNameDescendingCache)) {
            if (!Objects.isNull(this.preferencesSortedNameAscendingCache)) {
                List<IPreference> tmpAscendingList = Arrays.asList(this.preferencesSortedNameAscendingCache);
                Collections.reverse(tmpAscendingList);
                this.preferencesSortedNameDescendingCache = tmpAscendingList.toArray(new IPreference[tmpAscendingList.size()]);
            } else {
                //TODO: Is there a better way to do this?
                List<StringSortWrapper> tmpWrapperList = Arrays.asList(this.preferenceNameWrapperSet.toArray(new StringSortWrapper[this.preferenceNameWrapperSet.size()]));
                Collections.reverse(tmpWrapperList);
                StringSortWrapper[] tmpArray = tmpWrapperList.toArray(new StringSortWrapper[tmpWrapperList.size()]);
                IPreference[] tmpPreferenceArray = new IPreference[tmpArray.length];
                for (int i = 0; i < tmpArray.length; i++) {
                    tmpPreferenceArray[i] = (IPreference) tmpArray[i].getWrappedObject();
                }
                this.preferencesSortedNameDescendingCache = tmpPreferenceArray;
            }
        }
        return this.preferencesSortedNameDescendingCache;
    }

    /**
     * Adds a new preference to this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size)).
     *
     * @param aPreference the preference to add
     * @return true, if the operation was successful; It is unsuccessful if the container already contains this preference
     * object (will be tested based on its GUID string)
     * @throws NullPointerException if aPreference is 'null'
     */
    public boolean add(IPreference aPreference) throws NullPointerException {
        Objects.requireNonNull(aPreference, "Given preference is 'null'!");
        if (this.contains(aPreference.getGUID())) {
            return false;
        }
        this.clearCache();
        boolean tmpWasAddingSuccessful = this.addWithoutChecks(aPreference);
        return tmpWasAddingSuccessful;
    }

    /**
     * Replaces a preference in this container with another one.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size) for both the addition
     * operation and the deletion operation.
     *
     * @param anOldPreference the preference to delete from this container
     * @param aNewPreference the preference to add to this container in place of the first parameter
     * @return true, if the operation was successful; It is unsuccessful if the container does not contain anOldPreference
     * or if it already contains aNewPreference (will be tested based on their GUID strings)
     * @throws NullPointerException if one parameter is 'null'
     */
    public boolean replace(IPreference anOldPreference, IPreference aNewPreference) throws NullPointerException {
        Objects.requireNonNull(anOldPreference, "anOldPreference is 'null'.");
        Objects.requireNonNull(aNewPreference, "aNewPreference is 'null'.");
        if (!this.contains(anOldPreference.getGUID()) || this.contains(aNewPreference.getGUID())) {
            return false;
        }
        boolean tmpWasDeletionSuccessful = this.delete(anOldPreference.getGUID());
        boolean tmpWasAdditionSuccessful = this.add(aNewPreference);
        return tmpWasDeletionSuccessful && tmpWasAdditionSuccessful;
    }

    /**
     * Replaces a preference in this container with another one.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size) for both the addition
     * operation and the deletion operation.
     *
     * @param aPreferenceGUID GUID string of the preference to delete from this container
     * @param aPreference the preference to add to this container in place of the preference with the given GUID string
     * @return true, if the operation was successful; It is unsuccessful if the container does not contain a preference
     * with the given GUID or if it already contains aPreference (will be tested based on its GUID string)
     * @throws NullPointerException if one parameter is 'null'
     */
    public boolean replace(String aPreferenceGUID, IPreference aPreference) throws NullPointerException {
        Objects.requireNonNull(aPreferenceGUID, "aPreferenceGUID is 'null'.");
        Objects.requireNonNull(aPreference, "aPreference is 'null'.");
        if (!this.contains(aPreferenceGUID) || this.contains(aPreference.getGUID())) {
            return false;
        }
        boolean tmpWasDeletionSuccessful = this.delete(aPreferenceGUID);
        boolean tmpWasAdditionSuccessful = this.add(aPreference);
        return tmpWasDeletionSuccessful && tmpWasAdditionSuccessful;
    }
    /**
     * Deletes a preference from this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size)).
     *
     * @param aPreferenceGUID GUID string of the preference to remove
     * @return true, if the operation was successful; It is unsuccessful if the container does not contain a preference
     * with the given GUID
     * @throws NullPointerException if aPreferenceGUID is 'null'
     */
    public boolean delete(String aPreferenceGUID) throws NullPointerException {
        Objects.requireNonNull(aPreferenceGUID, "aPreferenceGUID is 'null'.");
        if (!this.contains(aPreferenceGUID)) {
            return false;
        }
        boolean tmpWasDeletionSuccessful = delete(aPreferenceGUID);
        return tmpWasDeletionSuccessful;
    }

    /**
     * Deletes a preference from this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size)).
     *
     * @param aPreference the preference object to delete
     * @return true, if the operation was successful; It is unsuccessful if the container does not contain aPreference
     * (will be tested based on its GUID string)
     * @throws NullPointerException if aPreference is 'null'
     */
    public boolean delete(IPreference aPreference) throws NullPointerException {
        Objects.requireNonNull(aPreference, "aPrference is 'null'.");
        if (!this.contains(aPreference.getGUID())) {
            return false;
        }
        this.clearCache();
        this.preferenceMasterMap.remove(aPreference.getGUID());
        PreferenceType tmpType = aPreference.getType();
        ConcurrentSkipListSet<IPreference> tmpTypeSet = this.preferenceTypeMap.get(tmpType);
        tmpTypeSet.remove(aPreference);
        if (tmpTypeSet.isEmpty()) {
            this.preferenceTypeMap.remove(tmpType);
        }
        String tmpName = aPreference.getName();
        ConcurrentSkipListSet<IPreference> tmpNameSet = this.preferenceNameMap.get(tmpName);
        tmpNameSet.remove(aPreference);
        if (tmpNameSet.isEmpty()) {
            this.preferenceNameMap.remove(tmpName);
        }
        this.preferenceNameWrapperSet.remove(new StringSortWrapper<>(aPreference, tmpName));
        return true;
    }

    /**
     *  Checks whether this container contains a preference with the given GUID string.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size)).
     *
     * @param aPreferenceGUID the GUID to check for
     * @return true, if this container contains a preference with this GUID; false, if otherwise or if aPreferenceGUID is
     * 'null'
     */
    public boolean contains(String aPreferenceGUID) {
        if (Objects.isNull(aPreferenceGUID)) {
            return false;
        }
        if (this.isEmpty()) {
            return false;
        }
        boolean tmpContains = this.preferenceMasterMap.containsKey(aPreferenceGUID);
        return tmpContains;
    }
    /**
     * Checks whether this container contains the given preference.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(size) on average (worst case O(size)).
     *
     * @param aPreference the preference to check for
     * @return true, if aPreference is present in this container; false, if otherwise or if aPreference is 'null'
     */
    public boolean contains(IPreference aPreference) {
        if (Objects.isNull(aPreference)) {
            return false;
        }
        if (this.isEmpty()) {
            return false;
        }
        boolean tmpContains = this.contains(aPreference.getGUID());
        return tmpContains;
    }

    /**
     * Checks whether this container contains one or more preference(s) with the given name.
     * <br>The central preference management based on names of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(n) on average (worst case O(n)) with n being the number of
     * different names of preferences present in this container.
     *
     * @param aPreferenceName the preference name to check for
     * @return true, if this container contains one or more preference(s) with the given name; false, if otherwise or if
     * aPreferenceName is 'null'
     */
    public boolean containsPreferenceName(String aPreferenceName) {
        if (Objects.isNull(aPreferenceName)) {
            return false;
        }
        if (this.isEmpty()) {
            return false;
        }
        boolean tmpContains = this.preferenceNameMap.containsKey(aPreferenceName);
        if (tmpContains) {
            ConcurrentSkipListSet tmpSet = this.preferenceNameMap.get(aPreferenceName);
            if (tmpSet.isEmpty()) {
                tmpContains = false;
            }
        }
        return tmpContains;
    }

    /**
     * Checks whether this container contains one or more preference(s) with the given type.
     * <br>The central preference management based on types of this class is backed by a ConcurrentSkipListMap.
     * <br>The time consumption of this method scales log(n) on average (worst case O(n)) with n being the number of
     * different types of preferences present in this container.
     *
     * @param aPreferenceType the preference type to check for
     * @return true, if this container contains one or more preference(s) with the given type; false, if otherwise or if
     * apreferenceType is 'null'
     */
    public boolean containsPreferenceType(PreferenceType aPreferenceType) {
        if (Objects.isNull(aPreferenceType)) {
            return false;
        }
        if (this.isEmpty()) {
            return false;
        }
        boolean tmpContains = this.preferenceTypeMap.containsKey(aPreferenceType);
        if (tmpContains) {
            ConcurrentSkipListSet tmpSet = this.preferenceTypeMap.get(aPreferenceType);
            if (tmpSet.isEmpty()) {
                tmpContains = false;
            }
        }
        return tmpContains;
    }

    /**
     * Clears all preferences from this container.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>A collection representing the map values (preferences) is created (with weakly consistent iterator) and the
     * preferences are deleted successively.
     * <br>The time consumption of this method scales linear with O(size).
     */
    public void clearAll() {
        if (this.isEmpty()) {
            return;
        }
        this.clearCache();
        for (IPreference tmpPreference : this.preferenceMasterMap.values()) {
            this.delete(tmpPreference);
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Cloning and copying">
    /**
     * Returns a deep copy of this object.
     * <br>The central preference management of this class is backed by a ConcurrentSkipListMap.
     * <br>A collection representing the map values (preferences) is created (with weakly consistent iterator) and the
     * preferences are copied successively.
     * <br>The time consumption of this method scales linear with O(size) with size meaning the number of preferences.
     *
     * @return a deep copy of this object
     * @throws java.lang.CloneNotSupportedException if copying this object fails, i.e. du to a not copyable preference
     * object
     */
    public PreferenceContainer copy() throws CloneNotSupportedException {
        PreferenceContainer tmpCopy = new PreferenceContainer(new String(this.containerFile.getPath()));
        tmpCopy.guid = new String(this.guid);
        tmpCopy.timeStamp = new String(this.timeStamp);
        if (this.isEmpty()) {
            return tmpCopy;
        }
        for (IPreference tmpPreference : this.preferenceMasterMap.values()) {
            tmpCopy.addWithoutChecks(tmpPreference.copy());
        }
        return tmpCopy;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Persistence">
    /**
     * Writes a persistent representation of this preference container to a line-based text file.
     * <br>A new file in the data directory will be created with the specific file name of this preference container.
     * <br>The preferences in this container are written successively.
     * <br>The time consumption of this method scales linear with O(size) with size meaning the number of preferences
     * in this container.
     *
     * @throws java.io.IOException if anything goes wrong
     * @throws java.lang.SecurityException If a security manager exists and its
     * SecurityManager.checkRead(java.lang.String) method denies read access to the file or directory
     */
    public void writeRepresentation() throws IOException, SecurityException {
        this.writeRepresentationTo(this.containerFile.getPath());
    }

    /**
     * Writes a persistent representation of this container to a line-based text file represented by the given
     * file pathname.
     * <br>The file pathname of this object is not altered; this method is designed for backup operations.
     * <br>The preferences in this container are written successively.
     * <br>The time consumption of this method scales linear with O(size) with size meaning the number of preferences
     * in this container.
     *
     * @param aFilePathname pathname of the destined backup file (existing files will be overwritten)
     * @throws java.io.IOException if anything goes wrong, e.g. an invalid file pathname is given; check validity of a
     * given path using the static validation method in this class.
     * @throws SecurityException If a security manager exists and its SecurityManager.checkRead(java.lang.String) method
     * denies read access to the file or directory
     */
    public void writeRepresentationTo(String aFilePathname) throws IOException, SecurityException {
        PrintWriter tmpPrintWriter = this.constructCompressedPrintWriterForContainerFile(aFilePathname);
        try {
            tmpPrintWriter.println(PreferenceContainer.VERSION);
            tmpPrintWriter.println(this.guid);
            tmpPrintWriter.println(this.timeStamp);
            if (this.isEmpty()) {
                tmpPrintWriter.println(PreferenceContainer.CONTAINER_END);
                return;
            }
            for (String tmpKey : this.preferenceMasterMap.keySet()) {
                IPreference tmpPreference = this.preferenceMasterMap.get(tmpKey);
                tmpPrintWriter.println(tmpPreference.getType().name());
                tmpPreference.writeRepresentation(tmpPrintWriter);
            }
            tmpPrintWriter.println(PreferenceContainer.CONTAINER_END);
            tmpPrintWriter.close();
        } catch (Exception anException) {
            PreferenceContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            tmpPrintWriter.close();
            throw new IOException("Project object can not be written to file.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Caching">
    /**
     * Clear the cache of this PreferenceContainer object.
     */
    public void clearCache() {
        this.preferencesSortedNameAscendingCache = null;
        this.preferencesSortedNameDescendingCache = null;
        this.sizeCache = null;
        this.preferenceNameMapCache.clear();
        this.preferenceTypeMapCache.clear();
        this.allPreferencesCache = null;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Comparing and sorting">
    /**
     * Compares this PreferenceContainer object with another PreferenceContainer object based on their GUIDs.
     * Returns a negative integer, zero, or a positive integer as the GUID of this container is less than, equal to, or
     * greater than the GUID of aContainer.
     *
     * @param aContainer the container object to compare with
     * @return a negative integer, zero, or a positive integer as this container object is less than, equal to, or
     * greater than aContainer
     * @throws NullPointerException if aContainer is 'null'
     */
    @Override
    public int compareTo(PreferenceContainer aContainer) throws NullPointerException {
        Objects.requireNonNull(aContainer, "aContainer is 'null'");
        //Comparing on String level
        return this.guid.compareTo(aContainer.getGUID());
    }

    /**
     * Equality is determined by comparing GUID strings.
     * So containerA.equals(containerB) is true if containerA.getGUID().equals(containerB.getGUID()) is true.
     *
     * @param anObject the object to test for equality
     * @return true if this object equals anObject
     */
    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject == null || (anObject.getClass() != this.getClass())) {
            return false;
        }
        PreferenceContainer tmpContainer = (PreferenceContainer) anObject;
        return this.hashCode() == tmpContainer.hashCode();
    }

    /**
     * Returns a hash code for this object based on its GUID string.
     *
     * @return hash code based on GUID
     */
    @Override
    public int hashCode() {
        int tmpHash = 13;
        tmpHash = 31 * tmpHash + Objects.hashCode(this.guid);
        return tmpHash;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return this.getClass().getName() + "_GUID:" + this.getGUID() + "_size:" + this.getSize();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests whether the given string would be allowed as file pathname of a new preference container object.
     *
     * @param aFilePathname the string to test
     * @return true if given string is not 'null',it is not empty, it represents a file with a valid extension and
     * the application can write to this directory
     */
    public static boolean isValidContainerFilePathname(String aFilePathname) {
        if (Objects.isNull(aFilePathname) || aFilePathname.isEmpty()) {
            return false;
        }
        boolean tmpFileHasValidExtension = false;
        String tmpFileExtension = FileUtil.getFileExtension(aFilePathname);
        for (String tmpExtension : PreferenceContainer.VALID_FILE_EXTENSIONS) {
            if (tmpFileExtension.equals(tmpExtension)) {
                tmpFileHasValidExtension = true;
                break;
            }
        }
        if (!tmpFileHasValidExtension) {
            return false;
        }
        File tmpFile = new File(aFilePathname);
        boolean tmpIsDirectory = tmpFile.isDirectory();
        if (tmpIsDirectory) {
            return false;
        }
        try {
            tmpFile.getCanonicalPath();
        } catch (SecurityException | IOException anException) {
            PreferenceContainer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
        return true;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private methods">
    /**
     * Adds aPreference to this container. This method performs no checks and is private in order to be used in
     * PreferenceContainer(BufferedReader) to re-instantiate this object with maximum performance.
     * Cache-clearing must be done by calling method!
     */
    private boolean addWithoutChecks(IPreference aPreference) {
        this.preferenceMasterMap.put(aPreference.getGUID(), aPreference);
        PreferenceType tmpType = aPreference.getType();
        if (this.preferenceTypeMap.containsKey(tmpType)) {
            ConcurrentSkipListSet<IPreference> tmpSet = this.preferenceTypeMap.get(tmpType);
            tmpSet.add(aPreference);
        } else {
            ConcurrentSkipListSet<IPreference> tmpNewSet = new ConcurrentSkipListSet<>();
            tmpNewSet.add(aPreference);
            this.preferenceTypeMap.put(tmpType, tmpNewSet);
        }
        this.preferenceNameWrapperSet.add(new StringSortWrapper<>(aPreference, aPreference.getName()));
        String tmpName = aPreference.getName();
        if (this.preferenceNameMap.containsKey(tmpName)) {
            ConcurrentSkipListSet<IPreference> tmpSet = this.preferenceNameMap.get(tmpName);
            tmpSet.add(aPreference);
        } else {
            ConcurrentSkipListSet<IPreference> tmpNewSet = new ConcurrentSkipListSet<>();
            tmpNewSet.add(aPreference);
            this.preferenceNameMap.put(tmpName, tmpNewSet);
        }
        return true;
    }

    /**
     * Initializes all collections of this object.
     */
    private void initializeCollections() {
        this.preferenceMasterMap = new ConcurrentSkipListMap<>();
        this.preferenceNameMap = new ConcurrentSkipListMap<>();
        this.preferenceNameWrapperSet = new ConcurrentSkipListSet<>();
        this.preferenceTypeMap = new ConcurrentSkipListMap<>();
        this.preferenceNameMapCache = new ConcurrentSkipListMap<>();
        this.preferenceTypeMapCache = new ConcurrentSkipListMap<>();
    }

    /**
     * (Re-)instantiates a new PreferenceContainer object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws Exception {
        this.initializeCollections();
        this.clearCache();
        this.guid = aReader.readLine();
        this.timeStamp = aReader.readLine();
        String tmpPreferenceTypeOrContainerEnd = aReader.readLine();
        //If PreferenceContainer.CONTAINER_END is altered, this will not work anymore.
        while (!tmpPreferenceTypeOrContainerEnd.equals(PreferenceContainer.CONTAINER_END)) {
            if (tmpPreferenceTypeOrContainerEnd.isEmpty()) {
                continue;
            }
            IPreference tmpPreference = PreferenceFactory.reinitializePreference(tmpPreferenceTypeOrContainerEnd, aReader);
            boolean tmpWasLoadingSuccessful = this.addWithoutChecks(tmpPreference);
            if (!tmpWasLoadingSuccessful) {
                throw new Exception();
            }
            tmpPreferenceTypeOrContainerEnd = aReader.readLine();
        }
    }

    /**
     * Constructs a compressing (if necessary) PrintWriter object that can be used to write a preference container file.
     *
     * @param aFilePathname the destined file
     * @return a compressed (if necessary) PrintWriter object
     * @throws java.io.IOException if the file pathname is invalid or any other problem occurs
     * @throws SecurityException if application can not access the denoted file or directory
     */
    private PrintWriter constructCompressedPrintWriterForContainerFile(String aFilePathname) throws IOException, SecurityException {
        boolean tmpIsValidFilePathname = PreferenceContainer.isValidContainerFilePathname(aFilePathname);
        if (!tmpIsValidFilePathname) {
            throw new IOException("Given file pathname is invalid.");
        }
        String tmpFileExtension = FileUtil.getFileExtension(aFilePathname);
        File tmpContainerFile = new File(aFilePathname);
        boolean tmpFileAlreadyExists = tmpContainerFile.exists();
        if (tmpFileAlreadyExists) {
            boolean tmpWasDeletionSuccessful = FileUtil.deleteSingleFile(tmpContainerFile.getPath());
            if (!tmpWasDeletionSuccessful) {
                throw new IOException("Unable to delete former container file.");
            }
        }
        boolean tmpWasCreationSuccessful = FileUtil.createEmptyFile(tmpContainerFile.getPath());
        if (!tmpWasCreationSuccessful) {
            throw new IOException("Unable to create new container file.");
        }
        if (!tmpContainerFile.canWrite()) {
            throw new IOException("Unable to modify the destined container file.");
        }
        FileOutputStream tmpFileOut = new FileOutputStream(tmpContainerFile, false);
        BufferedOutputStream tmpBufOut;
        if (tmpFileExtension.equals(PreferenceContainer.VALID_FILE_EXTENSIONS[0])) {
            tmpBufOut = new BufferedOutputStream(tmpFileOut, BasicDefinitions.BUFFER_SIZE);
        } else if (tmpFileExtension.equals(PreferenceContainer.VALID_FILE_EXTENSIONS[1])) {
            GZIPOutputStream tmpGzipOut = new GZIPOutputStream(tmpFileOut, BasicDefinitions.BUFFER_SIZE, false);
            tmpBufOut = new BufferedOutputStream(tmpGzipOut, BasicDefinitions.BUFFER_SIZE);
        } else {
            tmpFileOut.close();
            throw new IOException("Invalid file extension.");
        }
        PrintWriter tmpPrintWriter = new PrintWriter(tmpBufOut, false);
        return tmpPrintWriter;
    }

    /**
     * Constructs a decompressing (if necessary) BufferedReader object that can be used to open and read container files.
     *
     * @param aFilePathname representing the persisted container file
     * @return buffered reader reading from the container file
     * @throws java.io.IOException if the file pathname is invalid or any other problem occurs
     * @throws SecurityException if application can not access the denoted file or directory
     */
    private BufferedReader constructDecompressedBufferedReaderForContainerFile(String aFilePathname)
            throws IOException, NullPointerException, SecurityException {
        boolean tmpIsValidFilePathname = PreferenceContainer.isValidContainerFilePathname(aFilePathname);
        if (!tmpIsValidFilePathname) {
            throw new IOException("Given file pathname is invalid.");
        }
        String tmpFileExtension = FileUtil.getFileExtension(aFilePathname);
        File tmpContainerFile = new File(aFilePathname);
        boolean tmpIsFile = tmpContainerFile.isFile();
        boolean tmpCanRead = tmpContainerFile.canRead();
        if (!tmpIsFile || !tmpCanRead) {
            throw new IOException("Given file does not exist, does not represent a file or can not be read.");
        }
        FileInputStream tmpFileIn = new FileInputStream(tmpContainerFile);
        InputStreamReader tmpInStreamReader;
        if (tmpFileExtension.equals(PreferenceContainer.VALID_FILE_EXTENSIONS[0])) {
            //TODO: Specify charset? Uses class of nio package...
            tmpInStreamReader = new InputStreamReader(tmpFileIn, System.getProperty("file.encoding"));
        } else if (tmpFileExtension.equals(PreferenceContainer.VALID_FILE_EXTENSIONS[1])) {
            GZIPInputStream tmpGzipIn = new GZIPInputStream(tmpFileIn, BasicDefinitions.BUFFER_SIZE);
            //TODO: Specify charset? Uses class of nio package...
            tmpInStreamReader = new InputStreamReader(tmpGzipIn, System.getProperty("file.encoding"));
        } else {
            tmpFileIn.close();
            throw new IOException("Invalid file extension.");
        }
        BufferedReader tmpReader = new BufferedReader(tmpInStreamReader, BasicDefinitions.BUFFER_SIZE);
        return tmpReader;
    }
    //</editor-fold>
}
