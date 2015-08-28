/*
    The MIT License (MIT)

    Copyright (c) 2015, Hans-Georg Becker, http://orcid.org/0000-0003-0432-294X

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
 */
package de.tu_dortmund.ub.hb_ng;

import de.tu_dortmund.ub.data.ldp.storage.LinkedDataStorage;
import de.tu_dortmund.ub.data.ldp.storage.LinkedDataStorageException;
import net.sf.saxon.s9api.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * Implementation of the LinkedDataStorage interface for hb_ng's de.tu_dortmund.ub.hb_ng.data.SolRDF
 *
 * @author Dipl.-Math. Hans-Georg Becker, M.L.I.S. (UB Dortmund)
 * @version 2015-08-15
 */
public class SolRDF implements LinkedDataStorage {

    private Properties config = null;
    private Logger logger = null;

    @Override
    public void init(Properties config) {

        this.config = config;
        PropertyConfigurator.configure(this.config.getProperty("service.log4j-conf"));
        this.logger = Logger.getLogger(SolRDF.class.getName());
    }

    @Override
    public String getResource(String uri, String format) throws LinkedDataStorageException {

        return this.getResource(null, uri, format, false);
    }

    @Override
    public String getResource(String uri, String format, boolean isAuthorized) throws LinkedDataStorageException {

        return this.getResource(null, uri, format, false);
    }

    @Override
    public String getResource(String graph, String uri, String format) throws LinkedDataStorageException {

        return this.getResource(graph, uri, format, false);
    }

    @Override
    public String getResource(String graph, String uri, String format, boolean isAuthorized) throws LinkedDataStorageException {

        this.logger.info("getResource: " + graph);
        this.logger.info("getResource: " + uri);
        this.logger.info("getResource: " + format);
        this.logger.info("getResource: " + isAuthorized);

        String resource = null;

        String queryResult = this.doResourceRequest(graph, uri, format, isAuthorized);

        if (queryResult != null && !queryResult.equals("")) {

            try {

                switch (format) {

                    case "html": {

                        Document document = new SAXBuilder().build(new StringReader(queryResult));

                        StringWriter stringWriter = new StringWriter();
                        stringWriter.write(htmlOutputter(document, this.config.getProperty("xslt.resource"), null));
                        resource = stringWriter.toString();

                        break;
                    }
                    case "rdf.xml": {

                        Document document = new SAXBuilder().build(new StringReader(queryResult));

                        StringWriter stringWriter = new StringWriter();
                        XMLOutputter xmlOutputter = new XMLOutputter();
                        xmlOutputter.output(document, stringWriter);
                        resource = stringWriter.toString();

                        break;
                    }
                    case "rdf.ttl": {

                        resource = queryResult;

                        break;
                    }
                    case "json": {

                        resource = queryResult;

                        break;
                    }
                    case "nquads": {

                        resource = queryResult;

                        break;
                    }
                }
            }
            catch (JDOMException | IOException e) {

                throw new LinkedDataStorageException(e.getMessage(), e.getCause());
            }
        }

        return resource;
    }

    @Override
    public String getAccessRights(String resource) throws LinkedDataStorageException {

        return this.getAccessRights(null, resource);
    }

    @Override
    public String getAccessRights(String graph, String uri) throws LinkedDataStorageException {

        this.logger.info("getAccessRights: " + graph);
        this.logger.info("getAccessRights: " + uri);

        String accessRights = "";

        if (uri.endsWith("/about")) {

            accessRights = "public";
        }
        else {

            // TODO config.properties
            String sparql = "SELECT ?o WHERE { <" + uri + "/about> <http://purl.org/dc/terms#accessRights> ?o }";

            try {

                JsonReader jsonReader = Json.createReader(IOUtils.toInputStream(this.sparqlQuery(graph, URLEncoder.encode(sparql, "UTF-8"), "json"), "UTF-8"));

                JsonObject jsonObject = jsonReader.readObject();

                JsonArray bindings = jsonObject.getJsonObject("results").getJsonArray("bindings");

                if (bindings.size() == 0) {

                    accessRights = "internal";
                } else {

                    for (JsonObject binding : bindings.getValuesAs(JsonObject.class)) {

                        accessRights = binding.getJsonObject("o").getJsonString("value").getString();
                    }
                }

                this.logger.info("accessRights: " + accessRights);
            }
            catch (IOException e) {

                this.logger.error("something went wrong", e);
                throw new LinkedDataStorageException(e.getMessage(), e.getCause());
            }
        }

        return accessRights;
    }

