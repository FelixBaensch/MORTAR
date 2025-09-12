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

/**
 * Represents a key used to identify properties within resource bundles or localized message sources.
 *
 * The `PropertyKey` class serves as a type-safe wrapper around a string to enforce stricter type checking
 * when passing keys to functions or methods that expect a property key. This helps reduce errors
 * associated with using plain strings as keys.
 *
 * Internally, the `PropertyKey` stores the key as a string and can provide a string representation of the key
 * when necessary.
 *
 * @author Martin Urban
 */
@JvmInline
value class PropertyKey(val key: String) {
    override fun toString(): String = key
}

//<editor-fold desc="Extension functions for PropertyKey">
/**
 * Retrieves a formatted localized message based on the specified property key and optional parameters.
 *
 * @param aListOfParams optional parameters to be substituted into the localized message
 * @return the localized and formatted message as a string
 */
fun PropertyKey.message(vararg aListOfParams: Any): String {
    return MortarBundle.message(this, *aListOfParams)
}

/**
 * Creates a lazily evaluated message pointer for a localized message identified by the given property key.
 * The message pointer serves as a function that retrieves the formatted and localized message when invoked.
 *
 * @param aListOfParams optional parameters to be formatted into the localized message
 * @return a function that, when called, returns the localized and formatted message as a string
 */
fun PropertyKey.messagePointer(vararg aListOfParams: Any): () -> String {
    return MortarBundle.messagePointer(this, *aListOfParams)
}

/**
 * Retrieves the localized message associated with the `PropertyKey`.
 *
 * This property provides shorthand access to the `MortarBundle.message`
 * function by utilizing the value of the `PropertyKey` to fetch the corresponding
 * localized message as a `String`. The localized message is determined based
 * on the current locale and resource bundles used within MORTAR.
 *
 * @receiver the `PropertyKey` representing the identifier for the localized message
 * @return the localized message as a `String`
 */
val PropertyKey.text: String
    get() = MortarBundle.message(this)

/**
 * Invokes the `message` function from `MortarBundle` to retrieve a formatted and localized message
 * corresponding to this `PropertyKey` and the provided parameters.
 *
 * @param aListOfParams optional parameters to be formatted into the localized message
 * @return the formatted localized message as a string
 */
operator fun PropertyKey.invoke(vararg aListOfParams: Any): String {
    return MortarBundle.message(this, *aListOfParams)
}
//</editor-fold>
