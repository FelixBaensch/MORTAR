/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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
package de.unijena.cheminf.mortar.gradle.bundle

import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Utility object for managing and retrieving localized messages from resource bundles
 * within the MORTAR framework.
 *
 * Resource bundles are expected to follow the naming convention specified by
 * the `BUNDLE_NAME` constant.
 *
 * @author Martin Urban
 */
object MortarBundle {
    private val LOGGER: Logger = Logger.getLogger(MortarBundle::class.java.name)
    private const val BUNDLE_NAME = "messages.MortarBundle"
    private val bundleCache = ConcurrentHashMap<Locale, ResourceBundle>()
    private val defaultLocale = Locale.getDefault()

    //<editor-fold desc="public static methods">
    /**
     * Retrieves a formatted localized message corresponding to the given key and optional parameters.
     *
     * @param aKey the property key used to identify the localized message
     * @param aListOfParams optional parameters to be formatted into the localized message
     * @return the formatted localized message as a string
     */
    @JvmStatic
    fun message(aKey: PropertyKey, vararg aListOfParams: Any): String {
        return message(aKey.key, *aListOfParams)
    }

    /**
     * Retrieves a localized message corresponding to the given key and formats it using the provided parameters.
     *
     * @param aKey the key used to identify the localized message
     * @param aListOfParams an optional vararg array of parameters to be inserted into the localized message
     * @return the localized and formatted message as a string, or a fallback string if the key is not found or formatting fails
     */
    @JvmStatic
    fun message(aKey: String, vararg aListOfParams: Any): String {
        return try {
            val tmpBundle = getBundle()
            val tmpPattern = tmpBundle.getString(aKey)

            if (aListOfParams.isEmpty()) {
                tmpPattern
            } else {
                MessageFormat.format(tmpPattern, *aListOfParams)
            }
        } catch (anException: Exception) {
            // Fallback for any formatting errors
            LOGGER.log(Level.WARNING, anException.toString(), anException)
            "!$aKey!"
        }
    }

    /**
     * Creates a lazily evaluated message pointer that retrieves a formatted localized message
     * based on the given key and parameters when invoked.
     *
     * @param aKey the property key used to identify the localized message
     * @param aListOfParams optional parameters to be formatted into the localized message
     * @return a function that, when called, returns the localized and formatted message as a string
     */
    @JvmStatic
    fun messagePointer(aKey: PropertyKey, vararg aListOfParams: Any): () -> String {
        return { message(aKey, *aListOfParams) }
    }

    /**
     * Clears the bundle cache. Useful for testing or when locale changes.
     */
    @JvmStatic
    fun clearCache() {
        bundleCache.clear()
    }

    /**
     * Checks if the specified property key exists in the resource bundle.
     *
     * @param aKey the property key to check for existence in the resource bundle
     * @return true if the key exists in the bundle, false otherwise
     */
    @JvmStatic
    fun hasKey(aKey: PropertyKey): Boolean = hasKey(aKey.key)

    /**
     * Checks if the given key exists in the resource bundle.
     *
     * @param aKey the key to be checked for existence in the resource bundle
     * @return true if the key exists, false otherwise
     */
    @JvmStatic
    fun hasKey(aKey: String): Boolean {
        try {
            getBundle().getString(aKey)
            return true
        } catch (anException: MissingResourceException) {
            LOGGER.log(Level.WARNING, anException.toString(), anException)
            return false
        }
    }

    /**
     * Retrieves the set of all keys available in the resource bundle. If the resource bundle is missing,
     * an empty set is returned.
     *
     * @return a set containing all keys in the resource bundle, or an empty set if the bundle is missing.
     */
    @JvmStatic
    fun getKeys(): Set<String> {
        try {
            return getBundle().keySet()
        } catch (anException: MissingResourceException) {
            LOGGER.log(Level.WARNING, anException.toString(), anException)
            return emptySet()
        }
    }
    //</editor-fold>

    //<editor-fold desc="private methods">
    /**
     * Retrieves a resource bundle for the specified locale. Falls back to the default locale if the
     * resource bundle for the specified locale is not found.
     *
     * @param aLocale the locale for which the resource bundle should be retrieved. Defaults to the application's default locale.
     * @return the resource bundle corresponding to the specified locale, or the default locale if the specific one is not available.
     */
    private fun getBundle(aLocale: Locale = defaultLocale): ResourceBundle {
        return bundleCache.computeIfAbsent(aLocale) { loc ->
            try {
                ResourceBundle.getBundle(BUNDLE_NAME, loc)
            } catch (anException: MissingResourceException) {
                LOGGER.log(Level.WARNING, anException.toString(), anException)
                // Fallback to default locale if specific locale not found
                if (loc != Locale.ROOT) {
                    ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT)
                } else {
                    throw anException
                }
            }
        }
    }
    //</editor-fold>
}
