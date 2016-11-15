package myschedule.quartz.extra.mail;

import java.io.File;

import javax.activation.DataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

public class SendEmailService {

	private static final Logger LOG = Logger.getLogger(SendEmailService.class);

	private static String fromAddress = "no-reply@gigdata.com";

	public void sendEmail(String toAddress, String subject, String body, String attachment) {

		if(!StringUtils.hasText(toAddress)) {
			return;
		}
		try {
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.register(SendEmailConfig.class);
			ctx.refresh();

			JavaMailSenderImpl mailSender = ctx.getBean(JavaMailSenderImpl.class);
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper mailMsg = new MimeMessageHelper(mimeMessage, true);
			mailMsg.setFrom(new InternetAddress(fromAddress, fromAddress));
			if(toAddress.contains(",")) {
				String[] strArr = toAddress.split(",");
				for(String s : strArr) {
					s = s.trim();
				}
				mailMsg.setTo(strArr);
			} else {
				mailMsg.setTo(toAddress);
			}
			
			// Email contents
			if (subject != null) {
				mailMsg.setSubject(subject);
			} else {
				mailMsg.setSubject("Alert");
			}
			
			if (body != null) {
				mailMsg.setText("<p>" + body + "</p>", true);
			} else {
				mailMsg.setText("<p>This is an automatically generated email </p>", true);
			}

			// FileSystemResource object for attachment
			if (attachment != null) {
				FileSystemResource file = new FileSystemResource(new File(attachment));
				String filename = 
						attachment.contains("/") ? 
								attachment.substring(attachment.lastIndexOf("/") + 1) : 
									attachment.substring(attachment.lastIndexOf("\\") + 1);
				mailMsg.addAttachment(filename, file);
			}
			
			// send message
			mailSender.send(mimeMessage);
			ctx.close();

		} catch (Exception e) {
			LOG.error("Exception while sending email..." + e.getMessage());
		}

	}
	
	public void sendEmail(String toAddress, String subject, String body, DataSource ds, String filename) {
		if(!StringUtils.hasText(toAddress)) {
			return;
		}
		try {
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.register(SendEmailConfig.class);
			ctx.refresh();

			JavaMailSenderImpl mailSender = ctx.getBean(JavaMailSenderImpl.class);
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper mailMsg = new MimeMessageHelper(mimeMessage, true);
			mailMsg.setFrom(new InternetAddress(fromAddress, fromAddress));
			if(toAddress.contains(",")) {
				String[] strArr = toAddress.split(",");
				for(String s : strArr) {
					s = s.trim();
				}
				mailMsg.setTo(strArr);
			} else {
				mailMsg.setTo(toAddress);
			}

			// Email contents
			if (subject != null) {
				mailMsg.setSubject(subject);
			} else {
				mailMsg.setSubject("Alert");
			}

			if (body != null) {
				mailMsg.setText("<p>" + body + "</p>", true);
			} else {
				mailMsg.setText("<p>This is an automatically generated email</p>", true);
			}
			
			if(ds != null) {
				mailMsg.addAttachment(filename, ds);
			}
			// send message
			mailSender.send(mimeMessage);
			ctx.close();
		} catch (Exception e) {
			LOG.error("Exception while sending email..." + e.getMessage());
		}

	}

}