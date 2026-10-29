cmake_minimum_required(VERSION 3.12)
project(multiplayer)

set(CMAKE_CXX_STANDARD 20)

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_definitions(-DVER_x32=true)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_definitions(-DVER_x32=false)
endif()

# Add source files
file(GLOB_RECURSE SOURCES
        ${CMAKE_CURRENT_LIST_DIR}/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/**/**/**/**/*.c*
        ${CMAKE_CURRENT_LIST_DIR}/**/**/**/**/**/**/**/**/**/*.c*
)

# Add include directories
include_directories(
        ${CMAKE_CURRENT_LIST_DIR}
        ${CMAKE_CURRENT_LIST_DIR}/util/
        ${CMAKE_CURRENT_LIST_DIR}/vendor/armhook
        ${CMAKE_CURRENT_LIST_DIR}/vendor/imgui
        ${CMAKE_CURRENT_LIST_DIR}/vendor/
        ${CMAKE_CURRENT_LIST_DIR}/game/RW/
        ${CMAKE_CURRENT_LIST_DIR}/game/Core/
)

list(APPEND SOURCES
        ${CMAKE_CURRENT_LIST_DIR}/diag.cpp
        "game/CSkyBox.cpp"
        ${CMAKE_CURRENT_LIST_DIR}/game/TextRasterizer/TextRasterizer.cpp
        ${CMAKE_CURRENT_LIST_DIR}/game/TextRasterizer/ColouredText.cpp
)

if(NOT "game/CRenderTarget.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/CRenderTarget.cpp)
endif()
if(NOT "game/CGPS.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/CGPS.cpp)
endif()
if(NOT "game/Mobile/MobileSettings/MobileSettings.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/Mobile/MobileSettings/MobileSettings.cpp)
endif()
if(NOT "game/Mobile/MobileMenu/MobileMenu.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/Mobile/MobileMenu/MobileMenu.cpp)
endif()
if(NOT "game/CFirstPersonCamera.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/CFirstPersonCamera.cpp)
endif()
if(NOT "game/CHandlingDefault.cpp" IN_LIST SOURCES)
    list(APPEND SOURCES ${CMAKE_CURRENT_LIST_DIR}/game/CHandlingDefault.cpp)
endif()

# Opus
add_library(libopus STATIC IMPORTED)
set_target_properties(libopus PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/vendor/opus/libopus.a)

add_library(multiplayer SHARED ${SOURCES})
target_link_libraries(multiplayer log EGL GLESv3 opus shadowhook::shadowhook ${CMAKE_CURRENT_SOURCE_DIR}/vendor/bass/libs/${ANDROID_ABI}/libbass.so)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -w -s -g -fvisibility=hidden -pthread -Wall -O3 -fexceptions")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

set_target_properties(multiplayer PROPERTIES
        LINK_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
)