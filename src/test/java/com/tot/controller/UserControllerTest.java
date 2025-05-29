package com.tot.controller;

import com.tot.service.LLMService;
import com.tot.service.TotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private TotService totService;

    @Mock
    private LLMService llmService;

    @InjectMocks
    private UserController userController;

    // Sample JSON for previewTot tests
    private final String mockNodesJson_t1 = "[" +
            "{\"nodeId\":\"root\",\"treeId\":\"t1\",\"content\":\"Root Content\",\"criteria\":\"crit1\",\"children\":{\"left\":\"child1\",\"right\":\"child2\"}}," +
            "{\"nodeId\":\"child1\",\"treeId\":\"t1\",\"content\":\"Left Child\",\"criteria\":\"crit2\",\"children\":{\"grandchild\":\"gc1\"}}," +
            "{\"nodeId\":\"child2\",\"treeId\":\"t1\",\"content\":\"Right Child\",\"criteria\":\"crit3\",\"children\":{}}," +
            "{\"nodeId\":\"gc1\",\"treeId\":\"t1\",\"content\":\"Grand Child\",\"criteria\":\"crit4\",\"children\":{}}" +
            "]";

    private final String mockNodesJson_t2_disconnected = "[" +
            "{\"nodeId\":\"nodeA\",\"treeId\":\"t2\",\"content\":\"Node A Content\",\"criteria\":\"critA\",\"children\":{}}," +
            "{\"nodeId\":\"nodeB\",\"treeId\":\"t2\",\"content\":\"Node B Content\",\"criteria\":\"critB\",\"children\":{}}" +
            "]";
    
    private final String mockNodesJson_t3_otherTree = "[" +
        "{\"nodeId\":\"otherRoot\",\"treeId\":\"t3\",\"content\":\"Other Root Content\",\"children\":{}}" +
    "]";


    @BeforeEach
    void setUp() {
        // Mocks are injected via @InjectMocks
        // If specific per-test setup is needed, it can go here
    }

    // --- Tests for previewTot ---

    @Test
    void testPreviewTot_Success_TreeFound() {
        String treeId = "t1";
        String expectedValidationResult = "Validation OK";
        // Based on the implemented buildPreviewString logic:
        // root: Root Content
        //   left ->
        //     child1: Left Child
        //       grandchild ->
        //         gc1: Grand Child
        //   right ->
        //     child2: Right Child
        String expectedOutput = "root: Root Content\n" +
                                "  left ->\n" +
                                "    child1: Left Child\n" +
                                "      grandchild ->\n" +
                                "        gc1: Grand Child\n" +
                                "  right ->\n" +
                                "    child2: Right Child\n";


        when(totService.getTreeOfThought(treeId)).thenReturn(mockNodesJson_t1);
        when(llmService.validateTree(mockNodesJson_t1)).thenReturn(expectedValidationResult);

        ResponseEntity<String> response = userController.previewTot(treeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedValidationResult, response.getHeaders().getFirst("X-ToT-Validation"));
        assertEquals(expectedOutput, response.getBody());

        verify(totService).getTreeOfThought(treeId);
        verify(llmService).validateTree(mockNodesJson_t1);
    }
    
    @Test
    void testPreviewTot_Success_TreeFound_ButOnlyOneNodeFromDifferentTreeInList() {
        // This test ensures that filtering by treeId in buildPreviewString works
        String treeId = "t1";
        String mixedNodesJson = "[" +
            "{\"nodeId\":\"root\",\"treeId\":\"t1\",\"content\":\"Root Content\",\"children\":{}}," + // Node for t1
            "{\"nodeId\":\"otherNode\",\"treeId\":\"t99\",\"content\":\"Other Tree Node\",\"children\":{}}" + // Node for t99
        "]";
        String expectedValidationResult = "Validation OK";
        String expectedOutput = "root: Root Content\n"; // Only root from t1 should be processed

        when(totService.getTreeOfThought(treeId)).thenReturn(mixedNodesJson); // Service returns mixed list
        when(llmService.validateTree(mixedNodesJson)).thenReturn(expectedValidationResult);

        ResponseEntity<String> response = userController.previewTot(treeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedValidationResult, response.getHeaders().getFirst("X-ToT-Validation"));
        assertEquals(expectedOutput, response.getBody());
        verify(totService).getTreeOfThought(treeId);
        verify(llmService).validateTree(mixedNodesJson);
    }


    @Test
    void testPreviewTot_EmptyTreeJson() {
        String treeId = "emptyTree";
        String emptyJsonArray = "[]";
        String expectedValidationResult = "Validation OK for empty";

        when(totService.getTreeOfThought(treeId)).thenReturn(emptyJsonArray);
        when(llmService.validateTree(emptyJsonArray)).thenReturn(expectedValidationResult);

        ResponseEntity<String> response = userController.previewTot(treeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedValidationResult, response.getHeaders().getFirst("X-ToT-Validation"));
        assertEquals("", response.getBody()); // Expect empty string for empty tree

        verify(totService).getTreeOfThought(treeId);
        verify(llmService).validateTree(emptyJsonArray);
    }

    @Test
    void testPreviewTot_TotServiceThrowsException() {
        String treeId = "errorTree";
        when(totService.getTreeOfThought(treeId)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = userController.previewTot(treeId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error previewing ToT: DB error"));

        verify(totService).getTreeOfThought(treeId);
        verifyNoInteractions(llmService); // LLMService should not be called if fetching fails
    }
    
    @Test
    void testPreviewTot_DeserializationError() {
        String treeId = "badJsonTree";
        String malformedJson = "[{\"nodeId\":\"root\", \"treeId\":\"badJsonTree\""; // Incomplete JSON
        String expectedValidationResult = "Validation OK"; // Assume validation happens before full parsing for preview

        when(totService.getTreeOfThought(treeId)).thenReturn(malformedJson);
        // llmService.validateTree might still be called depending on UserController logic order
        when(llmService.validateTree(malformedJson)).thenReturn(expectedValidationResult);


        ResponseEntity<String> response = userController.previewTot(treeId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error deserializing tree JSON"));

        verify(totService).getTreeOfThought(treeId);
        // verify(llmService).validateTree(malformedJson); // This interaction might or might not occur based on exact code structure
    }

    // --- Tests for refineTot ---

    @Test
    void testRefineTot_Success() {
        String inputJson = "{\"prompt\":\"initial prompt\"}";
        String refinedJson = "[{\"nodeId\":\"refinedRoot\",\"treeId\":\"newTree\",\"content\":\"Refined Content\",\"children\":{}}]";
        String expectedTreeId = "newTree";

        when(llmService.refineTreeOfThought(inputJson)).thenReturn(refinedJson);
        when(totService.saveTreeOfThought(refinedJson)).thenReturn(expectedTreeId);

        ResponseEntity<String> response = userController.refineTot(inputJson);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Successfully refined and saved ToT with treeId: " + expectedTreeId, response.getBody());

        verify(llmService).refineTreeOfThought(inputJson);
        verify(totService).saveTreeOfThought(refinedJson);
    }

    @Test
    void testRefineTot_LlmServiceError() {
        String inputJson = "{\"prompt\":\"another prompt\"}";
        when(llmService.refineTreeOfThought(inputJson)).thenThrow(new RuntimeException("LLM processing failed"));

        ResponseEntity<String> response = userController.refineTot(inputJson);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error refining and saving ToT: LLM processing failed"));

        verify(llmService).refineTreeOfThought(inputJson);
        verifyNoInteractions(totService); // saveTreeOfThought should not be called
    }

    @Test
    void testRefineTot_SaveServiceError() {
        String inputJson = "{\"prompt\":\"good prompt\"}";
        String refinedJson = "[{\"nodeId\":\"refinedRoot2\",\"treeId\":\"treeToFailSave\",\"content\":\"Refined Content To Fail Save\",\"children\":{}}]";

        when(llmService.refineTreeOfThought(inputJson)).thenReturn(refinedJson);
        when(totService.saveTreeOfThought(refinedJson)).thenThrow(new RuntimeException("Database save failed"));

        ResponseEntity<String> response = userController.refineTot(inputJson);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error refining and saving ToT: Database save failed"));

        verify(llmService).refineTreeOfThought(inputJson);
        verify(totService).saveTreeOfThought(refinedJson);
    }
}
