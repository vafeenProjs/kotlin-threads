package com.example

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.withLock

class KThreadPool(
    private val countAvailableProcessors: Int = 2
//    Runtime.getRuntime().availableProcessors()
) {
    private val tasks = mutableListOf<() -> Unit>()
    private val lock = ReentrantLock()
    private val taskAvailable = lock.newCondition()

    @Volatile
    private var isThreadPoolActive = true

    init {
        for (i in 1..countAvailableProcessors) {
            Thread {
                println("Запуск $i процесса")
                run()
            }.start()
        }
    }

    fun addTask(job: () -> Unit) {
        lock.withLock {
            println("Добавление задачи")
            tasks.add(job)
            taskAvailable.signal()
        }
    }

    private fun run() {
        while (isThreadPoolActive) {
            var task: (() -> Unit)? = null

            lock.withLock {
                if (tasks.isEmpty() && isThreadPoolActive) {
                    println("Ждем задачу")
                    taskAvailable.await()
                    println("Сейчас будем выполнять")
                }

                if (!isThreadPoolActive) return

                task = tasks.removeFirstOrNull()
            }

            task?.invoke()
        }
    }

    fun shutdown() {
        lock.withLock {
            isThreadPoolActive = false
            taskAvailable.signalAll()
        }
    }
}