package myschedule.quartz.extra.mail;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configurable
public class SendEmailConfig {
	
	@Bean
	public JavaMailSenderImpl javaMailSenderImpl() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com");
		mailSender.setUsername("noreply");
		Properties prop = mailSender.getJavaMailProperties();
		prop.put("mail.transport.protocol", "smtp");
		prop.put("mail.smtp.auth", "false");
		prop.put("mail.smtp.starttls.enable", "false");
		prop.put("mail.debug", "false");
		return mailSender;
	}

}