package com.stream.aryanapp.videostream_backend.services;

import com.stream.aryanapp.videostream_backend.entities.Video;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

	//save video
	Video save(Video video, MultipartFile file);

	//get video by id
	Video getVideoByID(String fileid);

	//get video by title
	Video getVideoByTitle(String title);

	//get all videos

	List<Video> getall();

    String processVideo(String VideoId);


}
