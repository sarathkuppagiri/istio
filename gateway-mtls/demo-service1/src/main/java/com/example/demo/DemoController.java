package com.example.demo;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@CrossOrigin("*")
@RestController
public class DemoController {

	@GetMapping("/demo1")
	public ResponseEntity<String> testDemo(@RequestHeader HttpHeaders headers) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange("http://demo2-svc/demo2", HttpMethod.GET,
				new HttpEntity<Object>(new HttpHeaders()), String.class);
		return new ResponseEntity<>("hello1 from demo1 & " + response.getBody() + " from demo2", HttpStatus.OK);
	}

}
