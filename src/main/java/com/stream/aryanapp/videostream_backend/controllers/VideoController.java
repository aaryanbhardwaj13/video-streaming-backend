package com.stream.aryanapp.videostream_backend.controllers;

import com.stream.aryanapp.videostream_backend.AppConstants;
import com.stream.aryanapp.videostream_backend.entities.Video;
import com.stream.aryanapp.videostream_backend.payload.CustomMessage;
import com.stream.aryanapp.videostream_backend.services.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/videos")


public class VideoController {

	private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@Value("${files.video.hsl}")
	String HSL_DIR;

	//CREATE VIDEO
	@PostMapping
	public ResponseEntity<?> create(@RequestParam("file") MultipartFile file,
									@RequestParam("title") String title,
									@RequestParam("description") String description) {
		logger.info("Received request to upload video: {}", title);

		Video video = new Video();
		video.setVideoId(UUID.randomUUID().toString()); // Changed to setVideoId
		video.setDescription(description);
		video.setTitle(title);

		Video savedVideo = videoService.save(video, file);
		if (savedVideo != null) {
			logger.info("Video uploaded successfully: {}", savedVideo.getVideoId());
			return ResponseEntity.status(HttpStatus.OK).body(savedVideo);
		} else {
			logger.error("Failed to upload video: {}", title);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(CustomMessage.builder().message("video not saved").success(false).build());
		}
	}

	//STREAM VIDEO
	@GetMapping("/stream/{videoId}")
	public ResponseEntity<Resource> stream(@PathVariable String videoId) {
		logger.info("Streaming request for video ID: {}", videoId);

		Video video = videoService.getVideoByID(videoId);
		if (video == null) {
			logger.warn("Video not found for ID: {}", videoId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		String contentType = video.getContentType();
		String filePath = video.getFilePath();
		Resource resource = new FileSystemResource(filePath);
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		logger.info("Successfully prepared video stream for ID: {}", videoId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
	}

	//Get all videos
	@GetMapping
	public List<Video> getAll() {
		logger.info("Fetching all videos");
		return videoService.getall();
	}

	//stream videos in chunks
	@GetMapping("/stream/range/{videoId}")
	public ResponseEntity<Resource> streaminRange(@PathVariable String videoId,
												  @RequestHeader(value = "range", required = false) String range) throws IOException {
		logger.info("Chunked streaming request for video ID: {}", videoId);

		Video video = videoService.getVideoByID(videoId);
		if (video == null) {
			logger.warn("Video not found for ID: {}", videoId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		Path path = Paths.get(video.getFilePath());
		Resource resource = new FileSystemResource(path);

		String contentType = video.getContentType();
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		long fileLength = path.toFile().length();
		if (range == null) {
			logger.info("Full video streaming for ID: {}", videoId);
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
		}

		long rangeStart;
		long rangeEnd;

		String[] ranges = range.replace("bytes=", "").split("-");
		rangeStart = Long.parseLong(ranges[0]);
		rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
		if (rangeEnd >= fileLength) {
			rangeEnd = fileLength - 1;
		}

		logger.info("Streaming bytes {}-{} of video ID: {}", rangeStart, rangeEnd, videoId);

		try (InputStream inputStream = Files.newInputStream(path)) {
			inputStream.skip(rangeStart);
			long contentLength = rangeEnd - rangeStart + 1;
			byte[] data = new byte[(int) contentLength];
			int read = inputStream.read(data, 0, data.length);
			logger.info("this is the readData", read);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");
			headers.add("X-Content-Type-Options", "nosniff");
			headers.setContentLength(contentLength);

			logger.info("Successfully streamed chunk for video ID: {}", videoId);
			return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers)
					.contentType(MediaType.parseMediaType(contentType)).body(new ByteArrayResource(data));

		} catch (IOException ex) {
			logger.error("Error streaming video ID: {}", videoId, ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}


	@GetMapping("/{videoId}/master.m3u8")
	public ResponseEntity<Resource> serverMasterFile(
			@PathVariable String videoId
	) {

//        creating path
		Path path = Paths.get(HSL_DIR, videoId, "master.m3u8");

		System.out.println(path);

		if (!Files.exists(path)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Resource resource = new FileSystemResource(path);

		return ResponseEntity
				.ok()
				.header(
						HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
				)
				.body(resource);


	}

	//serve the segments

	@GetMapping("/{videoId}/{segment}.ts")
	public ResponseEntity<Resource> serveSegments(
			@PathVariable String videoId,
			@PathVariable String segment
	) {

		// create path for segment
		Path path = Paths.get(HSL_DIR, videoId, segment + ".ts");
		if (!Files.exists(path)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Resource resource = new FileSystemResource(path);

		return ResponseEntity
				.ok()
				.header(
						HttpHeaders.CONTENT_TYPE, "video/mp2t"
				)
				.body(resource);

	}
}
