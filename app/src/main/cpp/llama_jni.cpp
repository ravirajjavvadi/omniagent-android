#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <atomic>
#include "llama.h"

#include <mutex>

#define TAG "OmniAgent-Llama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global model state (singleton - loaded once)
static llama_model * g_model = nullptr;
static llama_context * g_ctx = nullptr;
static JavaVM * g_jvm = nullptr; // Store JVM for cross-thread callbacks
static char g_loaded_path[2048] = {0}; // Track currently loaded model path
static std::mutex g_mutex; // Protect singleton access
static std::atomic<bool> g_stop_requested(false); // Flag to stop inference

// Called when the library is first loaded
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    LOGI("JNI_OnLoad called, JavaVM stored");
    llama_backend_init();
    
    // BEAST MODE: Initialize NUMA/CPU Affinity for multicore performance
    llama_numa_init(GGML_NUMA_STRATEGY_DISTRIBUTE);
    
    LOGI("Llama backend and NUMA initialized");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    llama_backend_free();
    LOGI("JNI_OnUnload: resources cleaned up");
}

// ============================================================
// Load Model JNI
// ============================================================
extern "C" JNIEXPORT jboolean JNICALL
Java_com_omniagent_app_engine_LlamaEngine_loadModelJNI(JNIEnv *env, jobject thiz, jstring path) {
    const char * model_path = env->GetStringUTFChars(path, 0);
    LOGI("Loading model from: %s", model_path);

    // Skip re-loading if same model path already loaded
    if (g_model != nullptr && g_ctx != nullptr && strcmp(g_loaded_path, model_path) == 0) {
        LOGI("Model already loaded, skipping reload");
        env->ReleaseStringUTFChars(path, model_path);
        return true;
    }

    // Clean up existing resources
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
        LOGI("Freed previous context");
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
        LOGI("Freed previous model");
    }

    // Load model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU-only for Android
    g_model = llama_load_model_from_file(model_path, model_params);
    
    if (g_model == nullptr) {
        LOGE("Failed to load model from: %s", model_path);
        env->ReleaseStringUTFChars(path, model_path);
        return false;
    }
    LOGI("Model file loaded successfully");
    g_stop_requested = false;

    // Create context — 4096 for unlimited long responses without hitting context limit
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 4096;
    ctx_params.n_threads = 10;       // Beast Mode: 10 threads for multi-core performance
    ctx_params.n_threads_batch = 10; // Optimized for ultra-fast prompt processing

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (g_ctx == nullptr) {
        LOGE("Failed to create llama context");
        llama_free_model(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(path, model_path);
        return false;
    }

    // Store the loaded path so we can skip re-loads
    strncpy(g_loaded_path, model_path, sizeof(g_loaded_path) - 1);
    env->ReleaseStringUTFChars(path, model_path);

    LOGI("Model and context initialized successfully. n_ctx=4096, threads=10 (Beast Mode)");
    return true;
}

