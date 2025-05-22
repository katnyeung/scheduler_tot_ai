package com.tot.controller;

import com.tot.service.PerplexityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActionControllerTest {

    @Mock
    private PerplexityService perplexityService;

    @InjectMocks
    private ActionController actionController;

    @BeforeEach
    void setUp() {
        // MockitoAnnotations.openMocks(this) is not needed when using @ExtendWith(MockitoExtension.class)
        // actionController = new ActionController(perplexityService); // This is handled by @InjectMocks
    }

    @Test
    void testExecuteAction_success() {
        // 1. Define a sample totData JSON string
        String totData = "{ \"nodeId\": \"root\", \"value\": 10, \"children\": [] }";

        // 2. Construct the expected prompt
        String expectedPrompt = "please walk through the attached tree, and give me a true or false response " + totData;

        // 3. Define an expected response string from Perplexity
        String expectedResponse = "true";

        // 4. Use Mockito.when() to mock the perplexityService.generateCompletion() call
        when(perplexityService.generateCompletion(expectedPrompt)).thenReturn(expectedResponse);

        // 5. Call actionController.executeAction(totData)
        ResponseEntity<String> responseEntity = actionController.executeAction(totData);

        // 6. Assert that the ResponseEntity<String> returned by executeAction is not null,
        //    has a status of HttpStatus.OK, and its body is equal to the expectedResponse.
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());

        // 7. Use Mockito.verify() to ensure that perplexityService.generateCompletion()
        //    was called exactly once with the expectedPrompt.
        verify(perplexityService).generateCompletion(expectedPrompt);
    }

    @Test
    void testExecuteAction_perplexityServiceReturnsError() {
        // 1. Define a sample totData JSON string
        String totData = "{ \"nodeId\": \"error_case\", \"value\": -1, \"children\": [] }";

        // 2. Construct the expected prompt
        String expectedPrompt = "please walk through the attached tree, and give me a true or false response " + totData;

        // 3. Define an error response string from Perplexity
        String errorResponseFromService = "Error: Perplexity API unavailable";

        // 4. Use Mockito.when() to mock the perplexityService.generateCompletion() call
        when(perplexityService.generateCompletion(expectedPrompt)).thenReturn(errorResponseFromService);

        // 5. Call actionController.executeAction(totData)
        ResponseEntity<String> responseEntity = actionController.executeAction(totData);

        // 6. Assert that the ResponseEntity<String> returned by executeAction is not null,
        //    has a status of HttpStatus.INTERNAL_SERVER_ERROR, and its body is equal to the errorResponseFromService.
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(errorResponseFromService, responseEntity.getBody());

        // 7. Use Mockito.verify() to ensure that perplexityService.generateCompletion()
        //    was called exactly once with the expectedPrompt.
        verify(perplexityService).generateCompletion(expectedPrompt);
    }
}
