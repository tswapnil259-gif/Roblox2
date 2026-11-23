package com.robloxai.studio.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM scripts WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeScripts(projectId: Long): Flow<List<ScriptEntity>>

    @Upsert
    suspend fun upsertScript(script: ScriptEntity): Long

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt ASC, id ASC")
    fun observeChat(projectId: Long): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertChat(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearChat(projectId: Long)

    @Query("SELECT * FROM concepts_3d WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeConcepts3D(projectId: Long): Flow<List<Concept3DEntity>>

    @Insert
    suspend fun insertConcept3D(concept: Concept3DEntity): Long

    @Delete
    suspend fun deleteConcept3D(concept: Concept3DEntity)
}

@Database(
    entities = [
        ProjectEntity::class,
        ScriptEntity::class,
        ChatMessageEntity::class,
        Concept3DEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roblox_ai_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
