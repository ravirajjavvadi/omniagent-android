cmake_minimum_required(VERSION 3.22.1)

project("omniagent")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# BEAST MODE PERFORMANCE FLAGS
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG -march=armv8-a -flto")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -march=armv8-a -flto")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto")
    message(STATUS "Llama Beast Mode Enabled: ARM64 NEON + O3 + LTO")
endif()

# Llama.cpp Hardware Acceleration
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "")
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "")
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "")
set(GGML_NATIVE ON CACHE BOOL "")
set(LLAMA_LTO ON CACHE BOOL "")

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
