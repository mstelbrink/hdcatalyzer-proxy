package com.example.dda_proxy.controller;

import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.bson.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin( origins = "https://dda-web.ddnss.de, http://localhost:5173")
@Component
public class WebController {

    @Value("${mongodb.uri}")
    private String uri;

    @Value("${config.categories.file}")
    private String categoriesFile;

    private String databaseName = "templates";

    private String publicTemplates = "public_templates";
    private String snapshotsCollectionName = "snapshots";

    private final OllamaChatModel chatModel;

    @Autowired
    public WebController(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/templates")
    public List<Document> getTemplates() {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(publicTemplates);
            FindIterable<Document> docs = collection.find();
            List<Document> documents = new ArrayList<>();
            for (Document d : docs) {
                documents.add(d);
            }
            return documents;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        } 
    }

    @GetMapping("/template/{name}")
    public Document getTemplateByName(@PathVariable String name) {
        // Replace the placeholder with your MongoDB deployment's connection string
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(publicTemplates);
            Document document = collection.find(eq("_id", name)).first();
            return document;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        } 
    }

    @PostMapping("/template/{name}")
    public void addTemplate(@PathVariable String name, @RequestBody Document body) {
        // Replace the placeholder with your MongoDB deployment's connection string
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            database.createCollection(publicTemplates);
            MongoCollection<Document> collection = database.getCollection(publicTemplates);

            if (name.isBlank()) {
                throw new Exception("templateName must not be empty");
            }

            body.append("_id", name);
            collection.insertOne(body);
        } catch (Exception e) {
            System.out.println(e);
        } 
    }

    @GetMapping("/snapshot/{uuid}")
    public Document getSnapshot(@PathVariable String uuid, HttpServletResponse response) {
        // Replace the placeholder with your MongoDB deployment's connection string
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(snapshotsCollectionName);
            Document document = collection.find(eq("_id", uuid)).first();

            if (document == null) {
                throw new Exception("Document is empty.");
            }

            return document;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } 
    }

    @PostMapping("/snapshot/{uuid}")
    public void addSnapshot(@PathVariable String uuid, @RequestBody Document body) {
        // Replace the placeholder with your MongoDB deployment's connection string
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            database.createCollection(snapshotsCollectionName);
            MongoCollection<Document> collection = database.getCollection(snapshotsCollectionName);

            if (uuid.isBlank()) {
                throw new Exception("UUID must not be empty");
            }

            body.append("_id", uuid);
            collection.insertOne(body);
        } catch (Exception e) {
            System.out.println(e);
        } 
    }

    @PostMapping("/convert")
    public String convert(@RequestBody Map<String, String> body) {

        Lang type;
        if (body.get("type").equals("application/n-triples")) {
            type = Lang.NTRIPLES;
        } else if (body.get("type").equals("text/turtle")) {
            type = Lang.TURTLE;
        } else if (body.get("type").equals("application/rdf+xml")) {
            type = Lang.RDFXML;
        } else {
            throw new Error("Specified format not supported");
        }

        Model model = ModelFactory.createDefaultModel()
        .read(IOUtils.toInputStream(body.get("content"), "UTF-8"), null, "JSON-LD");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RDFDataMgr.write(out, model, type);

        String content = out.toString();

        return content;
    }

    @PostMapping("/ai/generate")
	public Map<String,String> generate(@RequestBody Map<String, String> body) {

        StringBuilder sb = new StringBuilder();

        if (body.get("sourceLangKey").equals("en")) body.put("sourceLang", "English");
        if (body.get("sourceLangKey").equals("de")) body.put("sourceLang", "German");
        
        if (body.get("targetLangKey").equals("en")) body.put("targetLang", "English");
        if (body.get("targetLangKey").equals("de")) body.put("targetLang", "German");

        sb.append("You are a professional ")
            .append(body.get("sourceLang"))
            .append(" (")
            .append(body.get("sourceLangKey"))
            .append(") to ")
            .append(body.get("targetLang"))
            .append(" (")
            .append(body.get("targetLangKey"))
            .append(") translator. Your goal is to accurately convey the meaning and nuances of the original ")
            .append(body.get("sourceLang"))
            .append(" text while adhering to ")
            .append(body.get("targetLang"))
            .append(" grammar, vocabulary, and cultural sensitivities.\n")
            .append("Produce only the ")
            .append(body.get("targetLang"))
            .append(" translation, without any additional explanations or commentary. Please translate the following ")
            .append(body.get("sourceLang"))
            .append(" text into ")
            .append(body.get("targetLang"))
            .append(":\n\n")
            .append(body.get("text"));

        return Map.of("generation", this.chatModel.call(sb.toString()));
    }

    @GetMapping("/kliniken")
    public List<Map<String, String>> getKliniken() throws Exception {
        ClassPathResource resource = new ClassPathResource("2026-09-01_TVERZ_Export.xml");
        InputStream inputStream = resource.getInputStream();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document document = builder.parse(inputStream);

        List<Map<String, String>> elements = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName("StandortKontaktDaten");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Map<String, String> map = new HashMap<>();
            Element element = (Element) nodeList.item(i);
            map.put("STOID", element.getAttribute("STOID"));
            map.put("Land", element.getAttribute("Land"));
            map.put("Name", element.getAttribute("Name"));
            map.put("Telefon", element.getAttribute("Telefon"));
            map.put("EMail", element.getAttribute("EMail"));
            map.put("URL", element.getAttribute("URL"));
            elements.add(map);
        }
        return elements;
    }

    @GetMapping("/categories")
    public Document getCategories() throws Exception {
        Resource resource = new FileSystemResource(categoriesFile);
        InputStream inputStream = resource.getInputStream();

        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8")); 
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
            responseStrBuilder.append(inputStr);

        Document document = Document.parse(responseStrBuilder.toString());

        return document;
    }
}
