# Versão mínima requerida do CMake
cmake_minimum_required(VERSION 3.6.4111459)

# Cria a biblioteca compartilhada com o código nativo
add_library(
        native-lib SHARED
        audio-equalizer-lib.cpp
)

# Localiza a biblioteca do sistema "log" para debug (opcional)
find_library(log-lib log)

# Vincula a biblioteca nativa à biblioteca "log"
target_link_libraries(native-lib ${log-lib})