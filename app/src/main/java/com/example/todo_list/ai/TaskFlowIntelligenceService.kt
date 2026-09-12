package com.example.todo_list.ai

import android.content.Context
import android.util.Log
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Primary AI service coordinator for TaskFlow Intelligence.
 * Dispatches grounded requests to Gemini 1.5 Flash / 2.0 Flash with low temperature and strict JSON schema,
 * and passes all results through the TaskFlowCrossValidator barrier.
 * Automatically falls back to LocalScheduleOptimizer when offline or unconfigured.
 */
object TaskFlowIntelligenceService {

    private const val TAG = "TaskFlowIntelligence"

    private const val STRICT_SYSTEM_DIRECTIVE = """
You are TaskFlow Intelligence, an on-device personal task and schedule planning AI assistant.
You have direct, real-time read access to the user's TaskFlow database provided in the USER_CONTEXT JSON.

CRITICAL GROUNDING DIRECTIVES:
1. You must answer and generate plans based ONLY on the provided user context.
2. ABSOLUTELY NEVER invent, imagine, or hallucinate tasks, times, deadlines, categories, or statistics that do not exist.
3. When referring to tasks, always use their exact 'taskId' and exact 'title'.
4. If asked about information or a task not present in the context, explicitly say that it is not present in the user's TaskFlow workspace.
5. All times must follow the 12-hour AM/PM format (e.g., '09:30 AM') matching user time conventions.
6. Always output valid JSON strictly conforming to the requested schema.
"""

    /**
     * Generates an intelligent, conflict-free daily schedule plan.
     */
    suspend fun generateSchedulePlan(
        tasks: List<TaskItem>,
        categories: List<TaskListCategory>,
        profile: UserProfile
    ): SchedulePlanResult = withContext(Dispatchers.IO) {
        val apiKey = AiConfigurationManager.apiKey
        val model = AiConfigurationManager.selectedModel

        if (apiKey.isBlank() || model == AiConfigurationManager.MODEL_LOCAL_HEURISTIC) {
            return@withContext LocalScheduleOptimizer.optimizeDailySchedule(tasks, profile)
        }

        try {
            val userContext = TaskFlowContextBuilder.buildUserContext(tasks, categories, profile)
            val prompt = """
$STRICT_SYSTEM_DIRECTIVE

USER_CONTEXT:
$userContext

TASK:
Analyze today's pending tasks, detect any time conflicts or overlaps, and formulate an optimized chronological schedule plan for today.
Recommend an optimal FocusStatus (e.g., '🎯 Deep Work', '⚡ In the Flow', '🚀 Shipping Code', '☕ Coffee Break') based on task load.
Suggest 30-minute buffers or stagger times where conflicts occur.

RESPONSE FORMAT (Valid JSON only, no markdown backticks):
{
  "summary": "Clear, grounded 2-line explanation of the schedule optimization",
  "focusStatusRecommendation": "🎯 Deep Work",
  "slots": [
    {
      "taskId": "exact taskId from context",
      "taskTitle": "exact title from context",
      "originalTime": "exact original time",
      "suggestedTime": "suggested time (hh:mm AM/PM)",
      "category": "exact category",
      "rationale": "specific grounded rationale why this slot is optimal"
    }
  ],
  "conflicts": [
    {
      "timeSlot": "time string",
      "conflictingTaskIds": ["id1", "id2"],
      "conflictingTaskTitles": ["title1", "title2"],
      "resolutionSuggestion": "how to resolve"
    }
  ],
  "gaps": [
    {
      "startTime": "hh:mm AM/PM",
      "endTime": "hh:mm AM/PM",
      "durationMinutes": 60,
      "recommendation": "Rest or focus recommendation"
    }
  ]
}
"""
            val responseText = executeGeminiRequest(apiKey, model, prompt)
            val parsedPlan = parseSchedulePlanJson(responseText)
            // Anti-hallucination validation gatekeeper
            TaskFlowCrossValidator.validateSchedulePlan(parsedPlan, tasks)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini plan generation failed, falling back to local optimizer: ${e.message}")
            if (AiConfigurationManager.isHeuristicFallbackEnabled) {
                LocalScheduleOptimizer.optimizeDailySchedule(tasks, profile)
            } else {
                throw e
            }
        }
    }

