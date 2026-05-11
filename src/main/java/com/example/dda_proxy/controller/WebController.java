package com.example.dda_proxy.controller;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class WebController {

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
