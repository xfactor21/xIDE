package com.aistudio.xide.core.automation

import com.aistudio.xide.core.xero.AnalysisPlan
import com.aistudio.xide.core.xero.XeroAction

/**
 * Maps high-level abstract XeroActions from an AnalysisPlan to target AutomationActions.
 */
class ActionPlannerImpl : ActionPlanner {
    override fun planActions(plan: AnalysisPlan): List<AutomationAction> {
        return plan.proposedActions.map { xeroAction ->
            when (xeroAction) {
                is XeroAction.CreateFile -> {
                    AutomationAction.CreateFile(xeroAction.path, xeroAction.content)
                }
                is XeroAction.EditFile -> {
                    AutomationAction.ModifyFile(
                        path = xeroAction.path,
                        targetContent = "", // Semantic target matching to occur in subsequent phases
                        replacementContent = xeroAction.instruction
                    )
                }
                is XeroAction.RunCommand -> {
                    AutomationAction.ExecuteCommand(xeroAction.command)
                }
            }
        }
    }
}
