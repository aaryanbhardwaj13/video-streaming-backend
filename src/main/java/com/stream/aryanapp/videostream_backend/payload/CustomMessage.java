package com.stream.aryanapp.videostream_backend.payload;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CustomMessage {
	private String message;
	private boolean success=false;
}
