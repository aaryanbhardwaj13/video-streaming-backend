package com.stream.aryanapp.videostream_backend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TestLombok {
	private String name;
	private int age;

	public static void main(String[] args) {
		TestLombok test = TestLombok.builder().name("Aryan").age(25).build();
		System.out.println(test.getName());
	}
}
