package org.zerock.controller;


import lombok.extern.log4j.Log4j;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.domain.AttachFileDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
@Log4j
public class UploadController {

    private boolean checkImageType(File file){
        try {


            String contentType = Files.probeContentType(file.toPath());

//            log.info("=========================");
//            log.info(contentType);
//            log.info("=========================");
            //이 문자열 인스턴스가 지정한 문자로 시작하는지를 확인합니다. image true
            if(contentType == null){
                return false;
            }
            return contentType.startsWith("image");

        }catch (IOException e){
            e.printStackTrace();
        }//catch
            return false;



    }//checkImageType

    @GetMapping("/uploadForm")
    public void uploadForm(){
        log.info("upload form");

    }//uploadForm


    @PostMapping("/uploadFormAction")
    public void uploadFormPost(MultipartFile[] uploadFile, Model model){

        String uploadFolder = "//Users//ijiseong//IdeaProjects//Security2//upload//temp";

        for (MultipartFile multipartFile : uploadFile){
            log.info("----------------------------------");
            log.info("Upload File Name: " + multipartFile.getOriginalFilename());
            log.info("Upload File Size: " + multipartFile.getSize());

            File saveFile = new File(uploadFolder, multipartFile.getOriginalFilename());
            try {
                multipartFile.transferTo(saveFile);
            } catch (Exception e){
                log.error(e.getMessage());
            }//catch


        }//for

    }//uploadFormAction

    @GetMapping("/uploadAjax")
    public void uploadAjax(){

        log.info("upload ajax");


    }//uploadAjax

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/uploadAjaxAction", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<List<AttachFileDTO>> uploadAjaxPost(MultipartFile[] uploadFile){


        List<AttachFileDTO> list = new ArrayList<>();
        //log.info("update ajax post........");
        String uploadFolder = "//Users//ijiseong//IdeaProjects//Security2//upload//temp";


        String uploadFolderPath = getFolder();// yyyy/mm/dd
        //make folder-------
        File uploadPath = new File(uploadFolder, uploadFolderPath);
        log.info("upload path: " + uploadPath);

        if(uploadPath.exists() == false){
            uploadPath.mkdirs();
        } //make yyyy/mm/dd folder


        for(MultipartFile multipartFile : uploadFile){




//            log.info("---------------------");
//            log.info("Upload File Name: " + multipartFile.getOriginalFilename());
//            log.info("Upload File Size: " + multipartFile.getSize());

            AttachFileDTO attachDTO = new AttachFileDTO();
            String uploadFileName = multipartFile.getOriginalFilename();


//            log.info("=========================");
//            log.info(multipartFile.getContentType());
//            log.info("=========================");



            //IE has file path
            //getOriginalFilename -> 크롬 파일 이름만, IE는 경로까지 반환
            uploadFileName = uploadFileName.substring(uploadFileName.lastIndexOf("\\")+1); // 크롬에서 잘 찾는다?
            //uploadFileName = uploadFileName.substring(uploadFileName.lastIndexOf("//")+1); //IE 쓰면 윈도우임
            log.info("only file name: " + uploadFileName);
            attachDTO.setFileName(uploadFileName);


            UUID uuid = UUID.randomUUID();
            uploadFileName = uuid.toString() +"_"+uploadFileName;


            try {
                //File saveFile = new File(uploadFolder, uploadFileName);
                File saveFile = new File(uploadPath, uploadFileName);
                multipartFile.transferTo(saveFile);



                log.info("=========================");
                log.info(saveFile.getName());
                log.info(saveFile.toPath());
                log.info(Files.probeContentType(saveFile.toPath()));
                log.info("=========================");


                attachDTO.setUuid(uuid.toString());
                attachDTO.setUploadPath(uploadFolderPath);


                //check image type file
                if (checkImageType(saveFile)){

                    attachDTO.setImage(true);

                    FileOutputStream thumbnail = new FileOutputStream( new File(uploadPath, "s_" + uploadFileName) );

                    //static void	createThumbnail(InputStream is, OutputStream os, int width, int height)
                    Thumbnailator.createThumbnail(multipartFile.getInputStream(), thumbnail, 400,400);
                    thumbnail.close();

                }//if

                //add to List
                list.add(attachDTO);

            }catch (Exception e){
                e.printStackTrace();
            }//catch




        }//for

        return new ResponseEntity<>(list, HttpStatus.OK);

    }//uploadAjaxPost

    private String getFolder(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String str = sdf.format(date);

        //log.info("str : "+ str.replace("-",File.separator) );

        return str.replace("-",File.separator);
    }//getFolder



    @GetMapping("/display")
    @ResponseBody
    public ResponseEntity<byte[]> getFile(String fileName){

        log.info("fileName: " + fileName);
        File file = new File("//Users//ijiseong//IdeaProjects//Security2//upload//temp//"+fileName);

        log.info("file: "+file);

        ResponseEntity<byte[]> result = null;

        try {
            HttpHeaders header = new HttpHeaders();
            //image header
            header.add("Content-Type", Files.probeContentType(file.toPath()));
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);

        }catch (IOException e){
            e.printStackTrace();
        }
        return result;

    }//getFile

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@RequestHeader("User-Agent") String userAgent, String fileName){
        log.info("download file: " + fileName);
        Resource resource = new FileSystemResource("//Users//ijiseong//IdeaProjects//Security2//upload//temp//"+fileName);
        log.info("resource: " + resource);

        if(resource.exists() == false){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String resourceName = resource.getFilename();

        // remove UUID
        String resourceOriginalName = resourceName.substring(resourceName.indexOf("_")+1);

        HttpHeaders headers = new HttpHeaders();
        try {
            String downloadName = null;
            if(userAgent.contains("Trident")){
                log.info("IE browser");
                downloadName = URLEncoder.encode(resourceOriginalName, "UTF-8").replaceAll("\\+"," ");
            } else if(userAgent.contains("Edge")){
                log.info("Edge browser");
                downloadName = URLEncoder.encode(resourceOriginalName, "UTF-8");
            } else{
                log.info("Chrome browser");
                downloadName = new String(resourceOriginalName.getBytes("UTF-8"), "ISO-8859-1");
            }//if

            log.info("downloadName: " + downloadName);
            //headers.add("Content-Disposition", "attachment; filename="+new String(resourceName.getBytes("UTF-8"),"ISO-8859-1"));
            headers.add("Content-Disposition", "attachment; filename=" + downloadName);
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }//catch

        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }//downloadFile


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deleteFile")
    @ResponseBody
    public ResponseEntity<String> deleteFile(String fileName, String type){

        log.info("deleteFile: " + fileName);

        File file;
        try {
            // javascript로 encodeURIComponent 처리된 문자열을 java에서 디코딩하기 위해 URLDecoder.decode() 메소드를 사용한다.
            file = new File("//Users//ijiseong//IdeaProjects//Security2//upload//temp//"+ URLDecoder.decode(fileName,"UTF-8"));
            file.delete();

            if(type.equals("image")){
                String largeFileName = file.getAbsolutePath().replace("s_","");
                log.info("lageFileName: "+largeFileName);
                file = new File(largeFileName);
                file.delete();
            }//if

        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }//catch

        return new ResponseEntity<String>("deleted", HttpStatus.OK);
    }




}
