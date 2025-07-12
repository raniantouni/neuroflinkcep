package web.controller;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.exception.OptimizerException;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.INFORENetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.document.INFOREDictionaryDocument;
import web.document.INFORENetworkDocument;
import web.document.OptimizationRequestDocument;
import web.document.OptimizerResponseDocument;
import web.repository.*;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/info")
@ResponseStatus(HttpStatus.OK)
public class InfoController {
    private final ZoneId zoneID = ZoneId.of("Europe/Athens");
    private Gson gson;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OptimizerRequestRepository optimizerRequestRepository;

    @Autowired
    private OptimizerResultRepository optimizerResultRepository;

    @PostConstruct
    private void postConstruct() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @GetMapping("/echo/{value}")
    public String echo(@PathVariable String value) {
        return value;
    }

    @GetMapping("/logs/{log_file}")
    public ResponseEntity<InputStreamResource> getLogs(@PathVariable String log_file) throws OptimizerException {
        File file = new File(log_file);
        try {
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s.log", log_file))
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            throw new OptimizerException("Logs not found.");
        }
    }

    @GetMapping("/time")
    public String time() {
        return ZonedDateTime.ofInstant(Instant.now(), zoneID).toString();
    }

    //GET all items
    @GetMapping(value = "/networks", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllNetwork() {
        List<INFORENetwork> res = new ArrayList<>();
        networkRepository.findAll().forEach(m -> res.add(m.getObject()));
        return gson.toJson(res);
    }

    @GetMapping(value = "/dictionaries", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllDictionaries() {
        List<INFOREDictionary> res = new ArrayList<>();
        dictionaryRepository.findAll().forEach(m -> res.add(m.getObject()));
        return gson.toJson(res);
    }

    @GetMapping(value = "/requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllRequests() {
        List<OptimizationRequestDocument> res = new ArrayList<>();
        optimizerRequestRepository.findAll().forEach(res::add);
        return gson.toJson(res);
    }
    @GetMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllResponses() {
        List<OptimizerResponseDocument> res = new ArrayList<>();
        optimizerResultRepository.findAll().forEach(res::add);
        return gson.toJson(res);
    }

    //GET with IDs
    @GetMapping(value = "/network/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getNetwork(@NotEmpty @PathVariable String id) {
        INFORENetwork res = networkRepository
                .findById(id)
                .map(INFORENetworkDocument::getObject)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Network with ID={%s} not found.", id)));
        return gson.toJson(res);
    }

    @GetMapping(value = "/dictionary/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDictionary(@NotEmpty @PathVariable String id) {
        INFOREDictionary res = dictionaryRepository
                .findById(id)
                .map(INFOREDictionaryDocument::getObject)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Dictionary with ID={%s} not found.", id)));
        return gson.toJson(res);
    }

    @GetMapping(value = "/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getRequest(@NotEmpty @PathVariable String id) {
        OptimizationRequestDocument res = optimizerRequestRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Optimizer Request with ID={%s} not found.", id)));
        return gson.toJson(res);
    }

    @GetMapping(value = "/response/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getResponse(@NotEmpty @PathVariable String id) {
        OptimizerResponseDocument res = optimizerResultRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Optimizer Response with ID={%s} not found.", id)));
        return gson.toJson(res);
    }
}
