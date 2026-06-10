package com.example.dda_proxy.controller;

import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayOutputStream;

import javax.print.Doc;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@RestController
@CrossOrigin
public class WebController {

    @GetMapping("/template")
    public Document getTemplateByName(@RequestParam String templateName) {
        // Replace the placeholder with your MongoDB deployment's connection string
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("templates");
            MongoCollection<Document> collection = database.getCollection("dataset_templates");
            Document document = collection.find(eq("_id", templateName)).first();
            return document;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        } 
    }

    @PostMapping("/template")
    public String addTemplate(@RequestParam String templateName, @RequestBody Document body) {
        // Replace the placeholder with your MongoDB deployment's connection string
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("templates");
            database.createCollection("dataset_templates");
            MongoCollection<Document> collection = database.getCollection("dataset_templates");

            if (templateName.isBlank()) {
                throw new Exception("templateName must not be empty");
            }

            body.append("_id", templateName);
            collection.insertOne(body);
            return "success";
        } catch (Exception e) {
            System.out.println(e);
            return "error";
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
