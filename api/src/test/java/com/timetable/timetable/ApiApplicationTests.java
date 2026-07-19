package com.timetable.timetable;

import com.timetable.timetable.config.SecurityProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableConfigurationProperties(SecurityProperties.class)
class ApiApplicationTests {

  @Test
  void contextLoads() {
  }

}
