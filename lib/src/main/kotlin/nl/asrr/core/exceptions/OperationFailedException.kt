/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.exceptions

/**
 * An operation that was allowed but could not be carried out in the current state (an external
 * system refused it, a document could not be generated). Answered with 409 and the message, not a
 * 500: a 500 says "our fault, look at the logs", and the logs have nothing for a refusal.
 */
open class OperationFailedException(override val message: String?) : Exception(message)