    /**
     * Auto-suggests Eisenhower Priorities for all pending tasks with grounded rationales.
     */
    suspend fun suggestPriorities(
        tasks: List<TaskItem>,
        categories: List<TaskListCategory>,
        profile: UserProfile
    ): PrioritySuggestionResult = withContext(Dispatchers.IO) {
        val apiKey = AiConfigurationManager.apiKey
        val model = AiConfigurationManager.selectedModel

        if (apiKey.isBlank() || model == AiConfigurationManager.MODEL_LOCAL_HEURISTIC) {
            return@withContext LocalScheduleOptimizer.suggestPriorities(tasks, profile)
        }

        try {
            val userContext = TaskFlowContextBuilder.buildUserContext(tasks, categories, profile)
            val prompt = """
$STRICT_SYSTEM_DIRECTIVE

USER_CONTEXT:
$userContext

TASK:
Classify each pending task into one of three priorities:
- 'HIGH' (P1: Urgent/Critical, fixed appointments, early high-impact work)
- 'MEDIUM' (P2: Important routines, study milestones, secondary deliverables)
- 'LOW' (P3: Flexible chores, personal errands, casual leisure)

Provide a 1-sentence transparent rationale for each assignment based strictly on the user's data.

RESPONSE FORMAT (Valid JSON only, no markdown backticks):
{
  "summary": "Brief summary of priority breakdown",
  "suggestions": [
    {
      "taskId": "exact taskId from context",
      "taskTitle": "exact title from context",
      "suggestedPriority": "HIGH|MEDIUM|LOW",
      "rationale": "clear grounded explanation"
    }
  ]
}
"""
            val responseText = executeGeminiRequest(apiKey, model, prompt)
            val parsedPriorities = parsePriorityJson(responseText, tasks)
            // Anti-hallucination validation gatekeeper
            TaskFlowCrossValidator.validatePrioritySuggestions(parsedPriorities, tasks)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini priority suggestion failed, falling back to local optimizer: ${e.message}")
            if (AiConfigurationManager.isHeuristicFallbackEnabled) {
                LocalScheduleOptimizer.suggestPriorities(tasks, profile)
            } else {
                throw e
            }
        }
    }

    /**
     * Answers conversational questions about user schedule and tasks.
     */
    suspend fun askAssistant(
        query: String,
        tasks: List<TaskItem>,
        categories: List<TaskListCategory>,
        profile: UserProfile
    ): AssistantChatResult = withContext(Dispatchers.IO) {
        val apiKey = AiConfigurationManager.apiKey
        val model = AiConfigurationManager.selectedModel

        if (apiKey.isBlank() || model == AiConfigurationManager.MODEL_LOCAL_HEURISTIC) {
            return@withContext LocalScheduleOptimizer.answerScheduleQuery(query, tasks, profile)
        }

        try {
            val userContext = TaskFlowContextBuilder.buildUserContext(tasks, categories, profile)
            val prompt = """
$STRICT_SYSTEM_DIRECTIVE

USER_CONTEXT:
$userContext

USER_QUERY:
$query

TASK:
Provide a concise, grounded answer to the user's query based strictly on their tasks, categories, and profile.
Do NOT invent tasks. If referring to tasks, include their exact taskIds in the relevantTaskIds array.

RESPONSE FORMAT (Valid JSON only, no markdown backticks):
{
  "answer": "Concise, helpful response grounded in user data",
  "relevantTaskIds": ["taskId1", "taskId2"],
  "quickFollowUps": ["Follow-up question 1", "Follow-up question 2"]
}
"""
            val responseText = executeGeminiRequest(apiKey, model, prompt)
            val parsedChat = parseChatJson(responseText)
            // Anti-hallucination validation gatekeeper
            TaskFlowCrossValidator.validateChatResult(parsedChat, tasks)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini chat failed, falling back to local optimizer: ${e.message}")
            if (AiConfigurationManager.isHeuristicFallbackEnabled) {
                LocalScheduleOptimizer.answerScheduleQuery(query, tasks, profile)
            } else {
                throw e
            }
        }
    }

