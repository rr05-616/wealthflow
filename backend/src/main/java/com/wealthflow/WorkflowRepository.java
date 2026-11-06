package com.wealthflow;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkflowRepository extends JpaRepository<WorkflowState,String>{}
