cmake_minimum_required(VERSION 3.22.1)

project("omniagent")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# Llama.cpp configuration
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "")
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "")
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "")
set(LLAMA_AVX OFF CACHE BOOL "")
set(LLAMA_AVX2 OFF CACHE BOOL "")
set(LLAMA_FMA OFF CACHE BOOL "")
set(LLAMA_F16C OFF CACHE BOOL "")
set(LLAMA_ACCELERATE OFF CACHE BOOL "")
set(LLAMA_OPENBLAS OFF CACHE BOOL "")
set(LLAMA_CUBLAS OFF CACHE BOOL "")
set(LLAMA_CLBLAST OFF CACHE BOOL "")
set(LLAMA_METAL OFF CACHE BOOL "")

# Add llama.cpp as a subdirectory
add_subdirectory(llama.cpp)

# Create our JNI wrapper library
add_library(
        omnilogic
        SHARED
        llama_jni.cpp
)

# Link our JNI library against llama.cpp and Android logging
target_link_libraries(
        omnilogic
        llama
        log
)
