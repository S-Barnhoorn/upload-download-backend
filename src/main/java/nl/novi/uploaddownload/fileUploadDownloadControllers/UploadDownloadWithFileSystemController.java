package nl.novi.uploaddownload.fileUploadDownloadControllers;

import nl.novi.uploaddownload.model.FileUploadResponse;
import nl.novi.uploaddownload.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CrossOrigin
@RestController
public class UploadDownloadWithFileSystemController {
    private FileStorageService fileStorageService;

    public UploadDownloadWithFileSystemController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

//    post for single upload
    @PostMapping("single/upload")
    FileUploadResponse singleFileUpload(@RequestParam("file") MultipartFile file){

        String fileName = fileStorageService.storeFile(file);

        // next line makes url. example "http://localhost:8080/download/naam.jpg"
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/download/").path(fileName).toUriString();

        String contentType = file.getContentType();

        FileUploadResponse response = new FileUploadResponse(fileName, contentType, url );

        return response;
    }

//    get for single download
    @GetMapping("/download/{fileName}")
    ResponseEntity<Resource> downLoadSingleFile(@PathVariable String fileName, HttpServletRequest request) {

        Resource resource = fileStorageService.downLoadFile(fileName);

//        this mediaType decides witch type you accept if you only accept 1 type
//        MediaType contentType = MediaType.IMAGE_JPEG;
//        this is going to accept multiple types
        String mimeType;

        try{
            mimeType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

//        for download attachment use next line
//        return ResponseEntity.ok().contentType(contentType).header(HttpHeaders.CONTENT_DISPOSITION, "attachment;fileName=" + resource.getFilename()).body(resource);
//        for showing image in browser
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimeType)).header(HttpHeaders.CONTENT_DISPOSITION, "inline;fileName=" + resource.getFilename()).body(resource);
    }

//    post for multiple uploads
    @PostMapping("multiple/upload")
    List<FileUploadResponse> multipleUpload(@RequestParam("files") MultipartFile[] files) {

        if(files.length > 7) {
            throw new RuntimeException("to many files");
        }
        List<FileUploadResponse> uploadResponseList = new ArrayList<>();
        Arrays.asList(files).stream().forEach(file -> {
            String fileName = fileStorageService.storeFile(file);

            // next line makes url. example "http://localhost:8080/download/naam.jpg"
            String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/download/").path(fileName).toUriString();

            String contentType = file.getContentType();

            FileUploadResponse response = new FileUploadResponse(fileName, contentType, url );
            uploadResponseList.add(response);

        });

        return uploadResponseList;

    }

    @GetMapping("zipDownload")
    public void zipDownload(@RequestParam("fileName") String[] files, HttpServletResponse response) throws IOException {

        try(ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())){
            Arrays.asList(files).stream().forEach(file -> {
                Resource resource = fileStorageService.downLoadFile(file);
                ZipEntry zipEntry = new ZipEntry(resource.getFilename());
                try {
                    zipEntry.setSize(resource.contentLength());
                    zos.putNextEntry(zipEntry);

                    StreamUtils.copy(resource.getInputStream(), zos);

                    zos.closeEntry();
                } catch (IOException e) {
                    System.out.println("some exception while zipping");
                }
            });
            zos.finish();
        }

        response.setStatus(200);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;fileName=zipfile");
    }

}
