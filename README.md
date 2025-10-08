# 🛒 Web Scraping with Selenium – Product Data Extractor  

This project is a **web scraping utility built with Selenium (Java)** to extract structured product data (name, description, price, etc.) from e-commerce websites such as **FreshCo**.  
It is designed to handle dynamic HTML structures and supports multiple selector variations to ensure data is captured accurately even when website layouts change.  

## 🚀 Features  
- Extracts **product name, description, and price** reliably  
- Handles **discounted prices** (e.g., red text for sale items)  
- Works with **multiple CSS selectors** for robust scraping  
- Saves output in structured format (**CSV / JSON**)  
- Flexible design for **adding more websites** in the future  

## 🛠️ Tech Stack  
- **Language:** Java  
- **Automation:** Selenium WebDriver  
- **Browser Driver:** ChromeDriver  
- **Build Tool:** Maven / Gradle  
- **Logging:** SLF4J / Logback  
- **Output:** CSV / JSON  

## 📂 Project Structure  

├── src  
│   ├── main  
│   │   ├── java  
│   │   │   ├── com.example.model       # Data models (Product, etc.)  
│   │   │   ├── com.example.service     # Scraper service logic  
│   │   │   ├── com.example.controller  # Controller for running scraper  
│   │   │   └── com.example.utils       # Helper functions  
│   │   └── resources  
│   └── test                            # Unit tests  
├── pom.xml                             # Maven dependencies  
└── README.md                           # Project documentation  

## ⚡ How to Run  

1. Clone the repository  
   git clone https://github.com/your-username/product-scraper.git  
   cd product-scraper  

2. Install dependencies  
   mvn clean install  

3. Run the scraper  
   mvn exec:java -Dexec.mainClass="com.example.controller.ScraperController"  

4. View output in output/products.csv  

## 📊 Example Output  

Input HTML:  

<p class="flex gap-1 items-center">  
   <span class="text-red200 font-bold">$4.99</span>  
   <span class="font-extrabold text-xl line-through opacity-50">$5.49</span>  
</p>  

Extracted Data:  

{  
  "name": "Plums Red Small",  
  "description": "Fresh, juicy red plums perfect for snacking or baking.",  
  "price": "$4.99"  
}  

## 🔮 Future Improvements  
- Support for pagination (scrape multiple pages)  
- Add proxy & user-agent rotation for anti-bot handling  
- Store data in a database (MongoDB / MySQL)  
- Deploy as a REST API for live scraping  

## 📜 License  
This project is licensed under the MIT License – feel free to use and modify.  
