package com.example.data.model

enum class ModelProvider(val displayName: String) {
    GEMINI("Google Gemini (Multimodal)"),
    OPENAI("OpenAI Vision (GPT-4o)"),
    CLAUDE("Anthropic Claude 3.5"),
    LOCAL("نموذج محلي (GGUF / Llama.cpp)")
}

data class LocalGgufConfig(
    val mainModelPath: String = "",
    val mmprojPath: String = "",
    val isEmbeddedMmproj: Boolean = false,
    val modelName: String = ""
)

data class ModelSettings(
    val provider: ModelProvider = ModelProvider.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val remoteModelId: String = "gemini-2.5-flash",
    val localGguf: LocalGgufConfig = LocalGgufConfig(
        mainModelPath = "/sdcard/models/qwen2.5-vl-7b-instruct.gguf",
        mmprojPath = "/sdcard/models/mmproj-qwen2.5-vl.gguf",
        isEmbeddedMmproj = false,
        modelName = "Qwen2.5-VL-7B-Local"
    ),
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val isMandatoryVisionEnforced: Boolean = true // Strict mandatory enforcement (100% screenshots)
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """أنت مهندس أنظمة خبير ووكيل تحكم مستقل بالجوال (Autonomous Android GUI Agent).
مهمتك تحليل لقطة الشاشة الحالية بدقة هندسية مطلقة وإصدار أوامر تحكم دقيقة بصيغة JSON.
تلتزم دائماً بالرؤية البصرية الإلزامية لكل خطوة، وتوليد عكس الخطوة (Reversal Action) لتمكين نظام التراجع العكسي الشامل."""
    }
}

data class SkillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val toolCallSignature: String,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false
)

data class OverlayConfig(
    val showCoordinatesGrid: Boolean = false,
    val showSetOfMarks: Boolean = true,
    val hideAllOverlays: Boolean = false,
    val gridDivision: Int = 10 // 10x10 normalized grid
)
