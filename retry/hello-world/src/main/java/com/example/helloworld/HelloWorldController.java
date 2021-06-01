package com.example.helloworld;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

	@GetMapping("/hello")
	public ResponseEntity<?> hello() {
		System.out.println("hello.........");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity("Error from server", HttpStatus.INTERNAL_SERVER_ERROR);

	}

}
