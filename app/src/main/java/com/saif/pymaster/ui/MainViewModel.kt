package com.saif.pymaster.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Learning
import com.saif.pymaster.data.Profile
import com.saif.pymaster.data.Progress
import com.saif.pymaster.data.Repository
import com.saif.pymaster.data.Settings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    val learning = Learning(app)
    val state: StateFlow<AppState?> = repo.state

    init {
        viewModelScope.launch {
            repo.load()
            learning.announceDailyQuiz()
        }
    }

    fun finishOnboarding() = viewModelScope.launch {
        repo.update { it.copy(onboarded = true) }
    }

    fun saveProfile(profile: Profile, isNew: Boolean) = viewModelScope.launch {
        repo.update { s ->
            val old = s.profile
            if (old?.customAvatarPath != null && old.customAvatarPath != profile.customAvatarPath) {
                runCatching { File(old.customAvatarPath).takeIf { it.exists() }?.delete() }
            }
            s.copy(onboarded = true, profile = profile.copy(createdAt = old?.createdAt?.takeIf { it > 0 } ?: System.currentTimeMillis()))
        }
        if (isNew) {
            learning.welcome(profile.name)
            learning.announceDailyQuiz()
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch {
        repo.update { it.copy(settings = transform(it.settings)) }
    }

    fun markNotificationRead(id: Long) = viewModelScope.launch {
        repo.update { s -> s.copy(notifications = s.notifications.map { if (it.id == id) it.copy(read = true) else it }) }
    }

    fun markAllRead() = viewModelScope.launch {
        repo.update { s -> s.copy(notifications = s.notifications.map { it.copy(read = true) }) }
    }

    fun deleteNotification(id: Long) = viewModelScope.launch {
        repo.update { s -> s.copy(notifications = s.notifications.filterNot { it.id == id }) }
    }

    fun clearNotifications() = viewModelScope.launch {
        repo.update { it.copy(notifications = emptyList()) }
    }

    fun resetProgress() = viewModelScope.launch {
        repo.update { it.copy(progress = Progress(), notifications = emptyList(), settings = it.settings.copy(devUnlocked = false)) }
    }

    fun addStudySeconds(seconds: Long) = viewModelScope.launch { learning.addStudySeconds(seconds) }
    fun openLesson(id: String) = viewModelScope.launch { learning.openLesson(id) }
    fun completeLesson(id: String) = viewModelScope.launch { learning.completeLesson(id) }
    fun setProjectStep(projectId: String, step: Int, done: Boolean) = viewModelScope.launch { learning.setProjectStep(projectId, step, done) }
    fun completeProject(id: String) = viewModelScope.launch { learning.completeProject(id) }

    fun submitModuleQuiz(moduleId: String, correct: Int, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(learning.submitModuleQuiz(moduleId, correct))
    }

    fun submitDailyQuiz(answers: List<Int>, correct: Int) = viewModelScope.launch {
        learning.submitDailyQuiz(answers, correct)
    }

    fun enableDeveloperMode() = viewModelScope.launch {
        repo.update { it.copy(settings = it.settings.copy(devUnlocked = true)) }
        learning.unlockCertificatePreview()
    }
}
