package ir.amv.snippets.referenceinitialsetup;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ReferenceInitialSetupApplication {

  @PostConstruct
  public void afterRun() {
    log.info("Application Startup");
  }

  public static void main(String[] args) {
    SpringApplication.run(ReferenceInitialSetupApplication.class, args);
  }

}
