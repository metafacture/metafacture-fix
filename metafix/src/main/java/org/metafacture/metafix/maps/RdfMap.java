/*
 * Copyright 2022 hbz
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metafacture.metafix.maps;

import org.metafacture.metafix.FixExecutionException;
import org.metafacture.metamorph.api.Maps;
import org.metafacture.metamorph.api.helpers.AbstractReadOnlyMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PropertyNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Provides a dynamically build {@link Map} based on an RDF resource. Can be one file or a comma separated list of RDF
 * files or an HTTP(S) URI. Redirections of HTTP(S) URIs are followed.
 * The resources are supposed to be UTF-8 encoded.
 * <p>
 *
 * <strong>Important:</strong> When using a list of files make sure to set the proper separator. All lines that are not
 * split in two parts by the separator are ignored!
 *
 * @author Markus Michael Geipel
 * @author Pascal Christoph (dr0i)
 * @see org.metafacture.metamorph.maps.FileMap
 */
public final class RdfMap extends AbstractReadOnlyMap<String, String> {
    private static String targetLanguage = "";
    private static String target;
    private static final Logger LOG = LoggerFactory.getLogger(RdfMap.class);
    private static final int MAX_REDIRECTIONS = 10;
    private static final int MIN_HTTP_STATUS_CODE = 299;
    private static final int MAX_HTTP_STATUS_CODE = 400;

    private Model model;
    private boolean isUninitialized = true;
    private final ArrayList<String> filenames = new ArrayList<>();
    private final Map<String, String> map = new HashMap<>();

    /**
     * Creates an instance of {@link RdfMap}.
     */
    public RdfMap() {
        targetLanguage = "";
    }

    private void init() {
        loadFiles();
        if (!map.containsKey(Maps.DEFAULT_MAP_KEY)) {
            setDefault(Maps.DEFAULT_MAP_KEY);
        }
        if (!target.toLowerCase().startsWith("http")) {
            final String[] nsPrefixAndProperty = target.split(":");
            target = nsPrefixAndProperty.length == 2 ? model.getNsPrefixURI(nsPrefixAndProperty[0]) + nsPrefixAndProperty[1] : nsPrefixAndProperty[0];
        }
        isUninitialized = false;
    }

    /**
     * Sets a comma separated list of files which provides the {@link Model}.
     *
     * @param files a comma separated list of files
     */
    public void setFiles(final String files) {
        Collections.addAll(filenames, files.split("\\s*,\\s*"));
    }

    /**
     * Sets a file which provides the {@link Model}.
     *
     * @param file the file
     */
    public void setFile(final String file) {
        Collections.addAll(filenames, file);
    }

    private void loadFiles() {
        filenames.forEach(this::loadFile);
    }

    private void loadFile(final String file) {
        String f = file;
        try {
            if (file.toLowerCase().startsWith("http")) {
                f = read(file);
            }
            if (model == null) {
                model = RDFDataMgr.loadModel(f);
            }
            else {
                RDFDataMgr.read(model, f);
            }
        }
        catch (final IOException e) {
            throw new FixExecutionException("rdf file: cannot read file", e);
        }
    }

    /**
     * Builds a Map dynamically by querying an RDF model based on a key and a targeted Property
     * (to be set in {@link RdfMap#setTarget(String)}) and an optional language tag (to be set in
     * {@link RdfMap#setTargetLanguage}).
     * <br>
     * The Map acts as a cache.
     * <p>
     * To minimize the need of parameters three different querying modes are gone through. If one fails, the next one is
     * tried:
     * <p>
     * 1. Get the value of the targeted Property of a Subject
     * <br>
     * 2. Get the Subject matching a targeted Property value
     * <br>
     * 3. Get the value of a Property using the value of a targeted Property
     *
     * @param key the Property value, or a Subject, to be looked up
     */
    @Override
    public String get(final Object key) {
        if (isUninitialized) {
            init();
        }
        String ret;
        if (map.containsKey(key.toString())) {
            ret = map.get(key.toString());
        }
        else {
            final Resource resource = ResourceFactory.createResource(key.toString());
            final Property targetProperty = ResourceFactory.createProperty(target);
            try {
                //first try to get LITERAL using SUBJECT and PROPERTY
                if (!RdfMap.targetLanguage.isEmpty()) {
                    ret = model.getRequiredProperty(resource, targetProperty, RdfMap.targetLanguage).getString();
                }
                else {
                    ret = model.getRequiredProperty(resource, targetProperty).getString();
                }
            }
            catch (final PropertyNotFoundException | NullPointerException | NoSuchElementException e) {
                //second try to get SUBJECT using PROPERTY and LITERAL
                ret = getSubjectUsingPropertyAndLiteral(key, targetProperty);
                //third try: get LITERAL of PREDICATE A using PREDICATE B
                if (ret.equals(Maps.DEFAULT_MAP_KEY)) {
                    ret = getLiteralOfPredicateUsingOtherPredicate(key, targetProperty);
                }
                else {
                    LOG.info("Could not lookup:'" + key + "@" + (RdfMap.targetLanguage.isEmpty() ? RdfMap.targetLanguage : "") + " for " + target + "'. Going with default value.");
                }
            }
            map.put(key.toString(), ret);
        }
        return ret;
    }

