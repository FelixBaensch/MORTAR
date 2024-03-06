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

package de.unijena.cheminf.mortar.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.Properties;

/**
 * Thread-safe singleton class for reading configuration properties file, e.g. for paths to resource folders.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class Configuration implements IConfiguration {
    /**
     * Single instance of this class.
     */
    private static Configuration instance;
    //
    /**
     * Properties imported and cached from the configuration properties file.
     */
    private final Properties properties;
    //
    /**
     * Path to the configuration properties file to read.
     */
    private static final String PROPERTIES_FILE_PATH = "de/unijena/cheminf/mortar/configuration/MORTAR_configuration.properties";
    //
    /**
     * Private constructor that creates the single instance.
     *
     * @throws IOException if the properties file is not found or if an error occurs when reading from the input stream
     */
    private Configuration() throws IOException {
        this.properties = new Properties();
        try (InputStream tmpInputStream = this.getClass().getClassLoader().getResourceAsStream(Configuration.PROPERTIES_FILE_PATH)) {
            if (tmpInputStream != null) {
                //throws IOException if an error occurs when reading from the input stream
                this.properties.load(tmpInputStream);
            } else {
                //extends IOException
                throw new FileNotFoundException(String.format("property file '%s' not found in the classpath", Configuration.PROPERTIES_FILE_PATH));
            }
        }
    }
    //
    /**
     * Returns the single instance of this class.
     *
     * @return instance of this class
     * @throws IOException if the properties file is not found or if an error occurs when reading from the input stream
     */
    public static synchronized Configuration getInstance() throws IOException {
        if (Configuration.instance == null) {
            Configuration.instance = new Configuration();
        }
        return Configuration.instance;
    }
    //
    @Override
    public String getProperty(String aKey) throws MissingResourceException {
        String tmpProperty = this.properties.getProperty(aKey).trim();
        if (tmpProperty == null) {
            throw new MissingResourceException(String.format("Property '%s' not found", aKey), Configuration.class.getName(), aKey);
        } else {
            return tmpProperty;
        }
    }
}
