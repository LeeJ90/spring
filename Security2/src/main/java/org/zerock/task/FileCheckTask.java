package org.zerock.task;


import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.domain.BoardAttachVO;
import org.zerock.mapper.BoardAttachMapper;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Component
public class FileCheckTask {




    @Setter(onMethod_ = { @Autowired})
    private BoardAttachMapper attachMapper;

    private String getFolderYesterDay() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Calendar cal = Calendar.getInstance();

        //cal.add(Calendar.DATE, -1);

        String str = sdf.format(cal.getTime());

        return str.replace("-", File.separator);
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkFiles() throws Exception {

        log.warn("File Check Task run.................");
        log.warn(new Date());
        // file list in database
        List<BoardAttachVO> fileList = attachMapper.getOldFiles();

        // ready for check file in directory with database file list
        List<Path> fileListPaths = fileList.stream()
                .map(vo -> Paths.get("//Users//ijiseong//IdeaProjects//Security2//upload//temp", vo.getUploadPath(), vo.getUuid() + "_" + vo.getFileName()))
                .collect(Collectors.toList());

        // image file has thumnail file
        fileList.stream().filter(vo -> vo.getFileType().trim().equals("true"))
                .map(vo -> Paths.get("//Users//ijiseong//IdeaProjects//Security2//upload//temp", vo.getUploadPath(), "s_" + vo.getUuid() + "_" + vo.getFileName()))
                .forEach(p -> fileListPaths.add(p));

        log.warn("===========================================");

        fileListPaths.forEach(p -> log.warn(p));

        // files in yesterday directory
        //toFile -> 파일객체 리턴함 Path-> File
        File targetDir = Paths.get("//Users//ijiseong//IdeaProjects//Security2//upload//temp", getFolderYesterDay()).toFile();


        //File[] files = dir.listFiles();
        //디렉토리의 파일목록(디렉토리포함)을 File 배열로 반환
        File[] removeFiles = targetDir.listFiles(file -> fileListPaths.contains(file.toPath()) == false);


        log.warn("-----------------------------------------");
        for (File file : removeFiles) {

            log.warn(file.getAbsolutePath());

            file.delete();

        }
    }

}
