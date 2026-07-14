package com.aistudio.xide.core.intelligence.symbols

import kotlinx.coroutines.flow.Flow

interface SymbolIndex {
    suspend fun queryByName(name: String): List<SymbolReference>
    suspend fun queryByType(type: SymbolType): List<SymbolReference>
    suspend fun getReferences(symbolId: String): List<SymbolLocation>
}

interface SymbolRepository {
    suspend fun saveSymbol(reference: SymbolReference)
    suspend fun removeSymbol(symbolId: String)
    suspend fun removeAllForFile(filePath: String)
    fun observeFileSymbols(filePath: String): Flow<List<SymbolReference>>
}

enum class SymbolType {
    CLASS, FUNCTION, VARIABLE, FILE, MODULE, DEPENDENCY, UNKNOWN
}

data class SymbolReference(
    val id: String,
    val name: String,
    val type: SymbolType,
    val location: SymbolLocation,
    val language: String,
    val metadata: Map<String, String> = emptyMap()
)

data class SymbolLocation(
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
)
