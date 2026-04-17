package com.delivery.SuAl.annotation;

import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.ReceiverType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SendNotifications.class)
public @interface SendNotification {

    ReceiverType receiverType();

    NotificationType notificationType();

    String title();

    String message();

    String receiverIdExpression() default "";

    String referenceIdExpression() default "";

    boolean evaluateMessage() default false;
}
