package com.LTD.ltdWorksAPI.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
@AllArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @SneakyThrows
    @Async
    public void sendEmail(String recipient, String subject, String content){
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("zemchickpro@gmail.com", "Libroty");
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }
}
