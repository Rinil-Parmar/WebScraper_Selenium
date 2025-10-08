package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@ComponentScan(basePackages = "com.example.freshcocrawler")
public class FreshcoCrawlerApplication {

//	@Autowired
//	private FreshCoCrawlerService crawlerService;

	public static void main(String[] args) {
//		SpringApplication.run(FreshcoCrawlerApplication.class, args);
		SpringApplication.run(FreshcoCrawlerApplication.class, args);
		System.out.println("FreshCo Scraper Application Started!");
		System.out.println("Visit: http://localhost:8080/api/scraper/scrape to start scraping");

	}


}
