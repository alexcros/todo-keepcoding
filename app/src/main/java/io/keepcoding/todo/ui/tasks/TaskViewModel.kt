package io.keepcoding.todo.ui.tasks

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.keepcoding.todo.data.model.Task
import io.keepcoding.todo.data.repository.TaskRepository
import io.keepcoding.todo.ui.base.BaseViewModel
import io.keepcoding.todo.util.Event
import io.keepcoding.todo.util.call
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.*

class TaskViewModel(val taskRepository: TaskRepository) : BaseViewModel() {

    val tasksEvent = MutableLiveData<List<Task>>()
    val newTaskAddedEvent = MutableLiveData<Event<Unit>>()
    val taskUpdatedEvent = MutableLiveData<Event<Task>>()

    init {
        loadTasks()
    }

    fun loadTasks() {
        taskRepository
            .getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { tasks ->
                    tasksEvent.value = tasks
                },
                onError = {
                    Log.e("TaskViewModel", "Error: $it")
                }
            ).addTo(compositeDisposable)
    }

    fun addNewTask(taskContent: String) {
        val newTask = Task(0, taskContent, Date(), false)

        Completable.fromCallable {
            taskRepository.insert(newTask)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    loadTasks()
                    newTaskAddedEvent.call()
                },
                onError = {
                    Log.e("TaskViewModel", "$it")
                }
            )
            .addTo(compositeDisposable)
    }

    fun deleteTask(task: Task) {
        Completable.fromCallable {
            taskRepository.delete(task)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    loadTasks()
                },
                onError = {
                    Log.e("TaskViewModel", "$it")
                }
            )
            .addTo(compositeDisposable)
    }

    fun markAsDone(task: Task) {
        if (task.isDone) {
            return
        }

        val newTask = task.copy(isDone = true)
        updateTask(newTask)
    }

    fun markAsNotDone(task: Task) {
        if (!task.isDone) {
            return
        }

        val newTask = task.copy(isDone = false)
        updateTask(newTask)
    }

    private fun updateTask(task: Task) {
        Completable.fromCallable {
            taskRepository.update(task)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    taskUpdatedEvent.call(task)
                },
                onError = {
                    Log.e("TaskViewModel", "$it")
                }
            )
            .addTo(compositeDisposable)
    }
}