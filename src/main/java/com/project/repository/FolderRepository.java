package com.project.repository;

import com.project.model.Folder;
import com.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findAllByOwnerAndParentIsNullOrderByCreatedAtDesc(User owner);
    List<Folder> findAllByOwnerAndParentOrderByCreatedAtDesc(User owner, Folder parent);
    Optional<Folder> findByIdAndOwner(Long id, User owner);
}
