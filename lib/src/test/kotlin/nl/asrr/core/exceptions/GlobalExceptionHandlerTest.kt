/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.exceptions

import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException

class GlobalExceptionHandlerTest {

    @RestController
    class Thrower {
        @GetMapping("/not-found") fun notFound(): String = throw NotFoundException("Project p1 not found")

        @GetMapping("/failed") fun failed(): String = throw OperationFailedException("Exact refused the invoice")

        @GetMapping("/stale") fun stale(): String = throw OptimisticLockingFailureException("v2 != v3")

        @GetMapping("/require") fun require(): String = throw IllegalArgumentException("size must be between 1 and 10000")

        @GetMapping("/number") fun number(@RequestParam n: Int): String = "$n"

        @GetMapping("/bug") fun bug(): String = throw IllegalStateException("secret internals")

        @GetMapping("/gone") fun gone(): String = throw IOException("Broken pipe")
    }

    private fun mvc(explicit: Boolean = false) = MockMvcBuilders.standaloneSetup(Thrower()).setControllerAdvice(GlobalExceptionHandler(explicit)).build()

    @Test
    fun `refusals and bad input are 4xx, never 500`() {
        val mvc = mvc()
        mvc.get("/not-found").andExpect { status { isNotFound() }; jsonPath("$.detail") { value("Project p1 not found") } }
        mvc.get("/failed").andExpect { status { isConflict() }; jsonPath("$.type") { value("https://errors.asrr.nl/operation-failed") } }
        mvc.get("/stale").andExpect { status { isConflict() } }
        mvc.get("/require").andExpect { status { isBadRequest() } }
        mvc.get("/number?n=abc").andExpect { status { isBadRequest() } }
        mvc.get("/number").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `a real fault is a 500 with a request id and no internals`() {
        mvc().get("/bug").andExpect {
            status { isInternalServerError() }
            jsonPath("$.requestId") { exists() }
            jsonPath("$.detail") { value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret internals"))) }
        }
        mvc(explicit = true).get("/bug").andExpect { jsonPath("$.detail") { value(org.hamcrest.Matchers.containsString("secret internals")) } }
    }

    @Test
    fun `a client that went away gets no 500`() {
        mvc().get("/gone").andExpect { status { isOk() } }
    }
}