    /**
     * Dispatches HTTP request to Google Gemini API.
     */
    private fun executeGeminiRequest(apiKey: String, model: String, prompt: String): String {
        val resolvedModel = if (model.startsWith("gemini")) model else AiConfigurationManager.MODEL_GEMINI_1_5_FLASH
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"
        val url = URL(endpoint)

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply {
                            put("text", prompt)
                        }
                    ))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
                put("topK", 1)
                put("responseMimeType", "application/json")
            })
        }

        connection.outputStream.use { os ->
            os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IllegalStateException("Gemini API error ($responseCode): $err")
        }

        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseBody)
        val candidates = root.getJSONArray("candidates")
        if (candidates.length() == 0) {
            throw IllegalStateException("Empty candidates in Gemini response")
        }
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        return parts.getJSONObject(0).getString("text")
    }

    private fun parseSchedulePlanJson(rawJson: String): SchedulePlanResult {
        val cleanJson = cleanJsonString(rawJson)
        val root = JSONObject(cleanJson)

        val summary = root.optString("summary", "Schedule optimized for today.")
        val focusRec = root.optString("focusStatusRecommendation", "🎯 Deep Work")

        val slotsList = mutableListOf<ScheduledTaskSlot>()
        val slotsArray = root.optJSONArray("slots") ?: JSONArray()
        for (i in 0 until slotsArray.length()) {
            val item = slotsArray.getJSONObject(i)
            val taskId = item.optString("taskId")
            val title = item.optString("taskTitle")
            val origTime = item.optString("originalTime")
            val sugTime = item.optString("suggestedTime", origTime)
            val category = item.optString("category", "General")
            val rationale = item.optString("rationale", "")

            slotsList.add(
                ScheduledTaskSlot(
                    taskId = taskId,
                    taskTitle = title,
                    originalTime = origTime,
                    suggestedTime = sugTime,
                    category = category,
                    rationale = rationale,
                    isTimeChanged = origTime != sugTime
                )
            )
        }

        val conflictsList = mutableListOf<ScheduleConflict>()
        val conflictsArray = root.optJSONArray("conflicts") ?: JSONArray()
        for (i in 0 until conflictsArray.length()) {
            val item = conflictsArray.getJSONObject(i)
            val timeSlot = item.optString("timeSlot")
            val idList = mutableListOf<String>()
            val ids = item.optJSONArray("conflictingTaskIds") ?: JSONArray()
            for (j in 0 until ids.length()) idList.add(ids.getString(j))

            val titleList = mutableListOf<String>()
            val titles = item.optJSONArray("conflictingTaskTitles") ?: JSONArray()
            for (j in 0 until titles.length()) titleList.add(titles.getString(j))

            val res = item.optString("resolutionSuggestion")

            conflictsList.add(
                ScheduleConflict(
                    timeSlot = timeSlot,
                    conflictingTaskIds = idList,
                    conflictingTaskTitles = titleList,
                    resolutionSuggestion = res
                )
            )
        }

        val gapsList = mutableListOf<ScheduleGap>()
        val gapsArray = root.optJSONArray("gaps") ?: JSONArray()
        for (i in 0 until gapsArray.length()) {
            val item = gapsArray.getJSONObject(i)
            val start = item.optString("startTime")
            val end = item.optString("endTime")
            val duration = item.optInt("durationMinutes", 30)
            val rec = item.optString("recommendation")

            gapsList.add(
                ScheduleGap(
                    startTime = start,
                    endTime = end,
                    durationMinutes = duration,
                    recommendation = rec
                )
            )
        }

        return SchedulePlanResult(
            summary = summary,
            focusStatusRecommendation = focusRec,
            slots = slotsList,
            conflicts = conflictsList,
            gaps = gapsList,
            isFallback = false
        )
    }

    private fun parsePriorityJson(rawJson: String, knownTasks: List<TaskItem>): PrioritySuggestionResult {
        val cleanJson = cleanJsonString(rawJson)
        val root = JSONObject(cleanJson)

        val summary = root.optString("summary", "Tasks prioritized based on importance.")
        val suggestionsList = mutableListOf<TaskPrioritySuggestion>()
        val array = root.optJSONArray("suggestions") ?: JSONArray()

        val taskMap = knownTasks.associateBy { it.id }

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val taskId = item.optString("taskId")
            val title = item.optString("taskTitle")
            val prioStr = item.optString("suggestedPriority", "MEDIUM")
            val rationale = item.optString("rationale")

            val matchedTask = taskMap[taskId]
            val currentPrio = matchedTask?.priority ?: "NONE"

            suggestionsList.add(
                TaskPrioritySuggestion(
                    taskId = taskId,
                    taskTitle = title,
                    currentPriority = currentPrio,
                    suggestedPriority = TaskPriority.fromString(prioStr),
                    rationale = rationale
                )
            )
        }

        return PrioritySuggestionResult(
            summary = summary,
            suggestions = suggestionsList,
            isFallback = false
        )
    }

    private fun parseChatJson(rawJson: String): AssistantChatResult {
        val cleanJson = cleanJsonString(rawJson)
        val root = JSONObject(cleanJson)

        val answer = root.optString("answer", "Here is information based on your tasks.")
        val relevantIds = mutableListOf<String>()
        val idsArray = root.optJSONArray("relevantTaskIds") ?: JSONArray()
        for (i in 0 until idsArray.length()) relevantIds.add(idsArray.getString(i))

        val followUps = mutableListOf<String>()
        val fuArray = root.optJSONArray("quickFollowUps") ?: JSONArray()
        for (i in 0 until fuArray.length()) followUps.add(fuArray.getString(i))

        return AssistantChatResult(
            answer = answer,
            relevantTaskIds = relevantIds,
            quickFollowUps = followUps,
            isFallback = false
        )
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
