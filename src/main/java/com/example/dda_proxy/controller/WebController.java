package com.example.dda_proxy.controller;

import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@RestController
@CrossOrigin
public class WebController {

    @PostMapping("/template")
    public String test(@RequestBody Document body) {
        // Replace the placeholder with your MongoDB deployment's connection string
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("templates");
            database.createCollection("dataset_templates");
            MongoCollection<Document> collection = database.getCollection("dataset_templates");
            collection.insertOne(body);
            Document doc = collection.find(eq("password", "123")).first();
            if (doc != null) {
                System.out.println(doc.toJson());
            } else {
                System.out.println("No matching documents found.");
            }
        }
        return null;
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
