package com.fzph.sslclient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DemoController {

	@GetMapping("/test/sslclient")
	public ResponseEntity<String> hello() {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://ssl-server.mesh-external.svc.cluster.local:443/server/ssl";
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		System.out.println(response.getBody());
		return new ResponseEntity<>(response.getBody()+" client", HttpStatus.OK);
	}

}
