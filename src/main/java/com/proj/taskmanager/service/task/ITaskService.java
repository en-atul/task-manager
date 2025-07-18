package com.proj.taskmanager.service.task;

import com.proj.taskmanager.model.Task;
import com.proj.taskmanager.request.task.CreateTaskReq;
import com.proj.taskmanager.request.task.UpdateTaskReq;

import java.util.List;

public interface ITaskService {
    Task createTask(Long userId, CreateTaskReq task);

    List<Task> getAllTaskByProjectId(Long projectId);
    
    List<Task> getAllTaskByUserId(Long projectId, Long userId);
    
    Task getTaskById(Long taskId);
    
    Task updateTask(Long userId, Long taskId, UpdateTaskReq request);
    
    Task assignTask(Long userId, Long taskId, Long assigneeId);
    
    Task updateTaskDueDate(Long userId, Long taskId, java.time.LocalDate dueDate);
    
    void deleteTask(Long userId, Long taskId);
}
