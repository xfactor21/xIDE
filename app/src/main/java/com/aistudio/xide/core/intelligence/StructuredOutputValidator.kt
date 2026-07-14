package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.command.ActionDescriptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MalformedOutputException(message: String) : Exception(message)
class HallucinatedCapabilityException(message: String) : Exception(message)
class UnsafeActionException(message: String) : Exception(message)

interface StructuredOutputValidator {
    /**
     * Parses the LLM text output into an ActionPlan, performing strict checks
     * for malformed structure, hallucinated capabilities, and unsafe args.
     */
    @Throws(MalformedOutputException::class, HallucinatedCapabilityException::class, UnsafeActionException::class)
    fun validateAndParse(text: String, allowedCapabilities: Set<String>): ActionPlan
}

class DefaultStructuredOutputValidator : StructuredOutputValidator {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val actionPlanAdapter = moshi.adapter(ActionPlan::class.java)

    override fun validateAndParse(text: String, allowedCapabilities: Set<String>): ActionPlan {
        val cleanText = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val plan = try {
            actionPlanAdapter.fromJson(cleanText)
        } catch (e: Exception) {
            throw MalformedOutputException("Failed to deserialize ActionPlan JSON: ${e.message}")
        } ?: throw MalformedOutputException("ActionPlan JSON resolved to null.")

        if (plan.title.isBlank()) {
            throw MalformedOutputException("ActionPlan contains an empty or blank title.")
        }

        for (descriptor in plan.commands) {
            if (descriptor.commandId.isBlank()) {
                throw MalformedOutputException("ActionDescriptor contains an empty or blank command ID.")
            }
            if (descriptor.description.isBlank()) {
                throw MalformedOutputException("ActionDescriptor contains an empty or blank description.")
            }

            // Failure handling: Hallucinated capabilities
            if (!allowedCapabilities.contains(descriptor.commandId)) {
                throw HallucinatedCapabilityException("Proposed command ID '${descriptor.commandId}' is not a registered capability in xIDE.")
            }

            // Failure handling: Unsafe action proposals (e.g. malicious terminal execution patterns)
            if (descriptor.commandId == "automation.run_terminal") {
                val commandArg = descriptor.arguments["command"] ?: ""
                val unsafePatterns = listOf(
                    "rm -rf /",
                    ":(){ :|:& };:",
                    "mkfs",
                    "dd if=",
                    "> /dev/sda",
                    "chmod -R 777 /",
                    "chown -R"
                )
                for (pattern in unsafePatterns) {
                    if (commandArg.contains(pattern)) {
                        throw UnsafeActionException("Unsafe terminal execution pattern detected in proposed command: '$commandArg'")
                    }
                }
            }
        }

        return plan
    }
}