    private String getLiteralOfPredicateUsingOtherPredicate(final Object key, final Property targetProperty) {
        Resource resource;
        final ResIterator iter;
        String ret = map.get(Maps.DEFAULT_MAP_KEY);
        iter = model.listSubjectsWithProperty(targetProperty);
        while (iter.hasNext()) {
            resource = iter.nextResource();
            if (resource.getProperty(targetProperty).getString().equals(key.toString())) {
                Statement stmt;
                final StmtIterator iterProp = resource.listProperties(targetProperty);
                while (iterProp.hasNext()) {
                    stmt = iterProp.nextStatement();
                    if (stmt.getLanguage().equals(RdfMap.targetLanguage) && !stmt.getString().equals(key)) {
                        ret = stmt.getString();
                    }
                }
            }
        }
        return ret;
    }

    private String getSubjectUsingPropertyAndLiteral(final Object key, final Property targetProperty) {
        Resource resource;
        String ret = map.get(Maps.DEFAULT_MAP_KEY);
        final ResIterator iter = model.listSubjectsWithProperty(targetProperty);
        while (iter.hasNext()) {
            resource = iter.nextResource();
            if (resource.getProperty(targetProperty).getString().equals(key.toString())) {
                if (!RdfMap.targetLanguage.isEmpty()) {
                    if (resource.getProperty(targetProperty).getLanguage().equals(RdfMap.targetLanguage)) {
                        ret = resource.getURI();
                    }
                }
                else {
                    ret = resource.getURI();
                }
            }
        }
        return ret;
    }

    /**
     * Sets the language of the target Property which is queried in the RDF. Valid values are defined by BCP47.
     * <br>
     * Setting the language of the target Property is optional.
     *
     * @param targetLanguage the language of the target Property to be queried
     */
    public void setTargetLanguage(final String targetLanguage) {
        RdfMap.targetLanguage = targetLanguage;
    }

    /**
     * Sets the target Property which is queried in the RDF. Namespaces are allowed.
     * <br>
     * <strong>Setting a target Property is mandatory.</strong>
     *
     * @param target the Property to be queried
     */
    public void setTarget(final String target) {
        RdfMap.target = target;
    }

    /**
     * Sets the default value returned if the key couldn't be found.
     * <br>
     * <strong>Default value: {@link Maps#DEFAULT_MAP_KEY} </strong>
     *
     * @param defaultValue the default value returned
     */
    public void setDefault(final String defaultValue) {
        map.put(Maps.DEFAULT_MAP_KEY, defaultValue);
    }

    /**
     * Gets a redirected URL, if any redirection takes place. Adapted predated code from org.apache.jena.rdfxml.xmlinput.JenaReader.
     *
     * @Deprecated Using newer jena version (needs java 11) this method would be obsolete.
     * @param url the URL to resolve
     * @return the (redirected) URL
     * @throws IOException if any IO error occurs
     */
    private String read(final String url) throws IOException {
        String connectionURL = url;
        try {
            int count = 0;
            URLConnection conn;
            while (true) {
                final URLConnection conn2 = new URL(connectionURL).openConnection();
                if (!(conn2 instanceof HttpURLConnection)) {
                    conn = conn2;
                    break;
                }
                count += 1;
                if (count > MAX_REDIRECTIONS) {
                    throw new IOException("Too many redirects followed for " + url);
                }
                final HttpURLConnection httpURLConnection = (HttpURLConnection) conn2;
                conn2.setRequestProperty("accept", "*/*");
                final int statusCode = httpURLConnection.getResponseCode();
                if (statusCode <= MIN_HTTP_STATUS_CODE || statusCode >= MAX_HTTP_STATUS_CODE) {
                    conn = conn2;
                    break;
                }
                // Redirect
                connectionURL = conn2.getHeaderField("Location");
                if (connectionURL == null || url.equals(connectionURL)) {
                    throw new IOException("Failed to follow redirects for " + url);
                }
            }
            connectionURL = conn.getURL().toString();
        }
        catch (final IOException e) {
            throw new IOException(e);
        }
        return connectionURL;
    }
}