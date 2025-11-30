package com.badat.notificationservice.repository;

import com.badat.notificationservice.entity.EmailTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailTaskRepository extends JpaRepository<EmailTask, String> {
    @Query(value = "SELECT * FROM email_task " +
            "WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT 10 " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<EmailTask> findPendingTasksWithLock();
}
