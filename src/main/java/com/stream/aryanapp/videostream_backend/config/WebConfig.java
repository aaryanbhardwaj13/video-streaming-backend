package com.stream.aryanapp.videostream_backend.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/v1/videos/**")
						.allowedOrigins("http://localhost:5173")  // Allow Vite frontend
						.allowedMethods("GET", "POST", "PUT", "DELETE")
						.allowCredentials(true)
						.allowedHeaders("*");
			}
		};
	}
}


