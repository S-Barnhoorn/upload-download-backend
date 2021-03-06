package nl.novi.uploaddownload.repository;

import nl.novi.uploaddownload.model.FileDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.io.File;

@Transactional
public interface DocFileDao extends JpaRepository<FileDocument, Long> {
    FileDocument findByFileName(String fileName);
}
