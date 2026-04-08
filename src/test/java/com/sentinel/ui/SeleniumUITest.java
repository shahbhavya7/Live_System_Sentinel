package com.sentinel.ui;
import org.openqa.selenium.By;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;

import static org.junit.jupiter.api.Assertions.*;

class SeleniumUITest {

    @Test
    void testDashboardLoads() {

        System.setProperty(
                "webdriver.edge.driver",
                "drivers/msedgedriver.exe"
        );

        WebDriver driver = new EdgeDriver();

        driver.get("http://localhost:8080");

        boolean bodyVisible =
                driver.findElement(By.tagName("body")).isDisplayed();

        assertTrue(bodyVisible);

        driver.quit();
    }
}