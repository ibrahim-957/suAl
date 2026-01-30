package com.delivery.SuAl.aspect;

import com.delivery.SuAl.annotation.SendNotification;
import com.delivery.SuAl.annotation.SendNotifications;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationAspect {
    private final NotificationService notificationService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @AfterReturning(pointcut =
            "@annotation(com.delivery.SuAl.annotation.SendNotification) || " +
                    "@annotation(com.delivery.SuAl.annotation.SendNotifications)",
            returning = "result"
    )
    public void sendNotificationAfterMethod(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            SendNotifications multipleAnnotation = method.getAnnotation(SendNotifications.class);

            if (multipleAnnotation != null) {
                for (SendNotification notification : multipleAnnotation.value()) {
                    processSingleNotification(joinPoint, notification, result);
                }
            } else {
                SendNotification notification = method.getAnnotation(SendNotification.class);
                if (notification != null) {
                    processSingleNotification(joinPoint, notification, result);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to send notifications via AOP", ex);
        }
    }

    private void processSingleNotification(JoinPoint joinPoint, SendNotification sendNotification, Object result) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", result);
            context.setVariable("args", joinPoint.getArgs());

            Long receiverId = null;
            if (!sendNotification.receiverIdExpression().isEmpty()) {
                receiverId = parser.parseExpression(sendNotification.receiverIdExpression())
                        .getValue(context, Long.class);
            }

            Long referenceId = null;
            if (!sendNotification.referenceIdExpression().isEmpty()) {
                referenceId = parser.parseExpression(sendNotification.referenceIdExpression())
                        .getValue(context, Long.class);
            }

            String message = sendNotification.message();
            if (sendNotification.evaluateMessage()) {
                message = parser.parseExpression(sendNotification.message())
                        .getValue(context, String.class);
            }

            if (receiverId != null) {
                try {
                    notificationService.createNotification(
                            NotificationRequest.builder()
                                    .receiverType(sendNotification.receiverType())
                                    .receiverId(receiverId)
                                    .notificationType(sendNotification.notificationType())
                                    .title(sendNotification.title())
                                    .message(message)
                                    .referenceId(referenceId)
                                    .build()
                    );

                    log.info("Notification sent successfully via AOP - Receiver: {} ({}), Type: {}",
                            receiverId, sendNotification.receiverType(), sendNotification.notificationType());
                } catch (Exception ex) {
                    log.error("Failed to create notification for receiver {} ({}): {}",
                            receiverId, sendNotification.receiverType(), ex.getMessage(), ex);
                }
            } else {
                log.warn("Skipping notification - receiverId is null from expression: {}",
                        sendNotification.receiverIdExpression());
            }
        } catch (Exception ex) {
            log.error("Failed to process single notification", ex);
        }
    }
}
