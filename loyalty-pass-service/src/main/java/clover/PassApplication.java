package clover;

import clover.db.Pass;
import clover.pass.PassController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@SpringBootApplication
public class PassApplication {

  public static void main(String[] args) {
    SpringApplication.run(PassApplication.class,args);
  }
}
