package com.wams.testsupport;

import com.wams.exception.BusinessRuleException;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Test-only controller used by {@code GlobalExceptionHandlerIntegrationTest}.
 * Located in src/test/java so it is NOT shipped in the production classpath.
 * Component-scanned by {@code @SpringBootTest} because the package is under
 * {@code com.wams}, which is the main class's scan root.
 */
@RestController
@RequestMapping("/__test__")
public class ExceptionThrowingTestController {

    @GetMapping("/response-status/{code}")
    public void responseStatus(@PathVariable int code,
                               @RequestParam(defaultValue = "forced") String reason) {
        throw new ResponseStatusException(HttpStatus.valueOf(code), reason);
    }

    @GetMapping("/resource-not-found")
    public void resourceNotFound() {
        throw new ResourceNotFoundException("forced not found");
    }

    @GetMapping("/duplicate")
    public void duplicate() {
        throw new DuplicateResourceException("forced duplicate");
    }

    @GetMapping("/business-rule")
    public void businessRule() {
        throw new BusinessRuleException("forced business rule violation");
    }

    @GetMapping("/boom")
    public void boom() {
        // Any unexpected exception should be masked to a generic 500.
        throw new IllegalStateException("internal detail that must not leak");
    }
}
