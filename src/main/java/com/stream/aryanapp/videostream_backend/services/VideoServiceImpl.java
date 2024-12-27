package com.stream.aryanapp.videostream_backend.services;

import com.stream.aryanapp.videostream_backend.entities.Video;
import com.stream.aryanapp.videostream_backend.repositories.VideoRepository;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;


@Service

public class VideoServiceImpl implements VideoService {
	private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
	private VideoRepository videoRepository;

	public VideoServiceImpl(VideoRepository videoRepository) {
		this.videoRepository = videoRepository;
	}

	@Value("${files.video.hsl}")
	String HSL_DIR;
	@Value("${files.video}")
	String DIR;

	@PostConstruct
	public void init() {
		File file = new File(DIR);
		try {
			Files.createDirectories(Paths.get(HSL_DIR));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (!file.exists()) {
			file.mkdir();
			logger.info("Folder created");
		} else {
			logger.info("Folder already exists");
		}
	}


	@Override
	public Video save(Video video, MultipartFile file) {


		try {
			//fetch file details
			String fileName = file.getOriginalFilename();


			InputStream inputStream = file.getInputStream();
			String contentType = file.getContentType();
			//processing

			String cleanFileName = StringUtils.cleanPath(fileName);
			String cleanDirName = StringUtils.cleanPath(DIR);

			Path path = Paths.get(cleanDirName, cleanFileName);
			//copyfile
			logger.info("this is the path" + path);
			Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

			video.setFilePath(path.toString());
			video.setContentType(contentType);
			logger.info("this is content type and filepath" + " " + contentType + " " + path);
			Video savedVideo = videoRepository.save(video);
			processVideo(savedVideo.getVideoId());
			return savedVideo;


		} catch (IOException e) {
			throw new RuntimeException(e);

		}

	}

	@Override
	public Video getVideoByID(String fileid) {
		Optional<Video> result = videoRepository.findById(fileid);
		return result.orElse(null);
	}

	@Override
	public Video getVideoByTitle(String title) {
		Optional<Video> result = videoRepository.findByTitle(title);
		return result.orElse(null);

	}

	@Override
	public List<Video> getall() {
		return videoRepository.findAll();
	}

	@Override
	public String processVideo(String videoId) {
		Video video = this.getVideoByID(videoId);
		String filePath = video.getFilePath();
		logger.info("this is the video filepath->>" + filePath);

		Path videoPath = Paths.get(filePath);
		logger.info("this is the path wala path-->" + videoPath);

		try {
			Path outputPath = Paths.get(HSL_DIR, videoId);
			Files.createDirectories(outputPath);
			Path absoluteVideoPath = videoPath.toAbsolutePath();
			Path absoluteOutputPath = outputPath.toAbsolutePath();
			String escapedVideoPath = absoluteVideoPath.toString().replace("\\", "/");
			String escapedOutputPath = absoluteOutputPath.toString().replace("\\", "/");

			String ffmpegPath = "C:\\ffmpeg\\ffmpeg-7.1-essentials_build\\bin\\ffmpeg.exe";
			String ffmpegCmd = String.format(
					"%s -i \"%s\" -c:v libx264 -preset veryfast -crf 20 -c:a aac -b:a 128k -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%03d.ts\" \"%s/master.m3u8\"",
					ffmpegPath, escapedVideoPath, escapedOutputPath, escapedOutputPath
			);

//			String ffmpegCmd = String.format(
//					"ffmpeg -i \"%s\" -c:v libx264 -preset veryfast -crf 20 -c:a aac -b:a 128k -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%03d.ts\" \"%s/master.m3u8\"",
//					videoPath, outputPath, outputPath
//			);

			logger.info("ffmpeg cmd --> "+ffmpegCmd);
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			try (InputStream is = process.getInputStream()) {
				String result = new String(is.readAllBytes());
				logger.error("FFmpeg output: " + result);
			}

			int exit = process.waitFor();
			if (exit != 0) {
				throw new RuntimeException("video processing failed!!");
			}

			return videoId;


		} catch (IOException ex) {
			throw new RuntimeException("Video processing fail!!");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}
}
