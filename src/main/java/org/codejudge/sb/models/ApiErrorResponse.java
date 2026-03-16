package org.codejudge.sb.models;

/**
 * Uniform error envelope returned by all exception handlers.
 *
 * <pre>
 * {
 *   "status": "failure",
 *   "reason": "Parallel File Processing count must be greater than zero!"
 * }
 * </pre>
 */
public record ApiErrorResponse(String status, String reason) {

    /** Convenience factory — always uses "failure" as the status. */
    public static ApiErrorResponse failure(String reason) {
        return new ApiErrorResponse("failure", reason);
    }
}