    @Override
    public String searchResource(Properties query, String format) throws LinkedDataStorageException {
        return this.searchResource(null, query, format, false);
    }

    @Override
    public String searchResource(Properties query, String format, boolean isAuthorized) throws LinkedDataStorageException {
        return this.searchResource(null, query, format, false);
    }

    @Override
    public String searchResource(String graph, Properties query, String format) throws LinkedDataStorageException {
        return this.searchResource(graph, query, format, false);
    }

    @Override
    public String searchResource(String graph, Properties query, String format, boolean isAuthorized) throws LinkedDataStorageException {

        // TODO implement search

        return null;
    }

    @Override
    public String sparqlQuery(String query, String format) throws LinkedDataStorageException {

        return this.sparqlQuery(null, query, format, false);
    }

    @Override
    public String sparqlQuery(String query, String format, boolean isAuthorized) throws LinkedDataStorageException {

        return this.sparqlQuery(null, query, format, false);
    }

    @Override
    public String sparqlQuery(String graph, String query, String format) throws LinkedDataStorageException {

        return this.sparqlQuery(graph, query, format, false);
    }

    @Override
    public String sparqlQuery(String graph, String queryString, String format, boolean isAuthorized) throws LinkedDataStorageException {

        String resource = null;

        try {

            this.logger.info("sparqlQuery: " + graph);
            this.logger.info("sparqlQuery: " + queryString);
            this.logger.info("sparqlQuery: " + URLDecoder.decode(queryString, "UTF-8"));
            this.logger.info("sparqlQuery: " + format);
            this.logger.info("sparqlQuery: " + isAuthorized);

            if (isAuthorized) {

                if (graph != null) {

                    // TODO config
                    ArrayList<String> graphs = new ArrayList<String>();
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + "main-entities" + "-public");
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + "main-entities" + "-nonpublic");
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + graph + "-public");
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + graph + "-nonpublic");

                    queryString = extendSparqlQueryWithGraph(URLDecoder.decode(queryString, "UTF-8"), graphs);

                    // TODO HttpRequest zu de.tu_dortmund.ub.hb_ng.data.SolRDF - publicQueryString
                    resource = this.postQueryDirectly(queryString, format);

