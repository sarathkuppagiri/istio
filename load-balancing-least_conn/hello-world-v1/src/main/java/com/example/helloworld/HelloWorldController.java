package com.example.helloworld;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

	@GetMapping("/hello")
	public ResponseEntity<?> hello() {
		try {
			String host = InetAddress.getLocalHost().getHostName();
			return new ResponseEntity("Hello World V1 version from "+host, HttpStatus.OK);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ResponseEntity("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		

	}

}
