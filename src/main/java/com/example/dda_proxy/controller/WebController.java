package com.example.dda_proxy.controller;

import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@RestController
@CrossOrigin
@Component
public class WebController {

    @Value("${mongodb.uri}")
    private String uri;

    private String databaseName = "templates";

    private String publicTemplates = "public_templates";

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

    @PostMapping("/jsonld-to-ttl")
    public String convertJsonLdToTurtle(@RequestBody String data) {

        Model model = ModelFactory.createDefaultModel()
        .read(IOUtils.toInputStream(data, "UTF-8"), null, "JSON-LD");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RDFDataMgr.write(out, model, Lang.TURTLE);

        String turtle = out.toString();

        return turtle;
    }
}