                    this.logger.info(resource);
                }
                else {

                    // TODO HttpRequest zu de.tu_dortmund.ub.hb_ng.data.SolRDF - queryString
                    resource = this.postQueryDirectly(queryString, format);

                    this.logger.info(resource);
                }

            }
            else {

                if (graph != null) {

                    // TODO config
                    ArrayList<String> graphs = new ArrayList<String>();
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + "main-entities" + "-public");
                    graphs.add("http://data.ub.tu-dortmund.de/graph/" + graph + "-public");

                    // TODO extend to public graph
                    queryString = extendSparqlQueryWithGraph(URLDecoder.decode(queryString, "UTF-8"), graphs);
                }

                // TODO HttpRequest zu de.tu_dortmund.ub.hb_ng.data.SolRDF
                resource = this.postQueryDirectly(queryString, format);

                this.logger.info(resource);
            }
        }
        catch (Exception e) {

            e.printStackTrace();
            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting Linked Media Framework " + " - " + e.getMessage());
            throw new LinkedDataStorageException(e.getMessage(), e.getCause());
        }

        return resource;
    }

    @Override
    public String sparqlUpdate(String data) throws LinkedDataStorageException {

        String code = "";

        // de.tu_dortmund.ub.hb_ng.data.SolRDF
        String sparql_url = this.config.getProperty("storage.sparql-endpoint");

        // HTTP Request
        int timeout = Integer.parseInt(this.config.getProperty("storage.timeout"));

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();

        try {

            HttpPost httpPost = new HttpPost(sparql_url);
            httpPost.addHeader("Content-Type", "application/sparql-update");
            httpPost.setEntity(new StringEntity(data));

            CloseableHttpResponse httpResponse = null;

            long start = System.nanoTime();
            try {

                httpResponse = httpclient.execute(httpPost);
            }
            catch (ConnectTimeoutException | SocketTimeoutException e) {

                this.logger.info("[" + this.getClass().getName() + "] " + e.getClass().getName() + ": " + e.getMessage());
                httpResponse = httpclient.execute(httpPost);
            }
            long elapsed = System.nanoTime() - start;
            this.logger.info("[" + this.getClass().getName() + "] de.tu_dortmund.ub.hb_ng.data.SolRDF request - " + (elapsed / 1000.0 / 1000.0 / 1000.0) + " s");

            try {

                int statusCode = httpResponse.getStatusLine().getStatusCode();
                HttpEntity httpEntity = httpResponse.getEntity();

                switch (statusCode) {

                    case 200 : {

                        code = "201";
                        this.logger.info("[" + this.getClass().getName() + "] " + new Date() + " - Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());

                        break;
                    }
                    case 201 : {

                        code = "201";
                        this.logger.info("[" + this.getClass().getName() + "] " + new Date() + " - Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());

                        break;
                    }
                    default: {

                        code = String.valueOf(statusCode);
                        this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());
                    }
                }

                EntityUtils.consume(httpEntity);

            }
            finally {
                httpResponse.close();
            }
        }
        catch (Exception e) {

            this.logger.error("something went wrong ", e);
            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " - timeout");

            throw new LinkedDataStorageException(e.getMessage(), e.getCause());
        }
        finally {

            try {

                httpclient.close();
            }
            catch (IOException e) {

                this.logger.error("something went wrong", e);
                throw new LinkedDataStorageException(e.getMessage(), e.getCause());
            }
        }

        return code;
    }

    private String doResourceRequest(String graph, String uri, String format, boolean isAuthorized) throws LinkedDataStorageException {

        this.logger.info("doResourceRequest: " + graph);
        this.logger.info("doResourceRequest: " + uri);
        this.logger.info("doResourceRequest: " + format);
        this.logger.info("doResourceRequest: " + isAuthorized);

        String result = null;

        try {

            // query resource in de.tu_dortmund.ub.hb_ng.data.SolRDF
            String constructQuery = "CONSTRUCT { <" + uri + "> ?p ?o } WHERE { <" + uri + "> ?p ?o }";

            String resultString = this.sparqlQuery(graph, URLEncoder.encode(constructQuery, "UTF-8"), "xml", isAuthorized);

            // postprocessing
            RDFParser parser = Rio.createParser(RDFFormat.RDFXML);

            ArrayList<Statement> statements = new ArrayList<Statement>();
            StatementCollector collector = new StatementCollector(statements);
            parser.setRDFHandler(collector);

            parser.parse(new StringReader(resultString), this.config.getProperty("resource.baseurl"));

            if (statements == null || statements.size() == 0) {

                result = null;
            }
            else {

                RDFFormat formatString;
                switch (format) {

                    case "html": {

                        formatString = RDFFormat.RDFXML;
                        break;
                    }
                    case "rdf.xml": {

                        formatString = RDFFormat.RDFXML;
                        break;
                    }
                    case "rdf.ttl": {

                        formatString = RDFFormat.TURTLE;
                        break;
                    }
                    case "json": {

                        formatString = RDFFormat.JSONLD;
                        break;
                    }
                    case "nquads": {

                        formatString = RDFFormat.NQUADS;
                        break;
                    }
                    default: {

                        formatString = RDFFormat.NQUADS;
                    }
                }

                StringWriter stringWriter = new StringWriter();
                RDFWriter writer = Rio.createWriter(formatString, stringWriter);
                writer.startRDF();
                for (Statement statement : statements) {
                    writer.handleStatement(statement);
                }
                writer.endRDF();

                result = stringWriter.toString();
            }
        }
        catch (Exception e) {

            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting Linked Media Framework: " + uri + " - " + e.getMessage());
            throw new LinkedDataStorageException(e.getMessage(), e.getCause());
        }

        return result;
    }

    private String doSearchRequest(Properties query, String format) throws LinkedDataStorageException {

        String result = null;

        int timeout = Integer.parseInt(this.config.getProperty("storage.lmf.timeout"));

        try {

            String solr_url = this.config.getProperty("storage.lmf.endpoint.search")+ "q=" + URLEncoder.encode(query.getProperty("q"), "UTF-8");
            if (query.containsKey("start")) {
                solr_url += "&start=" + query.getProperty("start");
            }
            if (query.containsKey("fq")) {
                for (String f : query.getProperty("fq").split(";")) {
                    if (!f.contains(":")) {
                        solr_url += "&fq=" + URLEncoder.encode(f, "UTF-8");
                    }
                    else {
                        String[] facet = f.split(":");
                        solr_url += "&fq=" + facet[0] + ":\"" + URLEncoder.encode(f.replace(facet[0] + ":", "").replaceAll("\"",""), "UTF-8") + "\"";
                    }
                }
            }
            if (query.containsKey("rows")) {
                solr_url += "&rows=" + query.getProperty("rows");
            }
            if (query.containsKey("sort")) {
                solr_url += "&sort=" + query.getProperty("sort");
            }

            switch (format) {

                case "html" : {

                    solr_url += "&wt=xml&indent=true";
                    break;
                }
                case "xml" : {

                    solr_url += "&wt=xml&indent=true";
                    break;
                }
                case "json" : {

                    solr_url += "&wt=json&indent=true";
                    break;
                }
            }

            String solr_params = "&facet=true&facet.field=lmf.type&facet.missing=true&facet.mincount=1";

            solr_url += solr_params;

            this.logger.info("Solr-URL: " + solr_url);

            RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeout)
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .build();

            CloseableHttpClient httpclient = HttpClients.custom()
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .build();

            try {

                HttpGet httpGet = new HttpGet(solr_url);

                CloseableHttpResponse httpResponse = null;

                long start = System.nanoTime();
                try {

                    httpResponse = httpclient.execute(httpGet);
                }
                catch (ConnectTimeoutException | SocketTimeoutException e) {

                    this.logger.info("[" + this.getClass().getName() + "] " + e.getClass().getName() + ": " + e.getMessage());
                    httpResponse = httpclient.execute(httpGet);
                }
                long elapsed = System.nanoTime() - start;
                this.logger.info("[" + this.getClass().getName() + "] lmf request - " + (elapsed / 1000.0 / 1000.0 / 1000.0) + " s");

                try {

                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity httpEntity = httpResponse.getEntity();

                    switch (statusCode) {

                        case 200: {

                            StringWriter writer = new StringWriter();
                            IOUtils.copy(httpEntity.getContent(), writer, "UTF-8");
                            result = writer.toString();

                            break;
                        }
                        default: {

                            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting Linked Media Framework: " + solr_url + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());
                            throw new LinkedDataStorageException(statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());
                        }
                    }

                    EntityUtils.consume(httpEntity);

                }
                finally {
                    httpResponse.close();
                }
            }
            catch (ConnectTimeoutException | SocketTimeoutException e) {

                this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting Linked Media Framework: " + solr_url + " - timeout");
                throw new LinkedDataStorageException(e.getMessage(), e.getCause());
            }
            finally {
                httpclient.close();
            }
        }
        catch (Exception e) {

            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting Linked Media Framework " + " - " + e.getMessage());
            throw new LinkedDataStorageException(e.getMessage(), e.getCause());
        }

        return result;
    }

    private String extendSparqlQueryWithGraph(String queryString, ArrayList<String> graphs) {

        Query query = QueryFactory.create(queryString);

        String extendedQuery = "";

        if (query.isSelectType()) {

            extendedQuery += "SELECT ";
            if (query.isQueryResultStar()) {
                extendedQuery += "* ";
            }
            else {

                for (String var : query.getResultVars()) {

                    extendedQuery += "?" + var + " ";
                }
            }

            extendedQuery += " WHERE { ";

            for (int i = 0; i < graphs.size(); i++) {

                String queryPattern = "{ GRAPH <" + graphs.get(i) + "> " + query.getQueryPattern() + " } ";

                extendedQuery += queryPattern;

                if (i < graphs.size()-1) {

                    extendedQuery += " UNION ";
                }
            }

            extendedQuery += " } ";
        }
        else if (query.isDescribeType()) {

            // TODO DESCRIBE
        }
        else if (query.isAskType()) {

            // TODO ASK
        }
        else if (query.isConstructType()) {

            extendedQuery += "CONSTRUCT { ";

            for (Triple triple : query.getConstructTemplate().getTriples()) {

                extendedQuery += triple.getSubject().isVariable() ? "?" + triple.getSubject().getName() + " " :  "<" + triple.getSubject() + "> ";
                extendedQuery += triple.getPredicate().isVariable() ? "?" + triple.getPredicate().getName() + " " : "<" + triple.getPredicate() + "> " ;
                extendedQuery += triple.getObject().isVariable() ? "?" + triple.getObject().getName() + " " : "<" + triple.getObject() + "> " ;

                extendedQuery += ". ";
            }

            extendedQuery += "} WHERE { ";

            for (int i = 0; i < graphs.size(); i++) {

                String queryPattern = "{ GRAPH <" + graphs.get(i) + "> " + query.getQueryPattern() + " } ";

                extendedQuery += queryPattern;

                if (i < graphs.size()-1) {

                    extendedQuery += " UNION ";
                }
            }

            extendedQuery += " }";
        }
        else {

            // TODO throw Exception!
        }

        return extendedQuery;
    }

    private String postQueryDirectly(String query, String format) throws LinkedDataStorageException {

        String result = null;

        // de.tu_dortmund.ub.hb_ng.data.SolRDF
        String sparql_url = this.config.getProperty("storage.sparql-endpoint");

        // HTTP Request
        int timeout = Integer.parseInt(this.config.getProperty("storage.timeout"));

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();

        try {

            HttpPost httpPost = new HttpPost(sparql_url);
            httpPost.addHeader("Content-Type", "application/sparql-query");
            httpPost.addHeader("Accept", "application/sparql-results+json;charset=UTF-8");
            httpPost.setEntity(new StringEntity(query));

            CloseableHttpResponse httpResponse = null;

            long start = System.nanoTime();
            try {

                httpResponse = httpclient.execute(httpPost);
            }
            catch (ConnectTimeoutException | SocketTimeoutException e) {

                this.logger.info("[" + this.getClass().getName() + "] " + e.getClass().getName() + ": " + e.getMessage());
                httpResponse = httpclient.execute(httpPost);
            }
            long elapsed = System.nanoTime() - start;
            this.logger.info("[" + this.getClass().getName() + "] de.tu_dortmund.ub.hb_ng.data.SolRDF request - " + (elapsed / 1000.0 / 1000.0 / 1000.0) + " s");

            try {

                int statusCode = httpResponse.getStatusLine().getStatusCode();
                HttpEntity httpEntity = httpResponse.getEntity();

                switch (statusCode) {

                    case 200 : {

                        this.logger.info("[" + this.getClass().getName() + "] " + new Date() + " - Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " / " + query + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());

                        StringWriter writer = new StringWriter();
                        IOUtils.copy(httpEntity.getContent(), writer, "UTF-8");
                        result = writer.toString();

                        break;
                    }
                    default: {

                        this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " / " + query + " - " + statusCode + " : " + httpResponse.getStatusLine().getReasonPhrase());
                    }
                }

                EntityUtils.consume(httpEntity);

            }
            finally {
                httpResponse.close();
            }
        }
        catch (Exception e) {

            this.logger.error("something went wrong", e);
            this.logger.error("[" + this.getClass().getName() + "] " + new Date() + " - ERROR: Requesting de.tu_dortmund.ub.hb_ng.data.SolRDF: " + sparql_url + " - timeout");

            throw new LinkedDataStorageException(e.getMessage(), e.getCause());
        }
        finally {

            try {

                httpclient.close();
            }
            catch (IOException e) {

                this.logger.error("something went wrong", e);
                throw new LinkedDataStorageException(e.getMessage(), e.getCause());
            }
        }

        return result;
    }

    private String htmlOutputter(Document doc, String xslt, HashMap<String,String> params) throws IOException {

        String result = null;

        try {

            Processor processor = new Processor(false);
            XsltCompiler xsltCompiler = processor.newXsltCompiler();
            XsltExecutable xsltExecutable = xsltCompiler.compile(new StreamSource(xslt));

            XdmNode source = processor.newDocumentBuilder().build(new JDOMSource( doc ));
            Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");

            StringWriter buffer = new StringWriter();
            out.setOutputWriter(new PrintWriter( buffer ));

            XsltTransformer trans = xsltExecutable.load();
            trans.setInitialContextNode(source);
            trans.setDestination(out);

            if (params != null) {
                for (String p : params.keySet()) {
                    trans.setParameter(new QName(p), new XdmAtomicValue(params.get(p)));
                }
            }

            trans.transform();

            result = buffer.toString();

        } catch (SaxonApiException e) {

            this.logger.error("SaxonApiException: " + e.getMessage());
        }

        return result;
    }

}
