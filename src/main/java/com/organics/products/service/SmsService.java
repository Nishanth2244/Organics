package com.organics.products.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.HashMap;
import java.util.Map;


@Service
public class SmsService {

    private final SnsClient snsClient;

    public SmsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

//    public void sendOtpSms(String phoneNumber, String otp) {
//
//        String message =
//                "Your OTP is " + otp + ". Valid for 3 minutes.";
//
//        PublishRequest request = PublishRequest.builder()
//                .phoneNumber(phoneNumber)
//                .message(message)
//                .build();
//
//        snsClient.publish(request);
//    }
public String sendOtpSms(String phoneNumber, String message) {
    Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
    messageAttributes.put("AWS.SNS.SMS.SMSType",
            MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue("Transactional")
                    .build()
    );

    PublishRequest request = PublishRequest.builder()
            .message(message)
            .phoneNumber(phoneNumber)
            .messageAttributes(messageAttributes)
            .build();

    PublishResponse response = snsClient.publish(request);
    System.out.println("SMS sent with Message ID: " + response.messageId());
    return response.messageId();
}
    public String sendOtpEmail(String topicArn, String otp) {

        String message =
                "Admin Password Reset OTP: " + otp +
                        "\nValid for 3 minutes.\nDo not share this OTP.";

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("Admin Password Reset OTP")
                .message(message)
                .build();

        PublishResponse response = snsClient.publish(request);
        System.out.println("EMAIL sent with Message ID: " + response.messageId());
        return response.messageId();
    }


}

  