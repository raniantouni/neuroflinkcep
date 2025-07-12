package web.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.utils.JSONSingleton;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import web.repository.DictionaryRepository;
import web.repository.NetworkRepository;
import web.repository.OptimizerRequestRepository;
import web.repository.OptimizerResultRepository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/admin")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
@Log
public class AdminController {
    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OptimizerRequestRepository optimizerRequestRepository;

    @Autowired
    private OptimizerResultRepository optimizerResultRepository;

    private Gson gson;

    @PostConstruct
    private void postConstruct() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    //Spring container life-cycle mappings
    @GetMapping("/restart")
    public void restart() {
        log.warning("/restart");
        SpringApplication.exit(appContext, () -> 0);
    }

    @GetMapping("/shutdown")
    public int shutdown() {
        log.warning("/shutdown");
        int exit_code = SpringApplication.exit(appContext, () -> 0);
        try {
            return exit_code;
        } finally {
            System.exit(exit_code);
        }
    }

    //DELETE ALL
    @GetMapping("/drop_networks")
    public String deleteAllNetwork() {
        networkRepository.deleteAll();
        return "All networks were deleted.";
    }

    @GetMapping("/drop_dictionaries")
    public String deleteAllDictionaries() {
        dictionaryRepository.deleteAll();
        return "All dictionaries were deleted.";
    }

    @GetMapping("/drop_requests")
    public String deleteAllRequests() {
        optimizerRequestRepository.deleteAll();
        return "All optimizer requests were deleted.";
    }

    @GetMapping("/drop_results")
    public String deleteAllResults() {
        optimizerResultRepository.deleteAll();
        return "All optimizer results were deleted.";
    }

    @GetMapping(value = "/drop_all", produces = MediaType.APPLICATION_JSON_VALUE)
    public String dropAll() {
        log.warning("A /drop_all request has been issued.");
        List<String> list = new ArrayList<>();
        list.add(deleteAllNetwork());
        list.add(deleteAllDictionaries());
        list.add(deleteAllRequests());
        list.add(deleteAllResults());
        return gson.toJson(list);
    }
}
