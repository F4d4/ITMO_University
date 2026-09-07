package org.example.delegate.auth;

import org.camunda.bpm.engine.impl.form.validator.FormFieldValidator;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Проверяет формат email в стартовой форме регистрации.
 * Встроенных валидаторов формата у Camunda нет — только required/minlength/maxlength/min/max/readonly,
 * поэтому проверка подключается к полю как custom-валидатор.
 */
@Component("emailFormFieldValidator")
public class EmailFormFieldValidator implements FormFieldValidator {

    private static final Pattern EMAIL =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    @Override
    public boolean validate(Object submittedValue, FormFieldValidatorContext context) {
        String email = submittedValue == null ? null : submittedValue.toString().trim();
        if (email == null || email.isEmpty()) {
            // О пустом значении сообщает constraint "required", иначе пользователь получит две ошибки сразу.
            return true;
        }
        return EMAIL.matcher(email).matches();
    }
}
