package com.example.helloworld;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

	@GetMapping("/hello")
	public ResponseEntity<?> healthCheckFail() {
		return new ResponseEntity("Hello World V1 version!!!", HttpStatus.OK);

	}

}
