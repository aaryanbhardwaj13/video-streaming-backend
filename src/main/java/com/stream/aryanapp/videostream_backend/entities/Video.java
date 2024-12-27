package com.stream.aryanapp.videostream_backend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Video {
	@Id
	private String videoId;

	private String title;
	private String description;
	private String contentType;
	private String filePath;


}
