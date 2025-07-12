package web.controller;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import core.parser.dictionary.INFOREDictionary;
import core.parser.network.AvailablePlatform;
import core.parser.network.INFORENetwork;
import core.parser.network.Site;
import core.parser.workflow.OptimizationRequest;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import web.document.INFOREDictionaryDocument;
import web.document.INFORENetworkDocument;
import web.document.OptimizationRequestDocument;
import web.repository.DictionaryRepository;
import web.repository.NetworkRepository;
import web.repository.OptimizerRequestRepository;
import web.service.OptimizerService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/optimizer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
@Log
public class OptimizerRESTController {

    @Autowired
    private OptimizerService optimizerService;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OptimizerRequestRepository requestRepository;

    @PostConstruct
    private void init() {
        log.info("OptimizerRESTController service is up and running.");
    }

    //PUT
    @PutMapping(value = "/dictionary")
    @ResponseStatus(HttpStatus.CREATED)
    public String putDictionary(@NotEmpty @RequestBody String dictionaryPayload) {
        INFOREDictionary dictionary = new INFOREDictionary(dictionaryPayload);
        Set<String> operatorNames = dictionary.getOperatorPlatformOperatorNames().keySet();
        for (String operatorName : operatorNames) {
            Map<String, List<String>> implementations = dictionary.getImplementationsForClassKey(operatorName);
            System.out.println("Implementations for operator: " + operatorName);
            for (Map.Entry<String, List<String>> entry : implementations.entrySet()) {
                String site = entry.getKey();
                List<String> platforms = entry.getValue();
                System.out.println("  Site: " + site + ", Platforms: " + String.join(", ", platforms));
            }
            System.out.println();
        }
        // Print site static cost
//        System.out.println("--------------------------");
//        for (Map.Entry<String, Map<String, Integer>> entry : dictionary.getOperatorSiteStaticCosts().entrySet()) {
//            System.out.println("Operator: " + entry.getKey());
//            for (Map.Entry<String, Integer> innerEntry : entry.getValue().entrySet()) {
//                System.out.println("Site: " + innerEntry.getKey() + ", Static Cost: " + innerEntry.getValue());
//            }
//        }
        boolean previous = dictionaryRepository.existsById(dictionary.getName());
        INFOREDictionaryDocument uploadedDictionary = dictionaryRepository.save(new INFOREDictionaryDocument(dictionary));
        log.info(String.format("Uploaded dictionary document with ID=[%s]", uploadedDictionary.getId()));
        return String.format("{\"action\":\"%s\"}", previous ? "Updated" : "Created");
    }

    @PutMapping(value = "/network")
    @ResponseStatus(HttpStatus.CREATED)
    public String putNetworkJSON(@Valid @RequestBody INFORENetwork inforeNetwork) {
        boolean previous = networkRepository.existsById(inforeNetwork.getNetwork());
        INFORENetworkDocument uploadedNetwork = networkRepository.save(new INFORENetworkDocument(inforeNetwork));
        System.out.println("--------------------------");
        System.out.println("Available sites: " + inforeNetwork.getSites().stream().map(Site::getSiteName).reduce((a, b) -> a + ", " + b).orElse("None"));
        System.out.println("Available platforms: " + inforeNetwork.getPlatforms().stream().map(AvailablePlatform::getPlatformName).reduce((a, b) -> a + ", " + b).orElse("None"));
        log.info(String.format("Uploaded network document with ID=[%s]", uploadedNetwork.getId()));
        return String.format("{\"action\":\"%s\"}", previous ? "Updated" : "Created");
    }

    //POST
    @PostMapping(value = "/submit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String putRequestDocument(@Valid @RequestBody OptimizationRequest optimizationRequest, Principal principal) {
        OptimizationRequestDocument document = new OptimizationRequestDocument(optimizationRequest, principal);
        String docId = this.requestRepository.save(document).getId();
        this.optimizerService.submitRequestWithId(principal, docId);
        log.config(String.format("Submitted optimization request for ID=[%s].", docId));
        return String.format("{\"id\":\"%s\"}", docId);
    }

    @PostMapping(value = "/cancel")
    @ResponseStatus(HttpStatus.OK)
    public boolean cancelSubmittedJob(@RequestBody String cancelRequest) {
        //Returns false if the task could not be cancelled, typically because it has already completed normally; true otherwise
        try {
            String requestId = JsonParser.parseString(cancelRequest).getAsJsonObject().get("id").getAsString();
            boolean canceled = this.optimizerService.cancelRequestWithId(requestId);
            log.config(String.format("Cancel result for and requestID=[%s] is [%s]", requestId, canceled));
            return canceled;
        } catch (Exception e) {
            throw new JsonParseException("Field 'id' not present.");
        }
    }

    //DELETE
    @DeleteMapping("/network/{networkId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteNetwork(@NotEmpty @PathVariable String networkId) {
        if (networkRepository.existsById(networkId)) {
            networkRepository.deleteById(networkId);
            log.info(String.format("Deleted network document with ID=[%s]", networkId));
        } else {
            throw new ResourceNotFoundException(String.format("Network with networkName={%s} not found.", networkId));
        }
    }

    @DeleteMapping("/dictionary/{dictionaryId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteDictionary(@NotEmpty @PathVariable String dictionaryId) {
        if (dictionaryRepository.existsById(dictionaryId)) {
            dictionaryRepository.deleteById(dictionaryId);
            log.info(String.format("Deleted dictionary document with ID=[%s]", dictionaryId));
        } else {
            throw new ResourceNotFoundException(String.format("Dictionary with dictionaryName={%s} not found.", dictionaryId));
        }
    }
}
