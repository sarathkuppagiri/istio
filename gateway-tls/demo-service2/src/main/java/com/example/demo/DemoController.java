package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DemoController {


	@GetMapping("/demo2")
	public ResponseEntity<String> hello() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getForEntity("http://www.google.com", String.class);
		return new ResponseEntity<>("hello2", HttpStatus.OK);
	}

}
