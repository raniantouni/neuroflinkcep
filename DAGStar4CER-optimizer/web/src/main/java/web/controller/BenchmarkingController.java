package web.controller;

import core.parser.benchmarking.BenchmarkingRequest;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import web.document.BenchmarkingRequestDocument;
import web.repository.BenchmarkingRequestRepository;
import web.service.FileServerService;

import javax.validation.Valid;

@RestController
@RequestMapping("/benchmarking")
@ResponseStatus(HttpStatus.OK)
@ResponseBody
@Log
public class BenchmarkingController {
    @Autowired
    private BenchmarkingRequestRepository benchmarkingRequestRepository;

    @Autowired
    private FileServerService fileServerService;

    //Ensure that benchmarking request resources are VALID before saving the request to a persistent storage
    @PostMapping(value = "/submit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String postBenchmarkingRequest(@Valid @RequestBody BenchmarkingRequest benchmarkingRequest) {
        if (!this.fileServerService.checkIfFileExists(benchmarkingRequest.getJarFilePath())) {
            throw new ResourceNotFoundException(String.format("The following Benchmarking request resource is not present [%s].", benchmarkingRequest.getJarFilePath()));
        }
        BenchmarkingRequestDocument benchmarkingRequestDocument = benchmarkingRequestRepository.save(new BenchmarkingRequestDocument(benchmarkingRequest));
        log.info(String.format("Uploaded benchmarking request document with ID=[%s]", benchmarkingRequestDocument.getId()));
        return String.format("{\"id\":\"%s\"}", benchmarkingRequestDocument.id());
    }
}
