package com.aistudio.xide.core.automation

import com.aistudio.xide.core.xero.AnalysisPlan

/**
 * Interface for translating a high-level Xero AnalysisPlan into low-level execution AutomationActions.
 */
interface ActionPlanner {
    /**
     * Translates an AnalysisPlan into a sequence of AutomationActions.
     */
    fun planActions(plan: AnalysisPlan): List<AutomationAction>
}
