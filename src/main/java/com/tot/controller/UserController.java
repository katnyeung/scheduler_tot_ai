package com.tot.controller;


import com.tot.entity.TotNode;
import com.tot.repository.TotNodeRepository;
import com.tot.service.TotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tot")
@Tag(name = "Tree of Thought", description = "Manage the Tree of Thought (ToT)")
public class UserController {
    private final TotService totService;
    private final TotNodeRepository totNodeRepository;

    @Autowired
    public UserController(TotService totService, TotNodeRepository totNodeRepository) {
        this.totService = totService;
        this.totNodeRepository = totNodeRepository;
    }

    @PostMapping("/update")
    @Operation(summary = "Update TOT", description = "Create or update your Tree of Thought structure using LLM")
    public ResponseEntity<String> manageTot(@RequestBody String prompt) {
        // Call the TotService which uses the Spring AI OpenAI library.
        // For example, this call may attach a custom header ("X-Custom-Header") and process totData via LLM.
        String updatedTotJson = totService.queryLLM(prompt);

        return ResponseEntity.ok("TOT updated: " + updatedTotJson);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview TOT", description = "Preview the Tree of Thought structure and validate format")
    public ResponseEntity<String> previewTot(@RequestBody String totData) {
        // Use the TotService to validate and generate a JSON preview.
        String previewJson = totService.previewTot(totData);
        return ResponseEntity.ok("TOT preview: " + previewJson);
    }

    @PostMapping("/save")
    @Operation(summary = "Save TOT", description = "Save the Tree of Thought structure into the vector DB")
    public ResponseEntity<String> saveTot(@RequestBody TotNode totNode) {
        totNodeRepository.save(totNode);
        return ResponseEntity.ok("TOT saved: " + totNode.getNodeId());
    }
}
