package vn.org.thn.service.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages = "vn.org.thn.service" thay vi mac dinh chi quet
 * "vn.org.thn.service.example" -- de cac @Component/@Configuration trong
 * module dung chung "base" (package vn.org.thn.service.base, la project
 * anh em, khong phai package con) cung duoc Spring phat hien.
 */
@SpringBootApplication(scanBasePackages = "vn.org.thn.service")
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