// ============================================================
// Build Quality Sampler Chain
// Temperature(0.7) + Top-P(0.9) + Min-P(0.05) + Rep-Penalty(1.1)
// ============================================================
static llama_sampler * build_sampler_chain() {
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler * chain = llama_sampler_chain_init(sparams);

    // 1. Repetition penalty 
    llama_sampler_chain_add(chain, llama_sampler_init_penalties(
        llama_n_vocab(g_model),        // n_vocab
        llama_token_eos(g_model),      // special_eos_id
        llama_token_nl(g_model),       // linefeed_id
        64,                            // penalty_last_n
        1.1f,                          // penalty_repeat
        0.0f,                          // penalty_freq
        0.0f,                          // penalty_present
        false,                         // penalize_nl
        false                          // ignore_eos
    ));

    // 2. Temperature
    llama_sampler_chain_add(chain, llama_sampler_init_temp(0.7f));

    // 3. Top-P nucleus sampling
    llama_sampler_chain_add(chain, llama_sampler_init_top_p(0.9f, 1));

    // 4. Min-P filter
    llama_sampler_chain_add(chain, llama_sampler_init_min_p(0.05f, 1));

    // 5. Final distribution sampler
    llama_sampler_chain_add(chain, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    return chain;
}

// ============================================================
// Streaming Response JNI (with JVM thread attachment for safety)
// ============================================================
extern "C" JNIEXPORT jboolean JNICALL
Java_com_omniagent_app_engine_LlamaEngine_generateStreamingResponseJNI(
        JNIEnv *env, jobject thiz_original, jstring prompt, jint max_tokens_limit) {

    std::lock_guard<std::mutex> lock(g_mutex); // Prevent concurrent inference
    g_stop_requested = false; // Reset stop flag before starting

    if (g_ctx == nullptr || g_model == nullptr) {
        LOGE("Streaming failed: Model/Context not initialized");
        return false;
    }

    if (g_jvm == nullptr) {
        LOGE("JavaVM pointer is null — JNI_OnLoad may not have fired");
        return false;
    }

    // Get method IDs on the calling thread
    jclass clazz = env->GetObjectClass(thiz_original);
    jmethodID methodId = env->GetMethodID(clazz, "onNativeToken", "(Ljava/lang/String;)V");

    jobject thiz = env->NewGlobalRef(thiz_original);

    // Tokenize the prompt
    const char *prompt_text = env->GetStringUTFChars(prompt, 0);
    std::vector<llama_token> tokens;
    int n_tokens_req = -llama_tokenize(g_model, prompt_text, strlen(prompt_text), nullptr, 0, true, true);
    tokens.resize(n_tokens_req);
    llama_tokenize(g_model, prompt_text, strlen(prompt_text), tokens.data(), tokens.size(), true, true);
    env->ReleaseStringUTFChars(prompt, prompt_text);

    llama_kv_cache_clear(g_ctx);

    llama_sampler * sampler = build_sampler_chain();

    const int64_t start_time = llama_time_us();
    const int32_t n_max_tokens = max_tokens_limit;
    llama_token new_token_id = 0;
    int32_t n_decode = 0;
    int n_pos = 0;

    LOGI("FAST Streaming started. Tokens budget: %d", n_max_tokens);

    // CRITICAL SPEED FIX: Attach JVM thread ONCE before the loop.
    // Previously, AttachCurrentThread was called for EVERY token — catastrophic overhead.
    JNIEnv *callback_env = nullptr;
    bool attached = false;
    jint attach_status = g_jvm->GetEnv((void **)&callback_env, JNI_VERSION_1_6);
    if (attach_status == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&callback_env, nullptr) == JNI_OK) {
            attached = true;
            LOGI("JVM thread attached ONCE for entire token stream");
        }
    } else {
        callback_env = env; // Already on the correct thread
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());

    for (; n_decode < n_max_tokens; n_decode++) {
        if (g_stop_requested.load()) break;
        if (n_pos + (int)batch.n_tokens > llama_n_ctx(g_ctx)) break;
        if (llama_decode(g_ctx, batch) != 0) break;
        n_pos += batch.n_tokens;

        new_token_id = llama_sampler_sample(sampler, g_ctx, -1);
        llama_sampler_accept(sampler, new_token_id);

        if (llama_token_is_eog(g_model, new_token_id)) break;

        char buf[256] = {0};
        int piece_len = llama_token_to_piece(g_model, new_token_id, buf, sizeof(buf) - 1, 0, true);

        if (piece_len > 0 && callback_env) {
            buf[piece_len] = '\0';
            jstring j_piece = callback_env->NewStringUTF(buf);
            callback_env->CallVoidMethod(thiz, methodId, j_piece);
            callback_env->DeleteLocalRef(j_piece);
        }
        batch = llama_batch_get_one(&new_token_id, 1);
    }

    if (attached) g_jvm->DetachCurrentThread(); // Detach ONCE at the end

    llama_sampler_free(sampler);
    env->DeleteGlobalRef(thiz);
    LOGI("FAST Stream done in %lld ms. Tokens: %d", (long long)((llama_time_us() - start_time)/1000), n_decode);
    return (n_decode > 0);
}


// ============================================================
// Non-Streaming Response JNI (kept for fallback)
// ============================================================
extern "C" JNIEXPORT jstring JNICALL
Java_com_omniagent_app_engine_LlamaEngine_generateResponseJNI(JNIEnv *env, jobject thiz, jstring prompt) {
    if (g_ctx == nullptr || g_model == nullptr) {
        return env->NewStringUTF("Error: AI model not loaded. Please download a model first.");
    }

    const char *prompt_text = env->GetStringUTFChars(prompt, 0);
    std::vector<llama_token> tokens;
    int n_tokens = -llama_tokenize(g_model, prompt_text, strlen(prompt_text), nullptr, 0, true, true);
    if (n_tokens <= 0) {
        env->ReleaseStringUTFChars(prompt, prompt_text);
        return env->NewStringUTF("Error: Could not tokenize prompt.");
    }
    tokens.resize(n_tokens);
    llama_tokenize(g_model, prompt_text, strlen(prompt_text), tokens.data(), tokens.size(), true, true);
    env->ReleaseStringUTFChars(prompt, prompt_text);

    llama_kv_cache_clear(g_ctx);
    llama_sampler *sampler = build_sampler_chain();
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());

    std::string response;
    llama_token new_token_id = 0;

    for (int i = 0; i < 512; i++) {
        if (llama_decode(g_ctx, batch) != 0) break;
        new_token_id = llama_sampler_sample(sampler, g_ctx, -1);
        llama_sampler_accept(sampler, new_token_id);
        if (llama_token_is_eog(g_model, new_token_id)) break;
        char buf[256] = {0};
        int n = llama_token_to_piece(g_model, new_token_id, buf, sizeof(buf) - 1, 0, true);
        if (n > 0) { buf[n] = '\0'; response += buf; }
        batch = llama_batch_get_one(&new_token_id, 1);
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(response.empty() ? "Could not generate a response." : response.c_str());
}

// ============================================================
// Stop Inference JNI
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_com_omniagent_app_engine_LlamaEngine_stopInferenceJNI(JNIEnv *env, jobject thiz) {
    g_stop_requested = true;
    LOGI("Stop request received from Kotlin");
}

// ============================================================
// Free Model JNI
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_com_omniagent_app_engine_LlamaEngine_freeModelJNI(JNIEnv *env, jobject thiz) {
    // We intentionally keep the model loaded (singleton) for performance.
    // Call this only when app is destroyed.
    LOGI("freeModelJNI called (keeping model in memory for next query)");
}

// ============================================================
// Force Release JNI (called in onCleared)
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_com_omniagent_app_engine_LlamaEngine_forceReleaseJNI(JNIEnv *env, jobject thiz) {
    LOGI("Force releasing all Llama resources");
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    g_loaded_path[0] = '\0';
    LOGI("Resources released");
}
